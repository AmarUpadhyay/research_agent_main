package com.researchagent.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentStep {

    private final int stepNumber;
    private final AgentStepType type;
    private final String content;
    private final ToolResult toolResult;

    @JsonCreator
    public AgentStep(
            @JsonProperty("stepNumber") int stepNumber,
            @JsonProperty("type") AgentStepType type,
            @JsonProperty("content") String content,
            @JsonProperty("toolResult") ToolResult toolResult
    ) {
        this.stepNumber = stepNumber;
        this.type = type;
        this.content = content;
        this.toolResult = toolResult;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public AgentStepType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public ToolResult getToolResult() {
        return toolResult;
    }
}
