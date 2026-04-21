package com.researchagent.tools.database;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SqlBuilder {

    public String build(DatabaseIntent intent) {
        String entity = intent.getEntity();
        String tableName = "public.\"" + entity + "\"";

        if (intent.getOperation() == DatabaseOperation.COUNT) {
            return buildCountQuery(intent, tableName);
        }

        if (intent.getOperation() == DatabaseOperation.LIST) {
            return buildListQuery(intent, tableName);
        }

        throw new IllegalArgumentException("Unsupported database operation: " + intent.getOperation());
    }

    private String buildCountQuery(DatabaseIntent intent, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) AS total_count FROM ").append(tableName);

        String whereClause = buildWhereClause(intent.getFilters());
        if (!whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause);
        }

        return sql.toString();
    }

    private String buildListQuery(DatabaseIntent intent, String tableName) {
        List<String> columns = intent.getColumns().isEmpty()
                ? List.of("*")
                : intent.getColumns();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append(tableName);

        String whereClause = buildWhereClause(intent.getFilters());
        if (!whereClause.isBlank()) {
            sql.append(" WHERE ").append(whereClause);
        }

        int limit = intent.getLimit() == null ? 20 : intent.getLimit();
        sql.append(" LIMIT ").append(limit);

        return sql.toString();
    }

    private String buildWhereClause(List<FilterCondition> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }

        return filters.stream()
                .filter(f -> f.getValue() != null && !f.getValue().isBlank())
                .map(this::toSqlCondition)
                .collect(Collectors.joining(" AND "));
    }

    private String toSqlCondition(FilterCondition filter) {
        String field = filter.getField();
        String operator = filter.getOperator();
        String value = escapeSql(filter.getValue());

        if ("EQUALS".equalsIgnoreCase(operator)) {
            return field + " = '" + value + "'";
        }

        if ("STARTS_WITH".equalsIgnoreCase(operator)) {
            return "CAST(" + field + " AS TEXT) LIKE '" + value + "%'";
        }

        if ("CONTAINS".equalsIgnoreCase(operator)) {
            return "CAST(" + field + " AS TEXT) LIKE '%" + value + "%'";
        }

        if ("LESS_THAN".equalsIgnoreCase(operator)) {
            return field + " < '" + value + "'";
        }

        if ("GREATER_THAN".equalsIgnoreCase(operator)) {
            return field + " > '" + value + "'";
        }

        throw new IllegalArgumentException("Unsupported filter operator: " + operator);
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}