package com.example.detectionexample.agent;

import com.google.mlkit.genai.agent.AgentTool;

/**
 * Java bridge for AgentTool constructor to allow decorator subclasses
 * without triggering Kotlin metadata-level internal visibility constraints.
 */
public abstract class BaseAgentTool extends AgentTool {
    public BaseAgentTool(String name, String description, AgentTool delegate) {
        super(name, description, delegate);
    }
}
