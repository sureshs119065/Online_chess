package com.chess.common.dto;

import java.time.Instant;

/**
 * Standard shape for every error response returned across all services, so
 * the frontend (or Postman during dev) only has to handle one error format
 * no matter which microservice produced it.
 *
 * Kept as a plain immutable record rather than a Lombok-annotated class —
 * one less dependency to keep in sync across every module.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
