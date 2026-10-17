
package com.example.ai_coding_agent_harness;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final ChatClient chatClient;

    public AgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AgentTaskResponse process(AgentTaskRequest request) {

        String answer = chatClient.prompt()
                .system("""
                        You are a helpful AI coding assistant.
                        Explain programming concepts in simple language.
                        Provide clear Java examples when requested.
                        Mention important edge cases when relevant.
                        """)
                .user(request.task())
                .call()
                .content();

        return new AgentTaskResponse(
                request.task(),
                "COMPLETED",
                answer
        );
    }

  public JavaCodeResponse generateCode(AgentTaskRequest request) {
    String rawResponse = chatClient.prompt()
        .system("""
            You are a Java 21 coding assistant.
            Return a valid JSON object with exactly three fields:
            fileName, code, explanation.
            Generate complete, simple Java source code.
            Include necessary imports and a main method for standalone programs.
            Do not include Markdown fences.
            Keep the explanation under 50 words.
            """)
        .user(request.task())
        .call()
        .content();

    System.out.println("RAW MODEL RESPONSE: " + rawResponse);

    if (rawResponse == null || rawResponse.isBlank()) {
        throw new IllegalStateException("Ollama returned an empty response.");
    }

    return new org.springframework.ai.converter.BeanOutputConverter<>(
        JavaCodeResponse.class
    ).convert(rawResponse);
}

public JavaCodeResponse repairCode(
        JavaCodeResponse previousCode,
        String compilerErrors,
        String originalTask) {

    String prompt = """
            You are a Java coding assistant.
            Fix the Java compilation errors in the code below.

            Original task:
            %s

            Filename:
            %s

            Current Java code:
            %s

            Compiler errors:
            %s

            Requirements:
            1. Return corrected Java code.
            2. Keep the same filename.
            3. Return valid Java code that compiles.
            4. Do not include Markdown code fences.
            5. Return the result in the requested JSON structure.
            """.formatted(
            originalTask,
            previousCode.fileName(),
            previousCode.code(),
            compilerErrors
    );

    String rawResponse = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

    if (rawResponse == null || rawResponse.isBlank()) {
        throw new IllegalStateException(
                "AI returned an empty response while repairing code."
        );
    }

    return new org.springframework.ai.converter.BeanOutputConverter<>(
            JavaCodeResponse.class
    ).convert(rawResponse);
}

}
