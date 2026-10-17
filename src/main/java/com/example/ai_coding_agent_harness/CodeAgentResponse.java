
package com.example.ai_coding_agent_harness;

public record CodeAgentResponse(
  String task,
  String fileName,
  String code,
  String explanation,
  boolean compiled,
  int attempts,
  String status,
  String compilerOutput,
  boolean executed,
  String executionOutput
) {}
