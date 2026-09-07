package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.dynamic.ConfigApplyGuard;
import com.pos_billingwala.Extra.dynamic.PrinterConnectionType;
import com.pos_billingwala.Extra.dynamic.PrinterRole;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.R;

import java.util.List;

/**
 * Universal printer facade over live Bluetooth / Woosim bill + KOT stack.
 * Phase 16 + Dynamic Phase 10: roles + routing; print failure must never wipe a saved bill.
 * Connection today: Bluetooth (USB/WiFi reserved for future).
 */
public final class UniversalPrinterEngine {

    private UniversalPrinterEngine() {
    }

    public static boolean isRoleConfigured(POSBillingWalaDatabase db, PrinterRole role) {
        String addr = addressForRole(db, role);
        return addr != null && !addr.isEmpty();
    }

    public static TicketRoute routeForTicket(PrinterRole role) {
        if (role == PrinterRole.BOT || role == PrinterRole.BAR) {
            return TicketRoute.BAR_BOT;
        }
        return TicketRoute.KITCHEN_KOT;
    }

    public static PrinterRole roleForTicketCode(@Nullable String ticketCode) {
        if (ticketCode != null && "BOT".equalsIgnoreCase(ticketCode.trim())) {
            return PrinterRole.BOT;
        }
        return PrinterRole.KOT;
    }

    public static void markPrintSession(Context context, boolean active) {
        ConfigApplyGuard.setPrintActive(context, active);
    }

    /**
     * Bluetooth MAC for a printer role. Falls back: BOT→KOT→bill, KOT→bill, TOKEN/REPORT→bill.
     */
    public static String addressForRole(POSBillingWalaDatabase db, PrinterRole role) {
        return addressForRole(firstSettings(db), role);
    }

    public static String addressForRole(@Nullable PrinterSettingResponse s, PrinterRole role) {
        if (s == null || role == null) {
            return "";
        }
        String bill = s.getBluetoothAddress() != null ? s.getBluetoothAddress().trim() : "";
        String kot = s.getBluetoothKOTAddress() != null ? s.getBluetoothKOTAddress().trim() : "";
        String bot = s.getBluetoothBotAddress() != null ? s.getBluetoothBotAddress().trim() : "";
        switch (role) {
            case BOT:
            case BAR:
                if (!bot.isEmpty()) {
                    return bot;
                }
                if (!kot.isEmpty()) {
                    return kot;
                }
                return bill;
            case KOT:
            case KITCHEN:
                if (!kot.isEmpty()) {
                    return kot;
                }
                return bill;
            case INVOICE:
            case REPORT:
            case TOKEN:
            default:
                return bill;
        }
    }

    /** Returns address or shows a friendly toast and returns empty when missing. */
    public static String requireAddress(Activity activity, POSBillingWalaDatabase db, PrinterRole role) {
        String addr = addressForRole(db, role);
        if (addr == null || addr.isEmpty()) {
            if (activity != null) {
                Toast.makeText(activity, friendlyMissingMessage(activity, role), Toast.LENGTH_LONG).show();
            }
            return "";
        }
        return addr;
    }

    public static String requireAddress(Activity activity, @Nullable PrinterSettingResponse settings,
                                        PrinterRole role) {
        String addr = addressForRole(settings, role);
        if (addr == null || addr.isEmpty()) {
            if (activity != null) {
                Toast.makeText(activity, friendlyMissingMessage(activity, role), Toast.LENGTH_LONG).show();
            }
            return "";
        }
        return addr;
    }

    private static String friendlyMissingMessage(Context context, PrinterRole role) {
        if (context == null) {
            return "Printer not configured";
        }
        if (role == PrinterRole.BOT || role == PrinterRole.BAR) {
            return context.getString(R.string.toast_bot_printer_failed);
        }
        if (role == PrinterRole.KOT || role == PrinterRole.KITCHEN) {
            return context.getString(R.string.toast_please_select_printer_from_setting);
        }
        return context.getString(R.string.toast_please_select_printer_from_setting);
    }

    public static PrinterSettingResponse firstSettings(POSBillingWalaDatabase db) {
        if (db == null) {
            return null;
        }
        List<PrinterSettingResponse> list = db.getPrinterSettingDetails();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public static boolean hasBillPrinter(POSBillingWalaDatabase db) {
        return !addressForRole(db, PrinterRole.INVOICE).isEmpty();
    }

    public static boolean hasKotPrinter(POSBillingWalaDatabase db) {
        PrinterSettingResponse s = firstSettings(db);
        if (s == null) {
            return false;
        }
        String kot = s.getBluetoothKOTAddress();
        return kot != null && !kot.trim().isEmpty();
    }

    public static boolean isKotEnabled(POSBillingWalaDatabase db) {
        return RestaurantFoodModule.canKot(db);
    }

    public static boolean isBotEnabled(Context context, POSBillingWalaDatabase db) {
        return BarRestaurantModule.canBot(context, db);
    }

    public static boolean hasBotPrinter(POSBillingWalaDatabase db) {
        return BarRestaurantModule.hasDedicatedBotPrinter(db)
                || hasKotPrinter(db);
    }

    public static PrinterConnectionType connectionType(@Nullable PrinterSettingResponse s) {
        if (s == null) {
            return PrinterConnectionType.BLUETOOTH;
        }
        return PrinterConnectionType.fromWire(s.getPrinterConnectionType());
    }

    public static boolean usesBluetoothStack(@Nullable PrinterSettingResponse s) {
        return connectionType(s).usesBluetoothStack();
    }

    public static String moduleSummary(Context context) {
        POSBillingWalaDatabase db = context != null ? new POSBillingWalaDatabase(context) : null;
        PrinterSettingResponse s = firstSettings(db);
        return "Bill BT: " + (hasBillPrinter(db) ? "configured" : "missing")
                + " · KOT BT: " + (hasKotPrinter(db) ? "configured" : "missing/same")
                + " · BOT BT: " + (BarRestaurantModule.hasDedicatedBotPrinter(db) ? "dedicated" : "same as KOT")
                + "\nConnection type: " + connectionType(s).wire
                + "\nKOT: " + (isKotEnabled(db) ? "on" : "off")
                + " · BOT: " + (isBotEnabled(context, db) ? "on" : "off")
                + "\nRule: print failure never deletes saved invoice"
                + "\nRoles: INVOICE/KOT/KITCHEN/BOT/BAR/TOKEN/REPORT (BT today)"
                + "\nRouting: addressForRole live in BluetoothPrint / MessToken / Home"
                + "\nCustom-order photo: on bill when | Photo:… present"
                + "\nSettings: CompanyPrinterSetting (KOT + BOT + connection type)";
    }
}
