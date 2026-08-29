package com.pos_billingwala.Print;

import com.pos_billingwala.Model.PrinterSettingResponse;

/**
 * Runtime printer profile built from existing {@link PrinterSettingResponse}.
 * Does not replace the settings model — adapts it for the transport layer.
 */
public final class PrinterProfile {

    public final String channel; // bill | kot
    public final String paperSize; // 2-Inch | 3-Inch
    public final PrinterConnectionType connectionType;
    public final String bluetoothMac;
    public final String printerIp;
    public final int printerPort;
    public final String usbDeviceKey;
    public final boolean supportsCutter;
    public final boolean supportsCashDrawer;
    public final boolean autoCut;
    public final boolean autoOpenCashDrawer;
    public final DrawerOpenMode drawerOpenMode;
    public final int drawerPin;
    public final int drawerPulseOn;
    public final int drawerPulseOff;
    public final String cutCommandPreference;
    public final String printerModel;

    private PrinterProfile(Builder b) {
        this.channel = b.channel;
        this.paperSize = b.paperSize;
        this.connectionType = b.connectionType;
        this.bluetoothMac = b.bluetoothMac;
        this.printerIp = b.printerIp;
        this.printerPort = b.printerPort;
        this.usbDeviceKey = b.usbDeviceKey;
        this.supportsCutter = b.supportsCutter;
        this.supportsCashDrawer = b.supportsCashDrawer;
        this.autoCut = b.autoCut;
        this.autoOpenCashDrawer = b.autoOpenCashDrawer;
        this.drawerOpenMode = b.drawerOpenMode;
        this.drawerPin = b.drawerPin;
        this.drawerPulseOn = b.drawerPulseOn;
        this.drawerPulseOff = b.drawerPulseOff;
        this.cutCommandPreference = b.cutCommandPreference;
        this.printerModel = b.printerModel;
    }

    public static PrinterProfile forBill(PrinterSettingResponse s) {
        return fromSettings(s, true);
    }

    public static PrinterProfile forKot(PrinterSettingResponse s) {
        return fromSettings(s, false);
    }

    private static PrinterProfile fromSettings(PrinterSettingResponse s, boolean bill) {
        Builder b = new Builder().channel(bill ? "bill" : "kot");
        if (s == null) {
            return b.connectionType(PrinterConnectionType.BLUETOOTH).build();
        }
        b.paperSize(bill
                ? nullTo(s.getPrinterName(), "2-Inch")
                : nullTo(s.getKOTPrinterName(), nullTo(s.getPrinterName(), "2-Inch")));
        b.connectionType(PrinterConnectionType.fromStored(
                bill ? s.getBillConnectionType() : s.getKotConnectionType()));
        b.bluetoothMac(bill
                ? nullTo(s.getBluetoothAddress(), "")
                : nullTo(s.getBluetoothKOTAddress(), ""));
        b.printerIp(bill
                ? nullTo(s.getBillPrinterIp(), "")
                : nullTo(s.getKotPrinterIp(), nullTo(s.getBillPrinterIp(), "")));
        b.printerPort(parsePort(bill ? s.getBillPrinterPort() : s.getKotPrinterPort()));
        b.usbDeviceKey(bill
                ? nullTo(s.getBillUsbDeviceKey(), "")
                : nullTo(s.getKotUsbDeviceKey(), nullTo(s.getBillUsbDeviceKey(), "")));
        b.supportsCutter(isOn(s.getSupportsCutter(), true));
        b.supportsCashDrawer(isOn(s.getSupportsCashDrawer(), false));
        b.autoCut(isOn(s.getAutoCut(), true));
        b.autoOpenCashDrawer(isOn(s.getAutoOpenCashDrawer(), true));
        b.drawerOpenMode(DrawerOpenMode.fromStored(s.getDrawerOpenMode()));
        b.drawerPin(parseInt(s.getDrawerPin(), 0));
        b.drawerPulseOn(parseInt(s.getDrawerPulseOn(), 0x19));
        b.drawerPulseOff(parseInt(s.getDrawerPulseOff(), 0x78));
        b.cutCommandPreference(nullTo(s.getCutCommand(), "FULL"));
        b.printerModel(nullTo(s.getPrinterModel(), ""));
        return b.build();
    }

    public String identityKey() {
        switch (connectionType) {
            case USB:
                return "usb:" + (usbDeviceKey != null ? usbDeviceKey : "default");
            case WIFI:
                return "wifi:" + (printerIp != null ? printerIp : "") + ":" + printerPort;
            case BLUETOOTH:
            default:
                return "bt:" + (bluetoothMac != null ? bluetoothMac.trim().toUpperCase() : "");
        }
    }

    public byte[] cutBytes() {
        return EscPosCommands.resolveCutCommand(cutCommandPreference);
    }

    public byte[] drawerBytes() {
        return EscPosCommands.cashDrawerKick(drawerPin, drawerPulseOn, drawerPulseOff);
    }

    public boolean shouldCut() {
        return autoCut && supportsCutter;
    }

    public boolean shouldOpenDrawer(String paymentMode) {
        return autoOpenCashDrawer
                && supportsCashDrawer
                && drawerOpenMode != null
                && drawerOpenMode.shouldOpen(paymentMode);
    }

    private static String nullTo(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private static boolean isOn(String value, boolean defaultOn) {
        if (value == null || value.trim().isEmpty()) {
            return defaultOn;
        }
        String v = value.trim().toLowerCase();
        if ("on".equals(v) || "1".equals(v) || "true".equals(v) || "yes".equals(v)) {
            return true;
        }
        if ("off".equals(v) || "0".equals(v) || "false".equals(v) || "no".equals(v)) {
            return false;
        }
        return defaultOn;
    }

    private static int parsePort(String value) {
        int p = parseInt(value, 9100);
        return p > 0 && p <= 65535 ? p : 9100;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static final class Builder {
        private String channel = "bill";
        private String paperSize = "2-Inch";
        private PrinterConnectionType connectionType = PrinterConnectionType.BLUETOOTH;
        private String bluetoothMac = "";
        private String printerIp = "";
        private int printerPort = 9100;
        private String usbDeviceKey = "";
        private boolean supportsCutter = true;
        private boolean supportsCashDrawer = false;
        private boolean autoCut = true;
        private boolean autoOpenCashDrawer = true;
        private DrawerOpenMode drawerOpenMode = DrawerOpenMode.CASH_ONLY;
        private int drawerPin = 0;
        private int drawerPulseOn = 0x19;
        private int drawerPulseOff = 0x78;
        private String cutCommandPreference = "FULL";
        private String printerModel = "";

        public Builder channel(String v) {
            this.channel = v;
            return this;
        }

        public Builder paperSize(String v) {
            this.paperSize = v;
            return this;
        }

        public Builder connectionType(PrinterConnectionType v) {
            this.connectionType = v != null ? v : PrinterConnectionType.BLUETOOTH;
            return this;
        }

        public Builder bluetoothMac(String v) {
            this.bluetoothMac = v != null ? v : "";
            return this;
        }

        public Builder printerIp(String v) {
            this.printerIp = v != null ? v : "";
            return this;
        }

        public Builder printerPort(int v) {
            this.printerPort = v;
            return this;
        }

        public Builder usbDeviceKey(String v) {
            this.usbDeviceKey = v != null ? v : "";
            return this;
        }

        public Builder supportsCutter(boolean v) {
            this.supportsCutter = v;
            return this;
        }

        public Builder supportsCashDrawer(boolean v) {
            this.supportsCashDrawer = v;
            return this;
        }

        public Builder autoCut(boolean v) {
            this.autoCut = v;
            return this;
        }

        public Builder autoOpenCashDrawer(boolean v) {
            this.autoOpenCashDrawer = v;
            return this;
        }

        public Builder drawerOpenMode(DrawerOpenMode v) {
            this.drawerOpenMode = v != null ? v : DrawerOpenMode.CASH_ONLY;
            return this;
        }

        public Builder drawerPin(int v) {
            this.drawerPin = v;
            return this;
        }

        public Builder drawerPulseOn(int v) {
            this.drawerPulseOn = v;
            return this;
        }

        public Builder drawerPulseOff(int v) {
            this.drawerPulseOff = v;
            return this;
        }

        public Builder cutCommandPreference(String v) {
            this.cutCommandPreference = v != null ? v : "FULL";
            return this;
        }

        public Builder printerModel(String v) {
            this.printerModel = v != null ? v : "";
            return this;
        }

        public PrinterProfile build() {
            return new PrinterProfile(this);
        }
    }
}
