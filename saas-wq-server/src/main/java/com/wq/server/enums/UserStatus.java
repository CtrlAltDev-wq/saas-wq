package com.wq.server.enums;

/**
 * Lifecycle state of a user account.
 */
public enum UserStatus {

    /**
     * Account can authenticate.
     */
    ACTIVE,

    /**
     * Account is locked and cannot authenticate.
     */
    DISABLED
}
