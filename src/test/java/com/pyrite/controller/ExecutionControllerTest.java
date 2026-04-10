package com.pyrite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrite.dto.ExecutionRequest;
import com.pyrite.dto.ExecutionResponse;
import com.pyrite.service.PythonExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the HTTP layer without starting the full server and without running real Python.
 * <p>
 * {@link MockMvc} simulates HTTP requests against a Spring {@link org.springframework.web.servlet.DispatcherServlet}.
 * {@link MockMvcBuilders#standaloneSetup(Object...) standaloneSetup} wires only the controller (and optional
 * {@link GlobalExceptionHandler})—no database, no full Spring context—so tests stay fast and simple.
 * <p>
 * We use an <strong>anonymous subclass</strong> of {@link PythonExecutionService} to return a fixed response
 * instead of Mockito (avoids tooling issues on some JDK versions).
 */
class ExecutionControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PythonExecutionService stub = new PythonExecutionService() {
            @Override
            public ExecutionResponse execute(String code, String stdin, int timeoutSeconds) throws IOException {
                return new ExecutionResponse("ok\n", "", 0, false);
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new ExecutionController(stub))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void executeReturnsResult() throws Exception {
        ExecutionRequest body = new ExecutionRequest("print(1)", "", 30);
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stdout").value("ok\n"))
                .andExpect(jsonPath("$.exitCode").value(0))
                .andExpect(jsonPath("$.timedOut").value(false));
    }

    @Test
    void executeRejectsBlankCode() throws Exception {
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"stdin\":\"\",\"timeoutSeconds\":30}"))
                .andExpect(status().isBadRequest());
    }
}
