package com.nexora.role.entity;

/**
 * The four Nexora roles, as fixed values instead of free-text strings.
 * Using an enum here means invalid role names (typos like "CUSTOMR")
 * are caught at compile time in our Java code, and the database column
 * only ever stores one of these four exact values.
 */
public enum RoleName {
    CUSTOMER,
    STORE_OWNER,
    DELIVERY_PARTNER,
    ADMIN
}
