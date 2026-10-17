
package com.example.ai_coding_agent_harness;

public record ExecutionResponse(
        String fileName,
        boolean successful,
        String status,
        String output,
        String message
) {}
