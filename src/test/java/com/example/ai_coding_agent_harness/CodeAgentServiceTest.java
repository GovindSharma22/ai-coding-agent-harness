
package com.example.ai_coding_agent_harness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CodeAgentServiceTest {

  private final AgentService agentService = mock(AgentService.class);
  private final CodeVerificationService verificationService =
    mock(CodeVerificationService.class);
  private final CodeExecutionService executionService =
    mock(CodeExecutionService.class);

  private final CodeAgentService codeAgentService =
    new CodeAgentService(
      agentService,
      verificationService,
      executionService
    );

  @Test
  void shouldRepairCodeWhenOutputDoesNotMatch() throws Exception {

    JavaCodeResponse incorrectCode = new JavaCodeResponse(
      "ReverseHello.java",
      "public class ReverseHello {}",
      "Initial implementation"
    );

    JavaCodeResponse correctedCode = new JavaCodeResponse(
      "ReverseHello.java",
      "public class ReverseHello { public static void main(String[] args) { System.out.println(\"olleh\"); } }",
      "Corrected implementation"
    );

    when(agentService.generateCode(any(AgentTaskRequest.class)))
      .thenReturn(incorrectCode);

    when(verificationService.verify(any(JavaCodeResponse.class)))
      .thenReturn(
        new VerificationResponse(
          "ReverseHello.java",
          true,
          "COMPILED",
          "Compilation successful."
        )
      );

    when(executionService.executeAndVerify(
      eq(incorrectCode), eq("olleh")))
      .thenReturn(new ExecutionResponse(
        "ReverseHello.java",
        false,
        "OUTPUT_MISMATCH",
        "hello",
        "Output did not match expected output"
      ));

    when(agentService.repairCode(
      eq(incorrectCode), any(String.class), eq("Reverse hello")))
      .thenReturn(correctedCode);

    when(executionService.executeAndVerify(
      eq(correctedCode), eq("olleh")))
      .thenReturn(new ExecutionResponse(
        "ReverseHello.java",
        true,
        "VERIFIED",
        "olleh\n",
        "Output matches expected output"
      ));

    CodeAgentResponse response =
      codeAgentService.generateVerifyAndRun(
        "Reverse hello",
        "olleh"
      );

    assertEquals("VERIFIED", response.status());
    assertTrue(response.compiled());
    assertTrue(response.executed());
    assertEquals(2, response.attempts());
    assertEquals("olleh\n", response.executionOutput());

    verify(agentService, times(1)).repairCode(
      eq(incorrectCode), any(String.class), eq("Reverse hello"));
  }

  @Test
  void shouldStopAfterMaximumAttempts() throws Exception {

    JavaCodeResponse code = new JavaCodeResponse(
      "ReverseHello.java",
      "public class ReverseHello {}",
      "Test implementation"
    );

    when(agentService.generateCode(any(AgentTaskRequest.class)))
      .thenReturn(code);

    when(verificationService.verify(any(JavaCodeResponse.class)))
      .thenReturn(new VerificationResponse(
        "ReverseHello.java",
        true,
        "COMPILED",
        "Compilation successful."
      ));

    when(executionService.executeAndVerify(
      any(JavaCodeResponse.class), eq("olleh")))
      .thenReturn(new ExecutionResponse(
        "ReverseHello.java",
        false,
        "OUTPUT_MISMATCH",
        "hello",
        "Output did not match"
      ));

    when(agentService.repairCode(
      any(JavaCodeResponse.class),
      any(String.class),
      eq("Reverse hello")))
      .thenReturn(code);

    CodeAgentResponse response =
      codeAgentService.generateVerifyAndRun(
        "Reverse hello",
        "olleh"
      );

    assertEquals(3, response.attempts());
    assertNotEquals("VERIFIED", response.status());
    assertFalse(response.executed());

    verify(executionService, times(3)).executeAndVerify(
      any(JavaCodeResponse.class), eq("olleh"));

    verify(agentService, times(2)).repairCode(
      any(JavaCodeResponse.class),
      any(String.class),
      eq("Reverse hello"));
  }
}
