package com.pos_billingwala.Extra;

/**
 * Device-session staff role for POS RBAC.
 * Default {@link #OWNER} matches prior behaviour (full access).
 */
public enum StaffRole {
    OWNER("owner", "Owner"),
    MANAGER("manager", "Manager"),
    CASHIER("cashier", "Cashier"),
    WAITER("waiter", "Waiter");

    private final String id;
    private final String label;

    StaffRole(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static StaffRole fromId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return OWNER;
        }
        String v = raw.trim().toLowerCase();
        for (StaffRole role : values()) {
            if (role.id.equals(v)) {
                return role;
            }
        }
        return OWNER;
    }

    public boolean isElevated() {
        return this == OWNER || this == MANAGER;
    }
}
