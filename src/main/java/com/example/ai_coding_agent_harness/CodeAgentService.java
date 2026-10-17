
package com.example.ai_coding_agent_harness;

import org.springframework.stereotype.Service;

@Service
public class CodeAgentService {

  private static final int MAX_ATTEMPTS = 3;

  private final AgentService agentService;
  private final CodeVerificationService verificationService;
  private final CodeExecutionService executionService;

  public CodeAgentService(
    AgentService agentService,
    CodeVerificationService verificationService,
    CodeExecutionService executionService) {
    this.agentService = agentService;
    this.verificationService = verificationService;
    this.executionService = executionService;
  }

  public CodeAgentResponse generateAndVerify(String task)
    throws Exception {

    JavaCodeResponse code = agentService.generateCode(
      new AgentTaskRequest(task)
    );

    VerificationResponse verification =
      verificationService.verify(code);

    int attempts = 1;

    while (!verification.compiled() && attempts < MAX_ATTEMPTS) {
      code = agentService.repairCode(
        code,
        verification.compilerOutput(),
        task
      );

      verification = verificationService.verify(code);
      attempts++;
    }

    if (!verification.compiled()) {
      return new CodeAgentResponse(
        task,
        code.fileName(),
        code.code(),
        code.explanation(),
        false,
        attempts,
        "COMPILATION_FAILED",
        verification.compilerOutput(),
        false,
        ""
      );
    }

    ExecutionResponse execution = executionService.execute(code);

    return new CodeAgentResponse(
      task,
      code.fileName(),
      code.code(),
      code.explanation(),
      true,
      attempts,
      execution.successful()
        ? "EXECUTED"
        : execution.status(),
      verification.compilerOutput(),
      execution.successful(),
      execution.output()
    );
  }



  public CodeAgentResponse generateVerifyAndRun(
    String task,
    String expectedOutput) throws Exception {

    JavaCodeResponse code = agentService.generateCode(
      new AgentTaskRequest(task)
    );

    int attempts = 0;
    VerificationResponse verification = null;
    ExecutionResponse result = null;

    while (attempts < MAX_ATTEMPTS) {
      attempts++;

      // Step 1: Compile the generated code.
      verification = verificationService.verify(code);

      if (!verification.compiled()) {
        if (attempts < MAX_ATTEMPTS) {
          code = agentService.repairCode(
            code,
            verification.compilerOutput(),
            task
          );
          continue;
        }

        return new CodeAgentResponse(
          task,
          code.fileName(),
          code.code(),
          code.explanation(),
          false,
          attempts,
          "COMPILATION_FAILED",
          verification.compilerOutput(),
          false,
          ""
        );
      }

      // Step 2: Execute and compare with expected output.
      result = executionService.executeAndVerify(
        code,
        expectedOutput
      );

      if (result.successful()) {
        return new CodeAgentResponse(
          task,
          code.fileName(),
          code.code(),
          code.explanation(),
          true,
          attempts,
          "VERIFIED",
          verification.compilerOutput(),
          true,
          result.output()
        );
      }

      // Step 3: Repair runtime errors or incorrect output.
      if (attempts < MAX_ATTEMPTS) {
        String feedback =
          "Execution status: " + result.status()
            + "\nExecution output: " + result.output()
            + "\nExecution message: " + result.message()
            + "\nExpected output: " + expectedOutput
            + "\nPlease fix the Java code and return corrected code.";

        code = agentService.repairCode(
          code,
          feedback,
          task
        );
      }
    }

    // All attempts failed.
    return new CodeAgentResponse(
      task,
      code.fileName(),
      code.code(),
      code.explanation(),
      verification != null && verification.compiled(),
      attempts,
      result != null ? result.status() : "EXECUTION_FAILED",
      verification != null
        ? verification.compilerOutput()
        : "",
      false,
      result != null ? result.output() : ""
    );
  }


}
