package com.example.ai_coding_agent_harness;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ValidationErrorResponse handleValidation(MethodArgumentNotValidException exception) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorResponse(
						error.getField(),
						error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid value"
				))
				.toList();

		return new ValidationErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Request validation failed",
				fieldErrors
		);
	}

	public record ValidationErrorResponse(
			int status,
			String error,
			String message,
			List<FieldErrorResponse> fieldErrors
	) {
	}

	public record FieldErrorResponse(String field, String message) {
	}

}
