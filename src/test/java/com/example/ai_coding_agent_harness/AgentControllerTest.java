
package com.example.ai_coding_agent_harness;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
@Import(ApiExceptionHandler.class)
class AgentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CodeAgentService codeAgentService;

  @MockitoBean
  private CodeExecutionService executionService;

  @MockitoBean
  private AgentService agentService;

  @MockitoBean
  private CodeVerificationService verificationService;

  @Test
  void createTaskReturnsHttp200ForValidRequest() throws Exception {

    when(agentService.process(any(AgentTaskRequest.class)))
      .thenReturn(new AgentTaskResponse(
        "Write a hello world program",
        "COMPLETED",
        "Here is a simple Java hello world program."
      ));

    mockMvc.perform(post("/api/agent/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"task\":\"Write a hello world program\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.task")
        .value("Write a hello world program"))
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.message")
        .value("Here is a simple Java hello world program."));
  }

  @Test
  void createTaskReturnsHttp400ForBlankTask() throws Exception {

    mockMvc.perform(post("/api/agent/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"task\":\"\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(400))
      .andExpect(jsonPath("$.error").value("Bad Request"))
      .andExpect(jsonPath("$.message")
        .value("Request validation failed"))
      .andExpect(jsonPath("$.fieldErrors[0].field").value("task"))
      .andExpect(jsonPath("$.fieldErrors[0].message")
        .value("task must not be blank"));
  }

  @Test
  void generateVerifyAndRunReturnsVerifiedResponse() throws Exception {

    when(codeAgentService.generateVerifyAndRun(
      "Reverse hello",
      "olleh"
    )).thenReturn(new CodeAgentResponse(
      "Reverse hello",
      "ReverseHello.java",
      "public class ReverseHello {}",
      "Reverses the string",
      true,
      1,
      "VERIFIED",
      "Compilation successful.",
      true,
      "olleh\n"
    ));

    mockMvc.perform(post("/api/agent/code/run/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                  "task": "Reverse hello",
                                  "expectedOutput": "olleh"
                                }
                                """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("VERIFIED"))
      .andExpect(jsonPath("$.compiled").value(true))
      .andExpect(jsonPath("$.executed").value(true))
      .andExpect(jsonPath("$.executionOutput").value("olleh\n"));

    verify(codeAgentService).generateVerifyAndRun(
      "Reverse hello",
      "olleh"
    );
  }

  @Test
  void generateVerifyAndRunReturnsHttp400ForBlankExpectedOutput()
    throws Exception {

    mockMvc.perform(post("/api/agent/code/run/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                  "task": "Reverse hello",
                                  "expectedOutput": ""
                                }
                                """))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(400));
  }
}
