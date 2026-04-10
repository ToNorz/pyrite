package com.pyrite.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central place for turning exceptions into HTTP responses.
 * <p>
 * {@link org.springframework.web.bind.annotation.RestControllerAdvice RestControllerAdvice} applies to all
 * {@link org.springframework.web.bind.annotation.RestController RestController} beans. Methods here run when
 * a matching exception is thrown during request handling.
 * <p>
 * Without this, validation failures might produce a generic error page; here we return JSON
 * {@code {"error": "..."}} with status 400 so the React client can show a clear message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
