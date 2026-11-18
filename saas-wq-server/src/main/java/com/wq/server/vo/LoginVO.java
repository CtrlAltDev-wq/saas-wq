package com.wq.server.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Issued token and the authenticated profile.
 *
 * @param accessToken JWT access token
 * @param tokenType   always {@code Bearer}
 * @param expiresIn   lifetime in seconds
 * @param user        authenticated profile
 */
@Schema(description = "Login result")
public record LoginVO(String accessToken, String tokenType, long expiresIn, UserVO user) {
}
