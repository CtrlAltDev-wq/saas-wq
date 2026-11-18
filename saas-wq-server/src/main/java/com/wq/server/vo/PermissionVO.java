package com.wq.server.vo;

import com.wq.server.entity.SysPermission;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Permission catalogue entry.
 *
 * @param code        permission code used as a Spring Security authority
 * @param name        display name
 * @param resource    protected resource
 * @param action      allowed action
 * @param description human readable purpose
 */
@Schema(description = "Permission catalogue entry")
public record PermissionVO(String code, String name, String resource, String action, String description) {

    /**
     * Maps a permission entity to the API representation.
     *
     * @param permission permission entity
     * @return permission view object
     */
    public static PermissionVO from(SysPermission permission) {
        return new PermissionVO(permission.getCode(), permission.getName(), permission.getResource(),
                permission.getAction(), permission.getDescription());
    }
}
