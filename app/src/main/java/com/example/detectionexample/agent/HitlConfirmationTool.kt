package com.example.detectionexample.agent

import com.google.mlkit.genai.agent.AgentTool

/**
 * Decorator tool that intercepts sensitive tool execution (e.g., deleting enrolled users)
 * to request explicit human-in-the-loop (HITL) confirmation before proceeding.
 */
class HitlConfirmationTool(
    name: String,
    description: String,
    delegate: AgentTool,
    private val onConfirm: suspend (toolName: String, args: Map<String, Any>) -> Boolean,
) : BaseAgentTool(name, description, delegate) {

    constructor(
        delegate: AgentTool,
        onConfirm: suspend (toolName: String, args: Map<String, Any>) -> Boolean,
    ) : this(
        name = try { delegate.name } catch (_: Throwable) { "tool" },
        description = try { delegate.description } catch (_: Throwable) { "" },
        delegate = delegate,
        onConfirm = onConfirm,
    )

    override suspend fun execute(args: Map<String, Any>): Map<String, Any?> {
        val confirmed = onConfirm(name, args)
        if (!confirmed) {
            return mapOf(
                "status" to "CANCELLED_BY_USER",
                "message" to "The user denied permission to execute action '$name' with parameters: $args"
            )
        }
        return checkNotNull(delegate).execute(args)
    }
}
