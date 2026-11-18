package com.wq.server.vo;

import com.wq.server.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * User representation returned to API consumers. The password hash never leaves the
 * persistence layer.
 *
 * @param id        user identifier
 * @param tenantId  owning tenant
 * @param username  login name
 * @param nickname  display name
 * @param email     contact mailbox
 * @param role      role name
 * @param status    ACTIVE or DISABLED
 * @param createdAt creation time
 */
@Schema(description = "User profile")
public record UserVO(Long id, Long tenantId, String username, String nickname, String email, String role,
                     String status, LocalDateTime createdAt) {

    /**
     * Maps a persistence entity to its API representation.
     *
     * @param user entity
     * @return user view object
     */
    public static UserVO from(SysUser user) {
        return new UserVO(user.getId(), user.getTenantId(), user.getUsername(), user.getNickname(), user.getEmail(),
                user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
