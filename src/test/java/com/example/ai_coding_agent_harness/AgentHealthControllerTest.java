package com.example.ai_coding_agent_harness;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentHealthController.class)
class AgentHealthControllerTest {

	private final MockMvc mockMvc;

	AgentHealthControllerTest(@Autowired MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void healthReturnsHttp200AndStatusUp() throws Exception {
		mockMvc.perform(get("/api/agent/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

}
