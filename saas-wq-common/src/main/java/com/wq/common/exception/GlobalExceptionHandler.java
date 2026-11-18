package com.wq.common.exception;

import com.wq.common.api.Result;
import com.wq.common.api.ResultCode;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global translator between exceptions and the unified response envelope.
 *
 * <p>Unlike the common "always return HTTP 200" pattern, the status line is kept
 * meaningful: a missing resource yields 404, a validation failure yields 400 and an
 * unexpected failure yields 500, while the body still carries a business code.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles expected domain failures.
     *
     * @param exception domain exception
     * @return error response with the HTTP status declared by the result code
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        log.warn("Business rule rejected request: code={}, message={}", exception.getCode(), exception.getMessage());
        return build(exception.getHttpStatus(), Result.failure(exception.getCode(), exception.getMessage()));
    }

    /**
     * Handles request body validation failures.
     *
     * @param exception validation exception
     * @return 400 response listing every violated field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, Result.failure(ResultCode.BAD_REQUEST, message));
    }

    /**
     * Handles validation failures on request parameters and path variables.
     *
     * @param exception constraint violation exception
     * @return 400 response listing every violated constraint
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, Result.failure(ResultCode.BAD_REQUEST, message));
    }

    /**
     * Handles unreadable request bodies.
     *
     * @param exception parse exception
     * @return 400 response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.debug("Malformed request body: {}", exception.getMessage());
        return build(HttpStatus.BAD_REQUEST, Result.failure(ResultCode.BAD_REQUEST, "Malformed request body"));
    }

    /**
     * Handles missing mandatory query parameters.
     *
     * @param exception parameter exception
     * @return 400 response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        String message = "Missing required parameter: " + exception.getParameterName();
        return build(HttpStatus.BAD_REQUEST, Result.failure(ResultCode.BAD_REQUEST, message));
    }

    /**
     * Handles parameters declared with an incompatible type.
     *
     * @param exception type mismatch exception
     * @return 400 response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = "Invalid value for parameter: " + exception.getName();
        return build(HttpStatus.BAD_REQUEST, Result.failure(ResultCode.BAD_REQUEST, message));
    }

    /**
     * Handles unsupported HTTP methods.
     *
     * @param exception method exception
     * @return 405 response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, Result.failure(ResultCode.METHOD_NOT_ALLOWED));
    }

    /**
     * Handles requests targeting a non existing resource.
     *
     * @param exception resource exception
     * @return 404 response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND, Result.failure(ResultCode.NOT_FOUND));
    }

    /**
     * Last resort handler for unexpected failures.
     *
     * @param exception unexpected exception
     * @return 500 response without leaking internal details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                Result.failure(ResultCode.INTERNAL_SERVER_ERROR));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<Result<Void>> build(HttpStatus status, Result<Void> body) {
        return ResponseEntity.status(status).body(body);
    }
}
