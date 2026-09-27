package com.chess.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Throw this from any service-layer code when you want a specific HTTP
 * status returned to the client (e.g. 404 for "game not found", 409 for
 * "username already taken"), instead of letting an unchecked exception
 * bubble up as a generic 500.
 *
 * Pair with GlobalExceptionHandlerBase, which knows how to turn this into
 * an ErrorResponse.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // Convenience factories for the errors you'll reach for most often.

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }
}
