
package com.example.ai_coding_agent_harness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeExecutionServiceTest {

  private final CodeExecutionService service =
    new CodeExecutionService();

  @Test
  void shouldMatchExpectedOutput() {
    boolean result = service.outputMatches(
      "EXECUTION_SUCCESS\n",
      "EXECUTION_SUCCESS"
    );

    assertTrue(result);
  }

  @Test
  void shouldRejectDifferentOutput() {
    boolean result = service.outputMatches(
      "ACTUAL_OUTPUT",
      "EXPECTED_OUTPUT"
    );

    assertFalse(result);
  }

  @Test
  void shouldRejectNullOutput() {
    assertFalse(service.outputMatches(null, "EXPECTED_OUTPUT"));
    assertFalse(service.outputMatches("ACTUAL_OUTPUT", null));
  }

  @Test
  void shouldExecuteJavaProgramInsideDocker() throws Exception {
    JavaCodeResponse code = new JavaCodeResponse(
      "Hello.java",
      """
      public class Hello {
          public static void main(String[] args) {
              System.out.println("EXECUTION_SUCCESS");
          }
      }
      """,
      "Prints a success message"
    );

    ExecutionResponse response = service.execute(code);

    assertTrue(
      response.successful(),
      () -> "Execution failed: " + response.status()
        + "\nOutput: " + response.output()
        + "\nMessage: " + response.message()
    );

    assertEquals("EXECUTED", response.status());
    assertEquals("EXECUTION_SUCCESS", response.output().strip());
  }


  @Test
  void shouldVerifyCorrectProgramOutput() throws Exception {
    JavaCodeResponse code = new JavaCodeResponse(
      "Hello.java",
      """
      public class Hello {
          public static void main(String[] args) {
              System.out.println("olleh");
          }
      }
      """,
      "Prints the reversed string"
    );

    ExecutionResponse response =
      service.executeAndVerify(code, "olleh");

    assertTrue(response.successful());
    assertEquals("VERIFIED", response.status());
    assertEquals("olleh", response.output().strip());
  }

  @Test
  void shouldRejectIncorrectProgramOutput() throws Exception {
    JavaCodeResponse code = new JavaCodeResponse(
      "Hello.java",
      """
      public class Hello {
          public static void main(String[] args) {
              System.out.println("hello");
          }
      }
      """,
      "Prints the original string"
    );

    ExecutionResponse response =
      service.executeAndVerify(code, "olleh");

    assertFalse(response.successful());
    assertEquals("OUTPUT_MISMATCH", response.status());
    assertEquals("hello", response.output().strip());
  }

  @Test
  void shouldRejectNullExpectedOutput() {
    assertFalse(service.outputMatches("olleh", null));
  }


  @Test
  void shouldRejectSourceCodeExceedingSizeLimit() throws Exception {
    String largeCode = "A".repeat(100 * 1024 + 1);

    JavaCodeResponse code = new JavaCodeResponse(
      "Hello.java",
      largeCode,
      "Oversized source"
    );

    ExecutionResponse response = service.execute(code);

    assertFalse(response.successful());
    assertEquals("SOURCE_TOO_LARGE", response.status());
  }


  @Test
  void shouldStopProgramThatExceedsExecutionTimeout() throws Exception {
    JavaCodeResponse code = new JavaCodeResponse(
      "InfiniteLoop.java",
      """
      public class InfiniteLoop {
          public static void main(String[] args) {
              while (true) {
                  // Intentionally infinite for timeout testing
              }
          }
      }
      """,
      "Tests execution timeout"
    );


    ExecutionResponse response = service.execute(code);

    assertFalse(response.successful());
    assertEquals("TIMEOUT", response.status());
  }


  @Test
  void shouldLimitProgramOutputTo64KB() throws Exception {
    JavaCodeResponse code = new JavaCodeResponse(
      "LargeOutput.java",
      """
      public class LargeOutput {
          public static void main(String[] args) {
              for (int i = 0; i < 10000; i++) {
                  System.out.println("0123456789012345678901234567890123456789");
              }
          }
      }
      """,
      "Tests the execution output limit"
    );

    ExecutionResponse response = service.execute(code);

    assertTrue(response.successful());
    assertTrue(response.output().contains(
      "[Output truncated: 64 KB limit reached]"
    ));
    assertTrue(response.output().length() < 66 * 1024);
  }


}
