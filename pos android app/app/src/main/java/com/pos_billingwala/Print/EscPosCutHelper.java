package com.pos_billingwala.Print;

/**
 * ESC/POS paper-cut helpers.
 *
 * There is no universal ESC/POS command that reliably reports whether a
 * hardware cutter exists. Callers must gate cuts with
 * {@link PrinterCapabilityManager} (SUPPORTED / NOT_SUPPORTED / UNKNOWN).
 *
 * Preferred: ESC d n + GS V 65/66 (Function B) + GS V 0/1 (Function A).
 * Cheap BT clones (GLPrinter / POS-80-style) often ignore GS V 0/1 alone.
 * Legacy Epson-style: ESC i (0x1B 0x69) — kept for older firmware only.
 */
public final class EscPosCutHelper {

    public enum CutType {
        FULL,
        PARTIAL,
        DEFAULT
    }

    private static final int DEFAULT_FEED_TO_CUTTER = 5;

    private EscPosCutHelper() {
    }

    /**
     * Production cut bytes: feed to cutter, Function B, then Function A.
     * DEFAULT / FULL → full cut; PARTIAL → partial.
     */
    public static byte[] cutCommand(CutType type) {
        return cutCommand(type, DEFAULT_FEED_TO_CUTTER);
    }

    public static byte[] cutCommand(CutType type, int feedToCutter) {
        boolean full = type != CutType.PARTIAL;
        int n = Math.max(0, Math.min(20, feedToCutter));
        byte[] out = new byte[(n > 0 ? 3 : 0) + 4 + 3];
        int i = 0;
        if (n > 0) {
            out[i++] = 0x1B; /* ESC d n */
            out[i++] = 0x64;
            out[i++] = (byte) n;
        }
        out[i++] = 0x1D; /* GS V m n — Function B */
        out[i++] = 0x56;
        out[i++] = (byte) (full ? 0x41 : 0x42);
        out[i++] = 0x00;
        out[i++] = 0x1D; /* GS V m — Function A fallback */
        out[i++] = 0x56;
        out[i++] = (byte) (full ? 0x00 : 0x01);
        if (i == out.length) {
            return out;
        }
        byte[] trimmed = new byte[i];
        System.arraycopy(out, 0, trimmed, 0, i);
        return trimmed;
    }

    /** Legacy ESC i — some older thermal firmwares only. Prefer {@link #cutCommand}. */
    public static byte[] legacyEpsonCut() {
        return new byte[]{0x1B, 0x69};
    }

    /**
     * Sends a cut command through the given writer. Never throws to callers —
     * printers without a cutter typically ignore the bytes.
     *
     * @return true if write was accepted by the transport; false on I/O failure
     */
    public static boolean tryCut(CutWriter writer, CutType type) {
        if (writer == null) {
            return false;
        }
        try {
            return writer.write(cutCommand(type));
        } catch (Exception ignored) {
            return false;
        }
    }

    public interface CutWriter {
        boolean write(byte[] data);
    }
}
