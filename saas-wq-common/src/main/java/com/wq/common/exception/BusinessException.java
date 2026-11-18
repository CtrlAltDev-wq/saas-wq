package com.wq.common.exception;

import com.wq.common.api.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Domain level exception carrying both a business code and the HTTP status to expose.
 *
 * <p>Throwing this exception from any layer is enough to produce a consistent error
 * response, because {@link GlobalExceptionHandler} translates it automatically.</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    private final HttpStatus httpStatus;

    /**
     * Creates an exception using the default message of the given code.
     *
     * @param resultCode result code
     */
    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param resultCode result code
     * @param message    custom message
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.httpStatus = resultCode.getHttpStatus();
    }
}
