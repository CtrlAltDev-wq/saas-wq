package com.wq.common.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Central catalogue of business result codes.
 *
 * <p>Each entry couples a business code with the real HTTP status that must be returned
 * to the caller, keeping transport semantics and business semantics aligned.</p>
 */
@Getter
public enum ResultCode {

    SUCCESS(200, HttpStatus.OK, "Success"),
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "Bad request"),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(405, HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed"),
    CONFLICT(409, HttpStatus.CONFLICT, "Resource conflict"),
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    TENANT_REQUIRED(1001, HttpStatus.UNAUTHORIZED, "Tenant context is missing"),
    TENANT_NOT_FOUND(1002, HttpStatus.NOT_FOUND, "Tenant not found"),
    TENANT_DISABLED(1003, HttpStatus.FORBIDDEN, "Tenant is disabled"),
    INVALID_CREDENTIALS(1004, HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    USER_DISABLED(1005, HttpStatus.FORBIDDEN, "User account is disabled"),
    PLATFORM_TENANT_PROTECTED(1006, HttpStatus.FORBIDDEN, "Platform tenant is protected"),
    INVALID_ROLE(1007, HttpStatus.BAD_REQUEST, "Unsupported role");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ResultCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
