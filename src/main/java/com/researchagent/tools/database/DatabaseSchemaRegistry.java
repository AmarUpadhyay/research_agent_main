package com.researchagent.tools.database;

import com.researchagent.service.SchemaDiscoveryService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DatabaseSchemaRegistry {

    private static final Set<String> SENSITIVE_COLUMNS = Set.of(
            "password",
            "password_hash",
            "passwd",
            "secret",
            "token",
            "access_token",
            "refresh_token",
            "api_key",
            "apikey"
    );

    private final Map<String, List<String>> schema;

    public DatabaseSchemaRegistry(SchemaDiscoveryService schemaDiscoveryService) {
        this.schema = schemaDiscoveryService.loadSchema();
    }

    public boolean isValidEntity(String entity) {
        return schema.containsKey(entity);
    }

    public boolean isValidColumn(String entity, String column) {
        List<String> columns = schema.get(entity);
        return columns != null && columns.contains(column);
    }

    public List<String> getColumns(String entity) {
        return schema.getOrDefault(entity, List.of());
    }

    public List<String> getSafeColumns(String entity) {
        return getColumns(entity).stream()
                .filter(column -> !isSensitiveColumn(column))
                .collect(Collectors.toList());
    }

    public boolean isSensitiveColumn(String column) {
        return column != null && SENSITIVE_COLUMNS.contains(column.toLowerCase());
    }

    public Set<String> getSensitiveColumns(String entity) {
        return getColumns(entity).stream()
                .filter(this::isSensitiveColumn)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getEntities() {
        return schema.keySet();
    }
}
