
# AI Coding Agent with Verification Harness

An AI-powered Java coding agent built with Java 21, Spring Boot, Spring AI, Ollama, and Docker. It generates Java code from natural-language tasks, verifies compilation, executes programs in a constrained Docker container, checks expected output, and attempts automatic repairs when verification fails.

## Features

- **Local AI code generation:** Uses Ollama with the `qwen3:4b` model.
- **Java compilation verification:** Checks generated code using the Java compiler.
- **Automatic code repair:** Sends compiler errors back to the model for correction.
- **Bounded retry mechanism:** Limits repair attempts.
- **Constrained execution:** Runs generated programs inside Docker containers.
- **Resource controls:** Configures memory, CPU, process-count, network, and filesystem restrictions.
- **Execution timeout:** Stops programs that exceed the configured time limit.
- **Bounded output capture:** Limits captured output to 64 KB.
- **Expected-output verification:** Compares actual output with the expected result.
- **Automated tests:** Covers execution, retry behavior, validation, timeout, and output limits.

## Technology Stack

- Java 21
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Ollama with Qwen3 4B
- Maven
- Docker
- JUnit and Mockito

## Architecture

```text
Client
  |
  v
Spring Boot REST API
  |
  v
Code Agent Service
  |
  +--> AI Code Generation (Ollama)
  |
  +--> Java Compilation Verification
  |        |
  |        +--> Repair code when compilation fails
  |
  +--> Docker-based Execution
  |        |
  |        +--> Execution timeout and output limit
  |
  +--> Expected-output Verification
           |
           +--> Repair and retry within configured limits
  |
  v
Structured API Response
```

## How It Works

1. A client submits a coding task and, for verified execution, an expected output.
2. Ollama generates Java source code.
3. The harness validates the filename and source size.
4. The Java compiler checks whether the code compiles.
5. Compilation errors can be sent back to the model for repair.
6. The generated program runs inside a constrained Docker container.
7. The harness compares actual output with the expected output when requested.
8. Failed verification can trigger another repair attempt, within the configured retry limit.
9. The API returns the generated code, verification status, attempt count, and execution details.

## Prerequisites

Install:

- JDK 21
- Maven
- Docker Desktop
- Ollama

Download the local model:

```bash
ollama pull qwen3:4b
```

Make sure Docker Desktop and Ollama are running.

## Run the Application

From the project root:

```bash
mvn spring-boot:run
```

The API will normally be available at:

`http://localhost:8080`

## Run Tests

```bash
mvn test
```

The latest recorded development test run completed with **18 tests passed, zero failures, and zero errors**.

## API Endpoints


| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/agent/health` | Check application health |
| POST | `/api/agent/tasks` | Submit a coding task |
| POST | `/api/agent/code` | Generate Java code |
| POST | `/api/agent/verify` | Generate code and verify compilation |
| POST | `/api/agent/code/auto-fix` | Generate code and attempt compilation repairs |
| POST | `/api/agent/code/run` | Generate and verify code (as currently implemented) |
| POST | `/api/agent/execute` | Execute supplied Java code |
| POST | `/api/agent/execute/verify?expectedOutput=Hello%20World` | Execute supplied code and compare output |
| POST | `/api/agent/code/run/verify` | Generate, compile, execute, compare output, and retry repairs |


### Example: Generate, Execute, and Verify Code

**Endpoint:** `POST /api/agent/code/run/verify`

Request:

```json
{
  "task": "Write a Java program that prints Hello World.",
  "expectedOutput": "Hello World"
}
```

Illustrative response:

```json
{
  "task": "Write a Java program that prints Hello World.",
  "fileName": "HelloWorld.java",
  "code": "public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
  "explanation": "Prints Hello World.",
  "compiled": true,
  "attempts": 1,
  "status": "VERIFIED",
  "compilerOutput": "",
  "executed": true,
  "executionOutput": "Hello World\n"
}
```

This response is illustrative; actual generated code, explanation, output, and attempt count may differ.

### Example: Generate Java Code


**Endpoint:** `POST /api/agent/code`

Request:

```json
{
  "task": "Write a Java program to add two numbers."
}
```

The endpoint returns the generated Java filename, source code, and explanation.

### Example: Execute and Verify Supplied Java Code

**Endpoint:** `POST /api/agent/execute/verify?expectedOutput=Hello%20World`

Request:

```json
{
  "fileName": "HelloWorld.java",
  "code": "public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
  "explanation": "Prints Hello World."
}
```

Successful response:

```json
{
  "fileName": "HelloWorld.java",
  "successful": true,
  "status": "VERIFIED",
  "output": "Hello World\n",
  "message": "Program output matches the expected output."
}
```

## Input Validation

Task requests require a nonblank task of no more than 2,000 characters. The verified-run endpoint also validates the expected output.

## Security Considerations

Generated code is untrusted input. The execution harness applies Docker restrictions, including disabled container networking, memory and CPU limits, a process-count limit, a read-only root filesystem, dropped Linux capabilities, and a non-root user.

These controls reduce risk but do not guarantee complete isolation. Additional hardening and adversarial testing are required before production use.

## Project Goals

This project demonstrates:

- AI-assisted software development
- Spring Boot REST API design
- Service-layer orchestration
- Java compiler integration
- Bounded retries and automatic repair
- Docker-based process isolation
- Input validation and resource controls
- Automated testing with JUnit and Mockito

## Future Improvements

- Add execution and verification metrics.
- Persist task history and execution results.
- Add configurable model and execution policies.
- Improve container lifecycle monitoring.
- Expand integration and adversarial tests.
- Build a web UI for submitting coding tasks.

## Status

Active development. Core code generation, compilation verification, execution, bounded retries, and regression tests are implemented.
