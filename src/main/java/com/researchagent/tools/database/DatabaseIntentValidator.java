package com.researchagent.tools.database;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseIntentValidator {

    private final DatabaseSchemaRegistry schemaRegistry;

    public DatabaseIntentValidator(DatabaseSchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void validate(DatabaseIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("Database intent cannot be null.");
        }

        if (intent.getEntity() == null || intent.getEntity().isBlank()) {
            throw new IllegalArgumentException("Database entity is required.");
        }

        if (!schemaRegistry.isValidEntity(intent.getEntity())) {
            throw new IllegalArgumentException("Invalid database entity: " + intent.getEntity());
        }

        if (intent.getOperation() == null) {
            throw new IllegalArgumentException("Database operation is required.");
        }

        if (intent.getColumns() == null) {
            intent.setColumns(new ArrayList<>());
        }

        if (intent.getFilters() == null) {
            intent.setFilters(new ArrayList<>());
        }

        sanitizeFilters(intent);

        validateColumns(intent);
        validateFilters(intent);
        validateLimit(intent);

        applySafeDefaults(intent);
    }

    private void sanitizeFilters(DatabaseIntent intent) {
        List<FilterCondition> cleanedFilters = intent.getFilters().stream()
                .filter(filter -> filter != null)
                .filter(filter -> filter.getField() != null && !filter.getField().isBlank())
                .filter(filter -> filter.getOperator() != null && !filter.getOperator().isBlank())
                .filter(filter -> filter.getValue() != null && !filter.getValue().isBlank())
                .toList();

        intent.setFilters(cleanedFilters);
    }

    private void validateColumns(DatabaseIntent intent) {
        for (String column : intent.getColumns()) {
            if (column == null || column.isBlank()) {
                throw new IllegalArgumentException("Column name cannot be blank.");
            }

            if (!schemaRegistry.isValidColumn(intent.getEntity(), column)) {
                throw new IllegalArgumentException(
                        "Invalid column '" + column + "' for entity '" + intent.getEntity() + "'."
                );
            }

            if (schemaRegistry.isSensitiveColumn(column)) {
                throw new IllegalArgumentException(
                        "Sensitive column '" + column + "' cannot be selected."
                );
            }
        }
    }

    private void validateFilters(DatabaseIntent intent) {
        for (FilterCondition filter : intent.getFilters()) {
            if (!schemaRegistry.isValidColumn(intent.getEntity(), filter.getField())) {
                throw new IllegalArgumentException(
                        "Invalid filter field '" + filter.getField() + "' for entity '" + intent.getEntity() + "'."
                );
            }

            if (!isSupportedOperator(filter.getOperator())) {
                throw new IllegalArgumentException(
                        "Unsupported filter operator '" + filter.getOperator() + "'."
                );
            }

            if (schemaRegistry.isSensitiveColumn(filter.getField())) {
                throw new IllegalArgumentException(
                        "Sensitive column '" + filter.getField() + "' cannot be used in filters."
                );
            }
        }
    }

    private void validateLimit(DatabaseIntent intent) {
        if (intent.getLimit() != null && intent.getLimit() <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0.");
        }
    }

    private void applySafeDefaults(DatabaseIntent intent) {
        if (intent.getOperation() == DatabaseOperation.LIST) {
            if (!intent.getColumns().isEmpty()) {
                intent.setColumns(intent.getColumns().stream()
                        .filter(column -> !schemaRegistry.isSensitiveColumn(column))
                        .toList());
            }

            if (intent.getLimit() == null) {
                intent.setLimit(20);
            }

            if (intent.getLimit() > 100) {
                intent.setLimit(100);
            }

            if (intent.getColumns().isEmpty()) {
                intent.setColumns(defaultColumnsForEntity(intent.getEntity()));
            }
        }

        if (intent.getOperation() == DatabaseOperation.COUNT) {
            intent.setColumns(new ArrayList<>());
            intent.setLimit(null);
        }
    }

    private boolean isSupportedOperator(String operator) {
        return "EQUALS".equalsIgnoreCase(operator)
                || "STARTS_WITH".equalsIgnoreCase(operator)
                || "CONTAINS".equalsIgnoreCase(operator)
                || "LESS_THAN".equalsIgnoreCase(operator)
                || "GREATER_THAN".equalsIgnoreCase(operator);
    }

    private List<String> defaultColumnsForEntity(String entity) {
        if ("User".equalsIgnoreCase(entity)) {
            return List.of("id", "name", "email");
        }

        if ("Product".equalsIgnoreCase(entity)) {
            return List.of("id", "name", "category", "price", "stock");
        }

        List<String> safeColumns = schemaRegistry.getSafeColumns(entity);
        return safeColumns.isEmpty() ? schemaRegistry.getColumns(entity) : safeColumns;
    }
}
