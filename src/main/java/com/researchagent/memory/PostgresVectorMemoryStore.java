package com.researchagent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchagent.model.AgentStep;
import com.researchagent.model.AgentTask;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresVectorMemoryStore implements MemoryStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresVectorMemoryStore.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;
    private final int embeddingDimensions;
    private boolean pgvectorEnabled;
    private volatile boolean embeddingUnavailableLogged;

    public PostgresVectorMemoryStore(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            EmbeddingModel embeddingModel,
            @Value("${agent.memory.embedding-dimensions:768}") int embeddingDimensions
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
    }

    @PostConstruct
    void initializeSchema() {
        pgvectorEnabled = enableVectorExtensionIfAvailable();

        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS agent_tasks (
                        task_id VARCHAR(64) PRIMARY KEY,
                        goal TEXT NOT NULL,
                        max_steps INTEGER NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        final_response TEXT,
                        task_json JSONB NOT NULL
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS agent_memory_entries (
                        id BIGSERIAL PRIMARY KEY,
                        task_id VARCHAR(64) NOT NULL REFERENCES agent_tasks(task_id) ON DELETE CASCADE,
                        step_number INTEGER NOT NULL,
                        memory_type VARCHAR(32) NOT NULL,
                        content TEXT NOT NULL,
                        embedding_json JSONB NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        UNIQUE (task_id, step_number)
                    )
                    """);
            if (pgvectorEnabled) {
                jdbcTemplate.execute("""
                        ALTER TABLE agent_memory_entries
                        ADD COLUMN IF NOT EXISTS embedding vector(%d)
                        """.formatted(embeddingDimensions));
            }
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_agent_memory_task_id
                    ON agent_memory_entries (task_id)
                    """);
        } catch (DataAccessException ex) {
            throw new IllegalStateException(
                    "Failed to initialize vector memory schema. Ensure PostgreSQL is reachable and pgvector is installed.",
                    ex
            );
        }
    }

    @Override
    public AgentTask createTask(String goal, int maxSteps) {
        AgentTask task = new AgentTask(UUID.randomUUID().toString(), goal, maxSteps);
        saveTask(task);
        return task;
    }

    @Override
    public void saveTask(AgentTask task) {
        jdbcTemplate.update("""
                        INSERT INTO agent_tasks (
                            task_id, goal, max_steps, created_at, updated_at, status, final_response, task_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                        ON CONFLICT (task_id) DO UPDATE SET
                            goal = EXCLUDED.goal,
                            max_steps = EXCLUDED.max_steps,
                            created_at = EXCLUDED.created_at,
                            updated_at = EXCLUDED.updated_at,
                            status = EXCLUDED.status,
                            final_response = EXCLUDED.final_response,
                            task_json = EXCLUDED.task_json
                        """,
                task.getTaskId(),
                task.getGoal(),
                task.getMaxSteps(),
                Timestamp.from(task.getCreatedAt()),
                Timestamp.from(task.getUpdatedAt()),
                task.getStatus().name(),
                task.getFinalResponse(),
                writeTask(task)
        );

        persistStepMemories(task);
    }

    @Override
    public Optional<AgentTask> getTask(String taskId) {
        List<String> rows = jdbcTemplate.query(
                "SELECT task_json::text FROM agent_tasks WHERE task_id = ?",
                (rs, rowNum) -> rs.getString(1),
                taskId
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(readTask(rows.getFirst()));
    }

    @Override
    public List<RelevantMemory> findRelevantMemories(String taskId, String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        Embedding queryEmbedding = embedSafely(query);
        if (queryEmbedding == null) {
            return List.of();
        }

        String vectorLiteral = toVectorLiteral(queryEmbedding);

        if (pgvectorEnabled) {
            return jdbcTemplate.query("""
                            SELECT
                                task_id,
                                step_number,
                                memory_type,
                                content,
                                1 - (embedding <=> CAST(? AS vector)) AS similarity
                            FROM agent_memory_entries
                            WHERE task_id <> ?
                              AND embedding IS NOT NULL
                            ORDER BY embedding <=> CAST(? AS vector)
                            LIMIT ?
                            """,
                    (rs, rowNum) -> new RelevantMemory(
                            rs.getString("task_id"),
                            rs.getInt("step_number"),
                            rs.getString("memory_type"),
                            rs.getString("content"),
                            rs.getDouble("similarity")
                    ),
                    vectorLiteral,
                    taskId,
                    vectorLiteral,
                    limit
            );
        }

        List<RelevantMemory> memories = jdbcTemplate.query("""
                        SELECT task_id, step_number, memory_type, content, embedding_json::text
                        FROM agent_memory_entries
                        WHERE task_id <> ?
                        """,
                (rs, rowNum) -> new RelevantMemory(
                        rs.getString(1),
                        rs.getInt(2),
                        rs.getString(3),
                        rs.getString(4),
                        cosineSimilarity(queryEmbedding.vector(), readEmbeddingJson(rs.getString(5)))
                ),
                taskId
        );

        return memories.stream()
                .sorted(Comparator.comparingDouble(RelevantMemory::similarity).reversed())
                .limit(limit)
                .toList();
    }

    private void persistStepMemories(AgentTask task) {
        for (AgentStep step : task.getSteps()) {
            String memoryText = buildMemoryText(task, step);
            if (memoryText.isBlank()) {
                continue;
            }

            Embedding embedding = embedSafely(memoryText);
            if (embedding == null) {
                continue;
            }

            jdbcTemplate.update("""
                            INSERT INTO agent_memory_entries (
                                task_id, step_number, memory_type, content, embedding_json, created_at
                            ) VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?)
                            ON CONFLICT (task_id, step_number) DO UPDATE SET
                                memory_type = EXCLUDED.memory_type,
                                content = EXCLUDED.content,
                                embedding_json = EXCLUDED.embedding_json,
                                created_at = EXCLUDED.created_at
                            """,
                    task.getTaskId(),
                    step.getStepNumber(),
                    step.getType().name(),
                    memoryText,
                    toEmbeddingJson(embedding),
                    Timestamp.from(task.getUpdatedAt() == null ? Instant.now() : task.getUpdatedAt())
            );

            if (pgvectorEnabled) {
                jdbcTemplate.update("""
                                UPDATE agent_memory_entries
                                SET embedding = CAST(? AS vector)
                                WHERE task_id = ? AND step_number = ?
                                """,
                        toVectorLiteral(embedding),
                        task.getTaskId(),
                        step.getStepNumber()
                );
            }
        }
    }

    private String buildMemoryText(AgentTask task, AgentStep step) {
        String content = step.getContent();
        if (content == null || content.isBlank()) {
            return "";
        }

        return "Goal: " + task.getGoal()
                + "\nStep: " + step.getType()
                + "\nContent: " + content;
    }

    private Embedding embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        if (embedding.dimension() != embeddingDimensions) {
            throw new IllegalStateException(
                    "Embedding dimension mismatch. Expected " + embeddingDimensions
                            + " but received " + embedding.dimension()
                            + ". Update agent.memory.embedding-dimensions or switch embedding model."
            );
        }
        return embedding;
    }

    private Embedding embedSafely(String text) {
        try {
            return embed(text);
        } catch (RuntimeException ex) {
            logEmbeddingFailure(ex);
            return null;
        }
    }

    private String toVectorLiteral(Embedding embedding) {
        StringBuilder builder = new StringBuilder("[");
        float[] values = embedding.vector();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values[i]);
        }
        builder.append(']');
        return builder.toString();
    }

    private String toEmbeddingJson(Embedding embedding) {
        try {
            List<Float> values = new ArrayList<>(embedding.dimension());
            for (float value : embedding.vector()) {
                values.add(value);
            }
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize embedding", ex);
        }
    }

    private float[] readEmbeddingJson(String json) {
        try {
            List<Double> values = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class)
            );
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i).floatValue();
            }
            return result;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize embedding JSON", ex);
        }
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            return -1.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;

        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return -1.0;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private boolean enableVectorExtensionIfAvailable() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            return true;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private void logEmbeddingFailure(RuntimeException ex) {
        if (!embeddingUnavailableLogged) {
            embeddingUnavailableLogged = true;
            LOGGER.warn(
                    "Embedding model unavailable. Vector memory recall is disabled until the embedding model is available. Cause: {}",
                    ex.getMessage()
            );
        }
    }

    private String writeTask(AgentTask task) {
        try {
            return objectMapper.writeValueAsString(task);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent task " + task.getTaskId(), ex);
        }
    }

    private AgentTask readTask(String json) {
        try {
            return objectMapper.readValue(json, AgentTask.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize agent task JSON", ex);
        }
    }
}
