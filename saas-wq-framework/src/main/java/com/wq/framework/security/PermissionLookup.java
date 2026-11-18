package com.wq.framework.security;

import java.util.Collection;

/**
 * Resolves the permission codes granted to a role.
 *
 * <p>The contract lives in the framework module so that the security filter can inject
 * permissions without depending on the persistence layer. Implementations are expected to
 * cache the result and to evict it when role permissions change.</p>
 */
public interface PermissionLookup {

    /**
     * Returns the permission codes of the given role.
     *
     * @param roleCode role code without the {@code ROLE_} prefix
     * @return permission codes, empty when the role is unknown
     */
    Collection<String> permissionsFor(String roleCode);
}
