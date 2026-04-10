package com.pyrite.service;

import com.pyrite.dto.ExecutionResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs Python code as a separate operating-system process and captures its output.
 * <p>
 * {@link org.springframework.stereotype.Service @Service} registers this class as a Spring bean so
 * {@link com.pyrite.controller.ExecutionController} can receive it by constructor injection.
 * <p>
 * <strong>Why {@link ProcessBuilder}?</strong> It starts {@code python3 /path/to/script.py} as a real process
 * without going through a shell, which avoids shell-injection issues and keeps arguments explicit.
 * <p>
 * <strong>Why read stdout/stderr on background threads?</strong> If the process fills an output pipe and we do
 * not read it, the process can block; reading both streams concurrently avoids deadlock when both are used.
 */
@Service
public class PythonExecutionService {

    private static final String PYTHON = "python3";

    /**
     * Writes {@code code} to a temporary {@code .py} file, runs {@code python3} on it, sends {@code stdin} to
     * the process, waits up to {@code timeoutSeconds}, then returns captured output or a timeout result.
     */
    public ExecutionResponse execute(String code, String stdin, int timeoutSeconds) throws IOException {
        Path scriptPath = Files.createTempFile("pyrite_", ".py");
        try {
            // 1) Materialize user code as a file — Python expects a path, and multi-line code is awkward on a CLI.
            Files.writeString(scriptPath, code, StandardCharsets.UTF_8);

            // 2) Start python3 with the script path as the only argument (no shell).
            ProcessBuilder pb = new ProcessBuilder(PYTHON, scriptPath.toAbsolutePath().toString());
            pb.redirectErrorStream(false); // keep stderr separate from stdout so we can show both
            Process process = pb.start();

            // 3) Begin reading stdout/stderr immediately so pipes cannot fill and block the child process.
            CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
            CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

            // 4) Send stdin and close the stream — closing signals "no more input" to the Python process.
            pipeStdinAndClose(process, stdin);

            // 5) Wait for exit or timeout; on timeout, kill the process.
            boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finishedInTime) {
                process.destroyForcibly();
                waitQuietly(process);
                return new ExecutionResponse("", "", -1, true);
            }

            // 6) Collect stream contents (process has exited, so streams should finish).
            String out = awaitStream(stdoutFuture, timeoutSeconds);
            String err = awaitStream(stderrFuture, timeoutSeconds);
            return new ExecutionResponse(out, err, process.exitValue(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag — good practice when catching InterruptedException
            return new ExecutionResponse("", "Execution interrupted", -1, false);
        } finally {
            Files.deleteIfExists(scriptPath);
        }
    }

    private static void pipeStdinAndClose(Process process, String stdin) throws IOException {
        try (OutputStream os = process.getOutputStream()) {
            if (stdin != null && !stdin.isEmpty()) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /** After destroyForcibly(), give the OS a moment to reap the process (avoids noisy warnings in some setups). */
    private static void waitQuietly(Process process) {
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reads an entire stream on the common ForkJoin pool so the main flow can wait on {@link Process#waitFor}.
     * The stream is closed when done, which matches the process lifecycle.
     */
    private static CompletableFuture<String> readStreamAsync(InputStream in) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream stream = in) {
                return readAllBytes(stream);
            } catch (IOException e) {
                return "";
            }
        });
    }

    private static String awaitStream(CompletableFuture<String> future, int timeoutSeconds) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            future.cancel(true);
            return "";
        } catch (ExecutionException e) {
            return "";
        }
    }

    private static String readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        in.transferTo(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }
}
