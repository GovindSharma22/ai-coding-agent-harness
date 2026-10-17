package com.example.ai_coding_agent_harness;

public record VerificationResponse(
        String fileName,
        boolean compiled,
        String status,
        String compilerOutput
) {
}