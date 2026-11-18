package com.wq.server.service;

import com.wq.server.dto.LoginRequest;
import com.wq.server.vo.LoginVO;
import com.wq.server.vo.UserVO;

/**
 * Authentication use cases.
 */
public interface AuthService {

    /**
     * Authenticates a tenant scoped user and issues an access token.
     *
     * @param request login payload
     * @return token and profile
     */
    LoginVO login(LoginRequest request);

    /**
     * Revokes the supplied access token until its natural expiration.
     *
     * @param authorizationHeader raw {@code Authorization} header
     */
    void logout(String authorizationHeader);

    /**
     * Returns the profile of the authenticated user.
     *
     * @return authenticated profile
     */
    UserVO currentUser();
}
