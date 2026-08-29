package com.pos_billingwala.Print;

/**
 * Configurable ESC/POS cutter and cash-drawer kick commands.
 * Defaults work on most Epson-compatible thermal printers.
 */
public final class EscPosCommands {

    /** Full cut — ESC i */
    public static final byte[] CUT_FULL = new byte[]{0x1B, 0x69};

    /** Partial cut — ESC m */
    public static final byte[] CUT_PARTIAL = new byte[]{0x1B, 0x6D};

    /** GS V 0 — full cut (alternate) */
    public static final byte[] CUT_GS_V_FULL = new byte[]{0x1D, 0x56, 0x00};

    /** GS V 1 — partial cut (alternate) */
    public static final byte[] CUT_GS_V_PARTIAL = new byte[]{0x1D, 0x56, 0x01};

    private EscPosCommands() {
    }

    /**
     * ESC/POS drawer kick: ESC p m t1 t2
     * pin: 0 = pin 2, 1 = pin 5
     * pulseOn/pulseOff: units of 2ms (typical 0x19 / 0x78)
     */
    public static byte[] cashDrawerKick(int drawerPin, int pulseOn, int pulseOff) {
        int pin = drawerPin == 5 || drawerPin == 1 ? 1 : 0;
        int t1 = clamp(pulseOn, 0, 255);
        int t2 = clamp(pulseOff, 0, 255);
        return new byte[]{0x1B, 0x70, (byte) pin, (byte) t1, (byte) t2};
    }

    public static byte[] resolveCutCommand(String preference) {
        if (preference == null || preference.trim().isEmpty()) {
            return CUT_FULL;
        }
        switch (preference.trim().toUpperCase()) {
            case "PARTIAL":
            case "ESC_M":
                return CUT_PARTIAL;
            case "GS_V_FULL":
                return CUT_GS_V_FULL;
            case "GS_V_PARTIAL":
                return CUT_GS_V_PARTIAL;
            case "FULL":
            case "ESC_I":
            default:
                return CUT_FULL;
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
