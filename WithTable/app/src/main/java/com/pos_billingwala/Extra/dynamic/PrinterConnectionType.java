package com.pos_billingwala.Extra.dynamic;

/**
 * Printer connection kinds (Phase 10 close-out).
 * Live print path remains Bluetooth; Wi-Fi/Network store an identifier in the address field.
 */
public enum PrinterConnectionType {
    BLUETOOTH("bluetooth"),
    WIFI("wifi"),
    USB("usb"),
    NETWORK("network");

    public final String wire;

    PrinterConnectionType(String wire) {
        this.wire = wire;
    }

    public static PrinterConnectionType fromWire(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BLUETOOTH;
        }
        String v = value.trim().toLowerCase();
        for (PrinterConnectionType t : values()) {
            if (t.wire.equals(v)) {
                return t;
            }
        }
        return BLUETOOTH;
    }

    public boolean usesBluetoothStack() {
        return this == BLUETOOTH;
    }
}
