package com.researchagent.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolResult {

    private final String toolName;
    private final boolean success;
    private final String output;

    @JsonCreator
    public ToolResult(
            @JsonProperty("toolName") String toolName,
            @JsonProperty("success") boolean success,
            @JsonProperty("output") String output
    ) {
        this.toolName = toolName;
        this.success = success;
        this.output = output;
    }

    public String getToolName() {
        return toolName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }
}
