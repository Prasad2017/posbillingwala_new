package com.pos_billingwala.Extra;

/**
 * Printable geometry for ESC/POS thermal rolls (203 dpi ≈ 8 dots/mm).
 * Physical roll width ≠ printable width. Values match Flutter PrinterPaperProfile.
 */
public final class PrinterPaperProfile {

    public final String label;
    public final int physicalWidthMm;
    public final int printableWidthMm;
    public final int imageWidthPx;

    private PrinterPaperProfile(String label, int physicalWidthMm, int printableWidthMm, int imageWidthPx) {
        this.label = label;
        this.physicalWidthMm = physicalWidthMm;
        this.printableWidthMm = printableWidthMm;
        this.imageWidthPx = imageWidthPx;
    }

    public static final PrinterPaperProfile MM58 =
            new PrinterPaperProfile("58mm", 58, 48, 384);
    public static final PrinterPaperProfile MM60 =
            new PrinterPaperProfile("60mm", 60, 50, 400);
    public static final PrinterPaperProfile MM78 =
            new PrinterPaperProfile("78mm", 78, 70, 560);
    public static final PrinterPaperProfile MM80 =
            new PrinterPaperProfile("80mm", 80, 72, 576);

    public static PrinterPaperProfile of(String raw) {
        String n = PaperSizeHelper.normalize(raw);
        if ("60mm".equalsIgnoreCase(n)) {
            return MM60;
        }
        if ("78mm".equalsIgnoreCase(n)) {
            return MM78;
        }
        if ("3-Inch".equalsIgnoreCase(n)) {
            return MM80;
        }
        return MM58;
    }

    /** Effective print width in mm used when resizing layout bitmaps. */
    public int effectivePrintWidthMm() {
        return printableWidthMm;
    }
}
