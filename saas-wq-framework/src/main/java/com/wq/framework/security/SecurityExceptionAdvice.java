package com.wq.framework.security;

import com.wq.common.api.Result;
import com.wq.common.api.ResultCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates method level authorization failures raised inside controllers.
 *
 * <p>It takes precedence over the generic advice so that a denied
 * {@code @PreAuthorize} call becomes a 403 instead of a 500.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.failure(ResultCode.FORBIDDEN, "You are not allowed to perform this operation"));
    }
}
