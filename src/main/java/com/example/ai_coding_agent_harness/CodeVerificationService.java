package com.example.ai_coding_agent_harness;

import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CodeVerificationService {

    public VerificationResponse verify(
            JavaCodeResponse generatedCode) throws IOException {

        String fileName = generatedCode.fileName();

        // Basic filename validation
        if (fileName == null ||
                !fileName.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java")) {
            return new VerificationResponse(
                    fileName, false, "INVALID_FILENAME",
                    "A valid Java filename is required."
            );
        }

        if (generatedCode.code() == null ||
                generatedCode.code().isBlank()) {
            return new VerificationResponse(
                    fileName, false, "EMPTY_CODE",
                    "The generated Java code is empty."
            );
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            return new VerificationResponse(
                    fileName, false, "COMPILER_UNAVAILABLE",
                    "A full JDK is required. Check your Java installation."
            );
        }

        Path workspace = Files.createTempDirectory("ai-code-verify-");

        try {
            Path sourceFile = workspace.resolve(fileName);
            Files.writeString(
                    sourceFile,
                    generatedCode.code(),
                    StandardCharsets.UTF_8
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            int result = compiler.run(
                    null,
                    output,
                    output,
                    "-proc:none",
                    "-d", workspace.toString(),
                    sourceFile.toString()
            );

            String compilerOutput =
                    output.toString(StandardCharsets.UTF_8);

            return new VerificationResponse(
                    fileName,
                    result == 0,
                    result == 0 ? "COMPILED" : "COMPILATION_FAILED",
                    compilerOutput.isBlank()
                            ? (result == 0
                                ? "Compilation successful."
                                : "Compilation failed.")
                            : compilerOutput
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
}