package com.researchagent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchagent.memory.MemoryStore;
import com.researchagent.memory.RelevantMemory;
import com.researchagent.model.AgentDecision;
import com.researchagent.model.AgentStep;
import com.researchagent.model.AgentStepType;
import com.researchagent.model.AgentTask;
import com.researchagent.model.AgentTaskStatus;
import com.researchagent.model.ToolResult;
import com.researchagent.tools.AgentTool;
import com.researchagent.tools.database.DatabaseIntent;
import com.researchagent.tools.database.DatabaseIntentValidator;
import com.researchagent.tools.database.DatabaseResultFormatter;
import com.researchagent.tools.database.DatabaseSchemaRegistry;
import com.researchagent.tools.database.SqlBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AgentExecutor {

    private static final int DEFAULT_MAX_STEPS = 4;

    private final TaskAgent taskAgent;
    private final MemoryStore memoryStore;
    private final ObjectMapper objectMapper;
    private final Map<String, AgentTool> toolsByName;

    private final DatabaseIntentValidator databaseIntentValidator;
    private final SqlBuilder sqlBuilder;
    private final DatabaseResultFormatter databaseResultFormatter;
    private final DatabaseSchemaRegistry databaseSchemaRegistry;

    public AgentExecutor(
            TaskAgent taskAgent,
            MemoryStore memoryStore,
            ObjectMapper objectMapper,
            List<AgentTool> tools,
            DatabaseIntentValidator databaseIntentValidator,
            SqlBuilder sqlBuilder,
            DatabaseResultFormatter databaseResultFormatter,
            DatabaseSchemaRegistry databaseSchemaRegistry
    ) {
        this.taskAgent = taskAgent;
        this.memoryStore = memoryStore;
        this.objectMapper = objectMapper;
        this.toolsByName = tools.stream()
                .collect(Collectors.toUnmodifiableMap(AgentTool::getName, tool -> tool));
        this.databaseIntentValidator = databaseIntentValidator;
        this.sqlBuilder = sqlBuilder;
        this.databaseResultFormatter = databaseResultFormatter;
        this.databaseSchemaRegistry = databaseSchemaRegistry;
    }

    public AgentTask run(String goal) {
        AgentTask task = memoryStore.createTask(goal, DEFAULT_MAX_STEPS);
        task.setStatus(AgentTaskStatus.RUNNING);
        memoryStore.saveTask(task);

        int stepCounter = 1;

        for (int loop = 1; loop <= task.getMaxSteps(); loop++) {
            String prompt = buildPrompt(task, loop);
            String rawDecision = taskAgent.decideNextAction(prompt);
            String normalizedDecision = normalizeDecision(rawDecision);

            AgentDecision decision;
            try {
                decision = objectMapper.readValue(normalizedDecision, AgentDecision.class);
            } catch (JsonProcessingException ex) {
                task.addStep(new AgentStep(
                        stepCounter++,
                        AgentStepType.ERROR,
                        "Invalid JSON from agent. Raw: " + rawDecision + " | Normalized: " + normalizedDecision,
                        null
                ));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution failed due to invalid agent response.");
                memoryStore.saveTask(task);
                return task;
            }

            task.addStep(new AgentStep(
                    stepCounter++,
                    AgentStepType.PLAN,
                    defaultText(decision.getSummary(), "No summary provided."),
                    null
            ));

            if ("FINAL".equalsIgnoreCase(decision.getDecisionType())) {
                if (requiresToolExecution(task.getGoal()) && !hasSuccessfulObservation(task)) {
                    task.addStep(new AgentStep(
                            stepCounter++,
                            AgentStepType.OBSERVATION,
                            "Premature FINAL blocked. External data is still required, so call the appropriate tool before answering.",
                            null
                    ));
                    memoryStore.saveTask(task);
                    continue;
                }

                String finalResponse = defaultText(decision.getFinalResponse(), decision.getSummary());

                task.addStep(new AgentStep(
                        stepCounter++,
                        AgentStepType.FINAL,
                        finalResponse,
                        null
                ));
                task.setStatus(AgentTaskStatus.COMPLETED);
                task.setFinalResponse(finalResponse);
                memoryStore.saveTask(task);
                return task;
            }

            if (!"TOOL".equalsIgnoreCase(decision.getDecisionType())) {
                task.addStep(new AgentStep(
                        stepCounter++,
                        AgentStepType.ERROR,
                        "Unsupported decision type: " + decision.getDecisionType(),
                        null
                ));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution failed because the agent returned an unsupported decision type.");
                memoryStore.saveTask(task);
                return task;
            }

            AgentTool tool = toolsByName.get(decision.getToolName());
            if (tool == null) {
                task.addStep(new AgentStep(
                        stepCounter++,
                        AgentStepType.ERROR,
                        "Unknown tool requested: " + decision.getToolName(),
                        null
                ));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution failed because the agent requested an unknown tool.");
                memoryStore.saveTask(task);
                return task;
            }

            if ("database".equalsIgnoreCase(decision.getToolName())) {
                AgentTask resultTask = executeDatabaseStep(task, tool, decision, stepCounter);
                stepCounter = task.getSteps().size() + 1;

                if (resultTask.getStatus() != AgentTaskStatus.RUNNING) {
                    return resultTask;
                }
                continue;
            }

            Map<String, Object> safeInput = decision.getToolInput() == null
                    ? Map.of()
                    : decision.getToolInput();

            if (isRepeatedToolCall(task, decision.getToolName(), safeInput)) {
                task.addStep(new AgentStep(
                        stepCounter++,
                        AgentStepType.ERROR,
                        "Repeated tool call blocked for tool '" + decision.getToolName() + "'.",
                        null
                ));
                task.setStatus(AgentTaskStatus.FAILED);
                task.setFinalResponse("Execution stopped because the agent repeated the same tool call without making progress.");
                memoryStore.saveTask(task);
                return task;
            }

            task.addStep(new AgentStep(
                    stepCounter++,
                    AgentStepType.ACTION,
                    "Calling tool '" + tool.getName() + "' with input " + safeInput,
                    null
            ));

            ToolResult result = tool.execute(safeInput);

            task.addStep(new AgentStep(
                    stepCounter++,
                    AgentStepType.OBSERVATION,
                    defaultText(result.getOutput(), "Tool executed with no output."),
                    result
            ));

            memoryStore.saveTask(task);
        }

        task.setStatus(AgentTaskStatus.FAILED);
        task.setFinalResponse(hasSuccessfulObservation(task)
                ? "Execution stopped after reaching the maximum number of steps."
                : "Execution failed because the agent never completed the required tool call before reaching the step limit.");
        task.addStep(new AgentStep(
                stepCounter,
                AgentStepType.ERROR,
                task.getFinalResponse(),
                null
        ));
        memoryStore.saveTask(task);
        return task;
    }

    private AgentTask executeDatabaseStep(
            AgentTask task,
            AgentTool databaseTool,
            AgentDecision decision,
            int stepCounter
    ) {
        DatabaseIntent databaseIntent;

        try {
            databaseIntent = objectMapper.convertValue(
                    decision.getToolInput(),
                    DatabaseIntent.class
            );

            databaseIntentValidator.validate(databaseIntent);

        } catch (Exception ex) {
            task.addStep(new AgentStep(
                    stepCounter++,
                    AgentStepType.OBSERVATION,
                    "Database intent validation failed: " + ex.getMessage(),
                    null
            ));

            memoryStore.saveTask(task);
            return task;
        }

        String sql;

        try {
            sql = sqlBuilder.build(databaseIntent);

        } catch (Exception ex) {
            task.addStep(new AgentStep(
                    stepCounter++,
                    AgentStepType.OBSERVATION,
                    "SQL build failed: " + ex.getMessage(),
                    null
            ));

            memoryStore.saveTask(task);
            return task;
        }

        Map<String, Object> toolInput = Map.of("sql", sql);

        if (isRepeatedToolCall(task, "database", toolInput)) {
            task.addStep(new AgentStep(
                    stepCounter++,
                    AgentStepType.OBSERVATION,
                    "Repeated database query detected. Try a different approach.",
                    null
            ));

            task.setStatus(AgentTaskStatus.FAILED);
            task.setFinalResponse("Execution stopped because the same database query was repeated without progress.");
            memoryStore.saveTask(task);
            return task;
        }

        task.addStep(new AgentStep(
                stepCounter++,
                AgentStepType.ACTION,
                "Calling tool 'database' with generated SQL: " + sql,
                null
        ));

        ToolResult result = databaseTool.execute(toolInput);

        String output = defaultText(result.getOutput(), "Database tool executed with no output.");

        task.addStep(new AgentStep(
                stepCounter++,
                AgentStepType.OBSERVATION,
                output,
                result
        ));

        if (!looksLikeError(output)) {
            if (isEmptyResult(output)) {
                if (databaseIntent.getFilters() != null && !databaseIntent.getFilters().isEmpty()) {
                    String finalResponse = "No matching records were found.";

                    task.addStep(new AgentStep(
                            stepCounter++,
                            AgentStepType.FINAL,
                            finalResponse,
                            null
                    ));

                    task.setStatus(AgentTaskStatus.COMPLETED);
                    task.setFinalResponse(finalResponse);
                    memoryStore.saveTask(task);
                    return task;
                }

                task.addStep(new AgentStep(
                        stepCounter++,
                        AgentStepType.OBSERVATION,
                        "Query returned 0 rows. Try refining query.",
                        null
                ));

                memoryStore.saveTask(task);
                return task;
            }

            String finalResponse = databaseResultFormatter.format(databaseIntent, output);

            task.addStep(new AgentStep(
                    stepCounter++,
                    AgentStepType.FINAL,
                    finalResponse,
                    null
            ));

            task.setStatus(AgentTaskStatus.COMPLETED);
            task.setFinalResponse(finalResponse);
            memoryStore.saveTask(task);
            return task;
        }

        task.addStep(new AgentStep(
                stepCounter++,
                AgentStepType.OBSERVATION,
                "Database execution error. Try correcting query.",
                null
        ));

        memoryStore.saveTask(task);
        return task;
    }

    private boolean isEmptyResult(String output) {
        if (output == null) {
            return true;
        }

        return output.contains("\"rows\":[]")
                || output.contains("\"rowCount\":0");
    }

    private boolean isRepeatedToolCall(AgentTask task, String toolName, Map<String, Object> input) {
        List<AgentStep> steps = task.getSteps();
        String inputAsText = String.valueOf(input);

        for (int i = steps.size() - 1; i >= 0; i--) {
            AgentStep step = steps.get(i);

            if (step.getType() == AgentStepType.OBSERVATION) {
                if (step.getContent() != null && looksLikeError(step.getContent())) {
                    return false;
                }
            }

            if (step.getType() == AgentStepType.ACTION) {
                String previousContent = step.getContent();
                if (previousContent == null) {
                    return false;
                }

                boolean sameTool = previousContent.contains("Calling tool '" + toolName + "'");
                boolean sameInput = previousContent.contains(inputAsText);

                return sameTool && sameInput;
            }
        }

        return false;
    }

    private boolean requiresToolExecution(String goal) {
        if (goal == null) {
            return false;
        }

        String g = goal.toLowerCase();

        return g.contains("count")
                || g.contains("how many")
                || g.contains("total")
                || g.contains("list")
                || g.contains("find")
                || g.contains("fetch")
                || g.contains("show")
                || g.contains("search")
                || g.contains("retrieve")
                || g.contains("database")
                || g.contains("email")
                || g.contains("report")
                || g.contains("excel");
    }

    private boolean hasSuccessfulObservation(AgentTask task) {
        for (AgentStep step : task.getSteps()) {
            if (step.getType() == AgentStepType.OBSERVATION) {
                String content = step.getContent();
                if (content != null && !content.isBlank() && !looksLikeError(content)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean looksLikeError(String content) {
        if (content == null) {
            return false;
        }

        String lower = content.toLowerCase();
        return lower.contains("error")
                || lower.contains("failed")
                || lower.contains("exception")
                || lower.contains("invalid");
    }

    private String summarizeStepForPrompt(AgentStep step) {
        if (step == null) {
            return "";
        }

        if (step.getType() == AgentStepType.OBSERVATION) {
            String content = step.getContent();

            if (content == null || content.isBlank()) {
                return "OBSERVATION: empty";
            }

            if (looksLikeError(content)) {
                return "OBSERVATION: tool returned error";
            }

            if (isEmptyResult(content)) {
                return "OBSERVATION: query returned 0 rows";
            }

            return "OBSERVATION: tool returned data successfully";
        }

        return step.getType() + ": " + step.getContent();
    }

    private String buildPrompt(AgentTask task, int loopNumber) {
        StringBuilder builder = new StringBuilder();

        builder.append("Goal: ").append(task.getGoal()).append("\n");
        builder.append("Attempt: ").append(loopNumber).append("\n");

        builder.append("Rules:\n");
        builder.append("- For requests asking to list, count, fetch, find, retrieve, search, or query records, use the database tool first.\n");
        builder.append("- Do not return FINAL before at least one successful tool observation when external data is required.\n");
        builder.append("- If external data is required and no successful observation exists yet, decisionType must be TOOL.\n");
        builder.append("- For database tool calls, return structured database intent only, not raw SQL.\n");
        builder.append("- Use LIST for list/find/show/search requests.\n");
        builder.append("- Use COUNT only when the user explicitly asks for count, total, or how many.\n");
        builder.append("- Choose only the minimum relevant columns needed to answer the request.\n");
        builder.append("- Never request sensitive columns like passwords, hashes, tokens, secrets, or API keys.\n");
        builder.append("- Never use all columns. Always choose explicit non-sensitive columns for LIST operations.\n");
        builder.append("- Keep limit small and safe. Prefer limit 20 for LIST.\n");
        builder.append("- Use real values only, never placeholders.\n");
        builder.append("- If a previous attempt returned 0 rows for a filtered search, do not repeat the same query again.\n");
        builder.append("- If no records match a specific filtered search, you may return FINAL saying no matching records were found.\n");

        builder.append("Available database entities and columns:\n");
        for (String entity : databaseSchemaRegistry.getEntities()) {
            builder.append("- ")
                    .append(entity)
                    .append(": safeColumns=")
                    .append(databaseSchemaRegistry.getSafeColumns(entity));
            if (!databaseSchemaRegistry.getSensitiveColumns(entity).isEmpty()) {
                builder.append(" | sensitiveColumns=")
                        .append(databaseSchemaRegistry.getSensitiveColumns(entity))
                        .append(" (never request)");
            }
            builder.append("\n");
        }

        builder.append("Available tools:\n");
        for (AgentTool tool : toolsByName.values()) {
            builder.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }

        List<RelevantMemory> relevantMemories = memoryStore.findRelevantMemories(task.getTaskId(), task.getGoal(), 3);
        builder.append("Relevant memories from previous tasks:\n");
        if (relevantMemories.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (RelevantMemory memory : relevantMemories) {
                builder.append("- [similarity=")
                        .append(String.format("%.2f", memory.similarity()))
                        .append("] prior ")
                        .append(memory.memoryType())
                        .append(": ")
                        .append(memory.content())
                        .append("\n");
            }
        }

        builder.append("Recent steps:\n");
        if (task.getSteps().isEmpty()) {
            builder.append("- none\n");
        } else {
            for (AgentStep step : task.getSteps()) {
                builder.append("- ").append(summarizeStepForPrompt(step)).append("\n");
            }
        }

        builder.append("Return exactly one JSON object only.\n");

        return builder.toString();
    }

    private String normalizeDecision(String raw) {
        if (raw == null) {
            return "";
        }

        String s = raw.trim();

        if (s.startsWith("```json")) {
            s = s.substring(7).trim();
        } else if (s.startsWith("```")) {
            s = s.substring(3).trim();
        }

        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3).trim();
        }

        int firstBrace = s.indexOf('{');
        int lastBrace = s.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            s = s.substring(firstBrace, lastBrace + 1);
        }

        s = s.replace('“', '"')
                .replace('”', '"')
                .replace('’', '\'')
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();

        s = s.replaceAll(",\\s*}", "}");
        s = s.replaceAll(",\\s*]", "]");

        if (s.startsWith("\"{") && s.endsWith("}\"")) {
            s = s.substring(1, s.length() - 1);
            s = s.replace("\\\"", "\"");
            s = s.replace("\\\\", "\\");
        }

        return s;
    }

    private String defaultText(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "";
    }
}
