package com.researchagent.memory;

import com.researchagent.model.AgentTask;

import java.util.List;
import java.util.Optional;

public interface MemoryStore {

    AgentTask createTask(String goal, int maxSteps);

    void saveTask(AgentTask task);

    Optional<AgentTask> getTask(String taskId);

    List<RelevantMemory> findRelevantMemories(String taskId, String query, int limit);
}
