package com.researchagent.memory;

public record RelevantMemory(
        String taskId,
        int stepNumber,
        String memoryType,
        String content,
        double similarity
) {
}
