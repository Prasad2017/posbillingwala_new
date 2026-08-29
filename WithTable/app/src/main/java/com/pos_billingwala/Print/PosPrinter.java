package com.pos_billingwala.Print;

/**
 * Unified transport for thermal printers. Receipt bytes are produced by existing
 * Billingwala raster code; adapters only send those bytes.
 */
public interface PosPrinter {

    boolean connect();

    boolean print(byte[] data);

    boolean cut();

    boolean openCashDrawer();

    void disconnect();

    boolean isConnected();

    boolean supportsCutter();

    boolean supportsCashDrawer();

    PrinterConnectionType getConnectionType();

    /** Stable identity for the print queue (MAC / USB id / IP:port). */
    String getPrinterIdentity();

    String getDisplayName();
}
