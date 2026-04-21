package com.researchagent.tools.database;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseResultFormatter {

    private final ObjectMapper objectMapper;

    public DatabaseResultFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String format(DatabaseIntent intent, String dbOutput) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    dbOutput,
                    new TypeReference<>() {}
            );

            Object rowsObj = parsed.get("rows");
            if (!(rowsObj instanceof List<?> rows)) {
                return "Database query completed, but response rows could not be read.";
            }

            if (intent.getOperation() == DatabaseOperation.COUNT) {
                if (!rows.isEmpty() && rows.get(0) instanceof Map<?, ?> firstRow) {
                    Object value = firstRow.get("total_count");
                    if (value == null) {
                        value = firstRow.get("total_users");
                    }
                    if (value != null) {
                        return "The total count is " + value + ".";
                    }
                }
                return "Count query completed, but no count value was found.";
            }

            if (intent.getOperation() == DatabaseOperation.LIST) {
                if (rows.isEmpty()) {
                    return "No matching records were found.";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(rows.size()).append(" record(s):");

                int index = 1;
                for (Object rowObj : rows) {
                    if (rowObj instanceof Map<?, ?> row) {
                        sb.append("\n").append(index++).append(". ");
                        boolean first = true;
                        for (Map.Entry<?, ?> entry : row.entrySet()) {
                            if (!first) {
                                sb.append(", ");
                            }
                            sb.append(entry.getKey()).append("=").append(valueOrDash(entry.getValue()));
                            first = false;
                        }
                    }
                }

                return sb.toString();
            }

            return "Database query completed successfully.";
        } catch (Exception ex) {
            return "Database query completed, but the result could not be formatted.";
        }
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}