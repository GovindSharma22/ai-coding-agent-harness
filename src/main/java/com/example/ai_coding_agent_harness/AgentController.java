
package com.example.ai_coding_agent_harness;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class AgentController {

  private final AgentService agentService;
  private final CodeVerificationService verificationService;
  private final CodeAgentService codeAgentService;
  private final CodeExecutionService executionService;

  public AgentController(
    AgentService agentService,
    CodeVerificationService verificationService,
    CodeAgentService codeAgentService,
    CodeExecutionService executionService) {

    this.agentService = agentService;
    this.verificationService = verificationService;
    this.codeAgentService = codeAgentService;
    this.executionService = executionService;
  }

  @PostMapping("/api/agent/tasks")
  public AgentTaskResponse createTask(
    @Valid @RequestBody AgentTaskRequest request) {

    return agentService.process(request);
  }

  @PostMapping("/api/agent/code")
  public JavaCodeResponse generateCode(
    @Valid @RequestBody AgentTaskRequest request) {

    return agentService.generateCode(request);
  }

  @PostMapping("/api/agent/verify")
  public VerificationResponse verifyCode(
    @Valid @RequestBody AgentTaskRequest request)
    throws IOException {

    JavaCodeResponse generatedCode =
      agentService.generateCode(request);

    return verificationService.verify(generatedCode);
  }

  @PostMapping("/api/agent/code/auto-fix")
  public CodeAgentResponse generateAndVerify(
    @Valid @RequestBody AgentTaskRequest request)
    throws Exception {

    return codeAgentService.generateAndVerify(request.task());
  }
  @PostMapping("/api/agent/code/run")
  public CodeAgentResponse generateAndRun(
    @Valid @RequestBody AgentTaskRequest request)
    throws Exception {

    return codeAgentService.generateAndVerify(request.task());
  }

  @PostMapping("/api/agent/execute")
  public ExecutionResponse executeCode(
    @Valid @RequestBody JavaCodeResponse request)
    throws IOException, InterruptedException {

    return executionService.execute(request);
  }

  @PostMapping("/api/agent/execute/verify")
  public ExecutionResponse executeAndVerify(
    @Valid @RequestBody JavaCodeResponse request,
    @RequestParam String expectedOutput)
    throws IOException, InterruptedException {

    return executionService.executeAndVerify(
      request, expectedOutput);
  }

  @PostMapping("/api/agent/code/run/verify")
  public CodeAgentResponse generateVerifyAndRun(
    @Valid @RequestBody VerifiedCodeRequest request)
    throws Exception {

    return codeAgentService.generateVerifyAndRun(
      request.task(),
      request.expectedOutput()
    );
  }

}
