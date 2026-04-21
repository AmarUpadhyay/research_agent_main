package com.researchagent.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AgentTask {

    private final String taskId;
    private final String goal;
    private final int maxSteps;
    private final Instant createdAt;
    private Instant updatedAt;
    private AgentTaskStatus status;
    private String finalResponse;
    private final List<AgentStep> steps = new ArrayList<>();

    public AgentTask(String taskId, String goal, int maxSteps) {
        this(taskId, goal, maxSteps, Instant.now(), Instant.now(), AgentTaskStatus.PENDING, null, List.of());
    }

    @JsonCreator
    public AgentTask(
            @JsonProperty("taskId") String taskId,
            @JsonProperty("goal") String goal,
            @JsonProperty("maxSteps") int maxSteps,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("status") AgentTaskStatus status,
            @JsonProperty("finalResponse") String finalResponse,
            @JsonProperty("steps") List<AgentStep> steps
    ) {
        this.taskId = taskId;
        this.goal = goal;
        this.maxSteps = maxSteps;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
        this.status = status == null ? AgentTaskStatus.PENDING : status;
        this.finalResponse = finalResponse;
        if (steps != null) {
            this.steps.addAll(steps);
        }
    }

    public void addStep(AgentStep step) {
        steps.add(step);
        updatedAt = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getGoal() {
        return goal;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public AgentTaskStatus getStatus() {
        return status;
    }

    public void setStatus(AgentTaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
        this.updatedAt = Instant.now();
    }

    public List<AgentStep> getSteps() {
        return List.copyOf(steps);
    }
}
