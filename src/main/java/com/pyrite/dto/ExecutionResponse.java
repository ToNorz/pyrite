package com.pyrite.dto;

/**
 * JSON returned to the browser after a Python run. Spring serializes this record to JSON automatically.
 *
 * @param stdout    text written to standard output
 * @param stderr    text written to standard error
 * @param exitCode  process exit status (often 0 = success); {@code -1} may be used when the run did not finish cleanly
 * @param timedOut  {@code true} if the process was killed because it exceeded the configured timeout
 */
public record ExecutionResponse(
        String stdout,
        String stderr,
        int exitCode,
        boolean timedOut
) {
}
