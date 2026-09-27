package com.chess.auth.exception;

import com.chess.common.exception.GlobalExceptionHandlerBase;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * All the actual @ExceptionHandler methods live in common-lib's
 * GlobalExceptionHandlerBase — this class just needs to exist, carrying
 * @RestControllerAdvice, for Spring to register those inherited handlers.
 * Add auth-service-specific handlers here if/when a case comes up that
 * doesn't fit the shared base.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {
}
