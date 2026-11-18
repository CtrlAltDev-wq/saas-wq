package com.wq.server.service;

import com.wq.common.api.PageResult;
import com.wq.server.dto.UserCreateRequest;
import com.wq.server.dto.UserUpdateRequest;
import com.wq.server.vo.UserVO;

/**
 * User management use cases scoped to the authenticated tenant.
 */
public interface UserService {

    /**
     * Lists users of the current tenant.
     *
     * @param page    one based page index
     * @param size    page size
     * @param keyword optional match on username or nickname
     * @return paged users
     */
    PageResult<UserVO> page(long page, long size, String keyword);

    /**
     * Loads a user of the current tenant.
     *
     * @param id user identifier
     * @return user profile
     */
    UserVO getById(Long id);

    /**
     * Creates a user inside the current tenant.
     *
     * @param request creation payload
     * @return created user
     */
    UserVO create(UserCreateRequest request);

    /**
     * Updates a user of the current tenant.
     *
     * @param id      user identifier
     * @param request update payload
     * @return updated user
     */
    UserVO update(Long id, UserUpdateRequest request);

    /**
     * Logically deletes a user of the current tenant.
     *
     * @param id user identifier
     */
    void delete(Long id);
}
