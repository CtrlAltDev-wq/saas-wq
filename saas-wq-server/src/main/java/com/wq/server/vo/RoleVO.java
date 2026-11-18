package com.wq.server.vo;

import com.wq.server.entity.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Role representation including the permission codes currently granted to it.
 *
 * @param code        stable role code
 * @param name        display name
 * @param description human readable purpose
 * @param builtIn     whether the role is built in
 * @param permissions granted permission codes
 */
@Schema(description = "Role with its granted permissions")
public record RoleVO(String code, String name, String description, boolean builtIn, List<String> permissions) {

    /**
     * Maps a role entity and its permission codes to the API representation.
     *
     * @param role        role entity
     * @param permissions granted permission codes
     * @return role view object
     */
    public static RoleVO from(SysRole role, List<String> permissions) {
        return new RoleVO(role.getCode(), role.getName(), role.getDescription(),
                Boolean.TRUE.equals(role.getBuiltIn()), permissions);
    }
}
