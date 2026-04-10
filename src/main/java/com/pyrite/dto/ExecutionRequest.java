package com.pyrite.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of {@code POST /api/execute}, deserialized from the HTTP request.
 * <p>
 * A <strong>record</strong> is an immutable data carrier: the compiler generates constructor, accessors
 * ({@code code()}, {@code stdin()}, …), {@code equals}, {@code hashCode}, and {@code toString}.
 * <p>
 * The <strong>compact constructor</strong> {@code public ExecutionRequest { ... }} runs after parameters are
 * read but before the record is built; we use it to replace null optional fields with defaults.
 * <p>
 * Annotations like {@link NotBlank} and {@link Size} are evaluated when {@code @Valid} is used on the controller
 * parameter (see {@link com.pyrite.controller.ExecutionController}).
 */
public record ExecutionRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 1_048_576, message = "Code must be at most 1MB")
        String code,
        @Size(max = 1_048_576, message = "Stdin must be at most 1MB")
        String stdin,
        @Min(value = 1, message = "Timeout must be at least 1 second")
        @Max(value = 300, message = "Timeout must be at most 300 seconds")
        Integer timeoutSeconds
) {
    public ExecutionRequest {
        if (stdin == null) {
            stdin = "";
        }
        if (timeoutSeconds == null) {
            timeoutSeconds = 30;
        }
    }
}
