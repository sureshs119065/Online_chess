package com.chess.common.exception;

import com.chess.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Extend this in each service with a class annotated {@code @RestControllerAdvice},
 * e.g.:
 *
 * <pre>{@code
 * @RestControllerAdvice
 * public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {
 *     // add service-specific @ExceptionHandler methods here if needed
 * }
 * }</pre>
 *
 * Spring picks up {@code @ExceptionHandler} methods declared on a superclass
 * as long as the concrete subclass itself carries {@code @RestControllerAdvice}
 * — this base class stays a plain class, not the advice bean, so every
 * service gets consistent error handling without copy-pasting these methods.
 */
public abstract class GlobalExceptionHandlerBase {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message.isBlank() ? "Validation failed" : message,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Something went wrong. Please try again.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
