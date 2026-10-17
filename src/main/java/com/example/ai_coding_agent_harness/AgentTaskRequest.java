package com.example.ai_coding_agent_harness;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentTaskRequest(
		@NotBlank(message = "task must not be blank")
		@Size(max = 2000, message = "task must be at most 2000 characters")
		String task
) {
}
