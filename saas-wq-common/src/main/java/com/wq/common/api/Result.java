package com.wq.common.api;

/**
 * Unified response envelope returned by every REST endpoint.
 *
 * <p>The HTTP status code reflects the transport level outcome, while {@code code}
 * carries the business level outcome. Both are stable and documented for API consumers.</p>
 *
 * @param code      business code, see {@link ResultCode}
 * @param message   human readable message
 * @param data      response payload, absent for void operations
 * @param timestamp epoch milliseconds when the response was produced
 * @param <T>       payload type
 */
public record Result<T>(int code, String message, T data, long timestamp) {

    /**
     * Builds a successful response without payload.
     *
     * @return unified success response
     */
    public static Result<Void> success() {
        return success(null);
    }

    /**
     * Builds a successful response carrying a payload.
     *
     * @param data payload
     * @param <T>  payload type
     * @return unified success response
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data,
                System.currentTimeMillis());
    }

    /**
     * Builds a failure response using the default message of the given code.
     *
     * @param resultCode result code
     * @param <T>        payload type
     * @return unified failure response
     */
    public static <T> Result<T> failure(ResultCode resultCode) {
        return failure(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * Builds a failure response with a custom message.
     *
     * @param resultCode result code
     * @param message    custom message
     * @param <T>        payload type
     * @return unified failure response
     */
    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        return failure(resultCode.getCode(), message);
    }

    /**
     * Builds a failure response from raw values.
     *
     * @param code    business code
     * @param message custom message
     * @param <T>     payload type
     * @return unified failure response
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}
