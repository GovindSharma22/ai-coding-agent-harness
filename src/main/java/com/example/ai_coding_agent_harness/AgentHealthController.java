package com.example.ai_coding_agent_harness;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentHealthController {

	@GetMapping("/api/agent/health")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}

}
