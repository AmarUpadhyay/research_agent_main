package com.researchagent.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchemaDiscoveryService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaDiscoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, List<String>> loadSchema() {
        String sql = """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                ORDER BY table_name, ordinal_position
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, List<String>> schema = new HashMap<>();

        for (Map<String, Object> row : rows) {
            String table = (String) row.get("table_name");
            String column = (String) row.get("column_name");

            // Convert to match your entity names (User, Product)
            String entityName = capitalize(table);

            schema.computeIfAbsent(entityName, k -> new ArrayList<>())
                    .add(column);
        }

        return schema;
    }

    private String capitalize(String name) {
        if (name == null || name.isBlank()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}