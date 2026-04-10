package com.pyrite.controller;

import com.pyrite.dto.ExecutionRequest;
import com.pyrite.dto.ExecutionResponse;
import com.pyrite.service.PythonExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP layer: maps URLs to Java methods (the "web API").
 * <p>
 * {@link RestController} tells Spring that return values should be serialized to JSON (not HTML view names).
 * {@link RequestMapping @RequestMapping("/api")} prefixes every handler in this class with {@code /api}.
 * <p>
 * Spring creates one instance of this class and injects {@link PythonExecutionService} via the constructor—
 * that is "constructor injection" (preferred over field injection because dependencies are explicit).
 */
@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final PythonExecutionService pythonExecutionService;

    public ExecutionController(PythonExecutionService pythonExecutionService) {
        this.pythonExecutionService = pythonExecutionService;
    }

    /**
     * Handles {@code POST /api/execute}.
     * <ul>
     *   <li>{@link RequestBody} — JSON in the HTTP body is converted to an {@link ExecutionRequest}.</li>
     *   <li>{@link Valid} — runs validation rules defined on {@link ExecutionRequest} (e.g. code not blank).</li>
     *   <li>If validation fails, {@link GlobalExceptionHandler} turns it into HTTP 400.</li>
     * </ul>
     */
    @PostMapping(value = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ExecutionResponse execute(@Valid @RequestBody ExecutionRequest request) throws Exception {
        return pythonExecutionService.execute(request.code(), request.stdin(), request.timeoutSeconds());
    }
}
