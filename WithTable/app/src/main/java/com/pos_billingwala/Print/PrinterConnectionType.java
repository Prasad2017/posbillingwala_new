package com.pos_billingwala.Print;

public enum PrinterConnectionType {
    BLUETOOTH,
    USB,
    WIFI;

    public static PrinterConnectionType fromStored(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BLUETOOTH;
        }
        String v = value.trim().toUpperCase();
        if ("USB".equals(v)) {
            return USB;
        }
        if ("WIFI".equals(v) || "WI-FI".equals(v) || "LAN".equals(v) || "WIFI/LAN".equals(v) || "NETWORK".equals(v)) {
            return WIFI;
        }
        return BLUETOOTH;
    }

    public String toStored() {
        switch (this) {
            case USB:
                return "USB";
            case WIFI:
                return "WIFI";
            case BLUETOOTH:
            default:
                return "BLUETOOTH";
        }
    }
}
