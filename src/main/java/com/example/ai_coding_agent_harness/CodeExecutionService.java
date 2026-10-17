
package com.example.ai_coding_agent_harness;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

  private static final String IMAGE = "eclipse-temurin:21-jdk";
  private static final String DOCKER =
    "/Applications/Docker.app/Contents/Resources/bin/docker";

  private static final long TIMEOUT_SECONDS = 20;
  private static final long MAX_SOURCE_BYTES = 100 * 1024;
  private static final int MAX_OUTPUT_BYTES = 64 * 1024;

  public ExecutionResponse execute(JavaCodeResponse generatedCode)
    throws IOException, InterruptedException {

    String fileName = generatedCode.fileName();
    String code = generatedCode.code();

    if (fileName == null ||
      !fileName.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java")) {
      return new ExecutionResponse(
        fileName, false, "INVALID_FILENAME", "",
        "A valid Java filename is required."
      );
    }

    if (code == null || code.isBlank()) {
      return new ExecutionResponse(
        fileName, false, "EMPTY_CODE", "",
        "Generated Java code is empty."
      );
    }

    if (code.getBytes(StandardCharsets.UTF_8).length
      > MAX_SOURCE_BYTES) {
      return new ExecutionResponse(
        fileName, false, "SOURCE_TOO_LARGE", "",
        "Generated source code exceeds the 100 KB limit."
      );
    }

    String containerName = "ai-code-run-" +
      UUID.randomUUID().toString().replace("-", "");

    Path workspace =
      Files.createTempDirectory("ai-code-execution-");

    try {
      Files.writeString(
        workspace.resolve(fileName),
        code,
        StandardCharsets.UTF_8
      );

      ProcessBuilder builder = new ProcessBuilder(
        DOCKER, "run",
        "--name", containerName,
        "--rm",
        "--network", "none",
        "--memory", "256m",
        "--cpus", "0.5",
        "--pids-limit", "32",
        "--read-only",
        "--cap-drop", "ALL",
        "--security-opt", "no-new-privileges",
        "--user", "10001:10001",
        "--tmpfs",
        "/tmp:rw,nosuid,size=16m,uid=10001,gid=10001,mode=700",
        "--mount",
        "type=bind,source=" + workspace.toAbsolutePath()
          + ",target=/workspace,readonly",
        "--workdir", "/tmp",
        IMAGE,
        "sh", "-c",
        "cp /workspace/" + fileName
          + " /tmp/" + fileName
          + " && javac /tmp/" + fileName
          + " && java -cp /tmp "
          + fileName.substring(0, fileName.length() - 5)
      );

      builder.redirectErrorStream(true);
      Process process = builder.start();

      // Drain output concurrently to prevent pipe-buffer deadlocks.
      CompletableFuture<String> outputFuture =
        CompletableFuture.supplyAsync(() -> {
          try (InputStream input = process.getInputStream();
               ByteArrayOutputStream captured =
                 new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            boolean truncated = false;

            while ((bytesRead = input.read(buffer)) != -1) {
              int remaining =
                MAX_OUTPUT_BYTES - captured.size();

              if (remaining > 0) {
                captured.write(
                  buffer, 0,
                  Math.min(bytesRead, remaining)
                );
              }

              if (bytesRead > remaining) {
                truncated = true;
              }
            }

            String output =
              captured.toString(StandardCharsets.UTF_8);

            if (truncated) {
              output +=
                "\n[Output truncated: 64 KB limit reached]";
            }

            return output;

          } catch (IOException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });

      boolean finished = process.waitFor(
        TIMEOUT_SECONDS, TimeUnit.SECONDS);

      if (!finished) {
        process.destroyForcibly();
        process.waitFor();

        // Request explicit container cleanup if Docker is still running.
        try {
          Process cleanupProcess = new ProcessBuilder(
            DOCKER, "rm", "--force", containerName
          ).redirectErrorStream(true).start();

          if (!cleanupProcess.waitFor(5, TimeUnit.SECONDS)) {
            cleanupProcess.destroyForcibly();
            cleanupProcess.waitFor();
          }
        } catch (IOException cleanupError) {
          // The Docker process may already have removed the container.
        }

        outputFuture.cancel(true);

        return new ExecutionResponse(
          fileName,
          false,
          "TIMEOUT",
          "",
          "Execution exceeded " + TIMEOUT_SECONDS
            + " seconds. Container cleanup was requested."
        );
      }

      String output = outputFuture.join();
      boolean successful = process.exitValue() == 0;

      return new ExecutionResponse(
        fileName,
        successful,
        successful ? "EXECUTED" : "EXECUTION_FAILED",
        output,
        successful
          ? "Program completed successfully."
          : "Compilation or execution failed."
      );

    } finally {
      deleteWorkspace(workspace);
    }
  }

  private void deleteWorkspace(Path workspace) throws IOException {
    try (var paths = Files.walk(workspace)) {
      for (Path path : paths
        .sorted(java.util.Comparator.reverseOrder())
        .toList()) {
        Files.deleteIfExists(path);
      }
    }
  }

  public ExecutionResponse executeAndVerify(
    JavaCodeResponse generatedCode,
    String expectedOutput
  ) throws IOException, InterruptedException {

    ExecutionResponse execution = execute(generatedCode);

    if (!execution.successful()) {
      return execution;
    }

    boolean outputMatches =
      outputMatches(execution.output(), expectedOutput);

    return new ExecutionResponse(
      execution.fileName(),
      outputMatches,
      outputMatches ? "VERIFIED" : "OUTPUT_MISMATCH",
      execution.output(),
      outputMatches
        ? "Program output matches the expected output."
        : "Program ran, but its output did not match the expected output."
    );
  }

  public boolean outputMatches(
    String actualOutput,
    String expectedOutput) {

    if (actualOutput == null || expectedOutput == null) {
      return false;
    }

    return actualOutput.strip().equals(expectedOutput.strip());
  }
}
