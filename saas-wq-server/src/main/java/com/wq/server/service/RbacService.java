package com.wq.server.service;

import com.wq.server.vo.PermissionVO;
import com.wq.server.vo.RoleVO;
import java.util.List;

/**
 * Role and permission administration.
 */
public interface RbacService {

    /**
     * Lists every role with its granted permission codes.
     *
     * @return roles ordered by identifier
     */
    List<RoleVO> roles();

    /**
     * Lists the permission catalogue.
     *
     * @return permissions ordered by resource and action
     */
    List<PermissionVO> permissions();

    /**
     * Replaces the permission set of a role.
     *
     * @param roleCode        role code
     * @param permissionCodes permission codes to grant
     * @return updated role
     */
    RoleVO updatePermissions(String roleCode, List<String> permissionCodes);
}
