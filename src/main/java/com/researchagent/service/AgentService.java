package com.researchagent.service;

import com.researchagent.agent.AgentExecutor;
import com.researchagent.memory.MemoryStore;
import com.researchagent.model.AgentStep;
import com.researchagent.model.AgentTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private final AgentExecutor agentExecutor;
    private final MemoryStore memoryStore;

    public AgentService(AgentExecutor agentExecutor, MemoryStore memoryStore) {
        this.agentExecutor = agentExecutor;
        this.memoryStore = memoryStore;
    }

    public AgentTask execute(String goal) {
        return agentExecutor.run(goal);
    }

    public AgentTask getTask(String taskId) {
        return memoryStore.getTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    public List<AgentStep> getTaskSteps(String taskId) {
        return getTask(taskId).getSteps();
    }
}
