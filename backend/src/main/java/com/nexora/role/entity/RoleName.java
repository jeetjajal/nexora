package com.nexora.role.entity;

/**
 * The Nexora roles, as fixed values instead of free-text strings.
 * Enforces compile-time role checking and strict database mapping.
 */
public enum RoleName {
    CUSTOMER,
    STORE_OWNER,
    STORE_MANAGER,
    PICKER,
    PACKER,
    DELIVERY_PARTNER,
    ADMIN,
    SUPER_ADMIN
}
