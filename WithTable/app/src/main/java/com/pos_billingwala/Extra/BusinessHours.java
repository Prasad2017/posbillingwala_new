package com.pos_billingwala.Extra;

import android.content.Context;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.R;

import java.util.Calendar;
import java.util.Locale;

/** Shop opening / closing hours — device prefs plus company row for server sync. */
public final class BusinessHours {

    public static final String KEY_OPEN = "businessOpeningMinutes";
    public static final String KEY_CLOSE = "businessClosingMinutes";

    private BusinessHours() {
    }

    public static boolean isConfigured(Context context) {
        return minutesOrEmpty(context, KEY_OPEN) >= 0 && minutesOrEmpty(context, KEY_CLOSE) >= 0;
    }

    public static int openingMinutes(Context context) {
        return minutesOrEmpty(context, KEY_OPEN);
    }

    public static int closingMinutes(Context context) {
        return minutesOrEmpty(context, KEY_CLOSE);
    }

    public static void save(Context context, int openMinutes, int closeMinutes) {
        savePrefs(context, openMinutes, closeMinutes);
        persistToCompany(context, true);
    }

    public static void persistToCompany(Context context, boolean markUnsynced) {
        if (context == null || !isConfigured(context)) {
            return;
        }
        try {
            POSBillingWalaDatabase database = new POSBillingWalaDatabase(context);
            database.updateCompanyBusinessHours(
                    String.valueOf(openingMinutes(context)),
                    String.valueOf(closingMinutes(context)),
                    markUnsynced);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Apply hours from getCompanyList so they match other shop details after cloud pull. */
    public static void applyFromServer(Context context, String openingRaw, String closingRaw) {
        if (context == null) {
            return;
        }
        int open = parseMinutes(openingRaw);
        int close = parseMinutes(closingRaw);
        if (open >= 0 && close >= 0) {
            savePrefs(context, open, close);
            persistToCompany(context, false);
            return;
        }
        if (isConfigured(context)) {
            persistToCompany(context, true);
        }
    }

    public static String openingForUpload(Context context, String dbValue) {
        return firstValidMinutes(dbValue, context, KEY_OPEN);
    }

    public static String closingForUpload(Context context, String dbValue) {
        return firstValidMinutes(dbValue, context, KEY_CLOSE);
    }

    public static String formatMinutes(int minutes) {
        if (minutes < 0) {
            return "";
        }
        int hour = minutes / 60;
        int min = minutes % 60;
        int hour12 = hour % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        String amPm = hour < 12 ? "AM" : "PM";
        return String.format(Locale.US, "%d:%02d %s", hour12, min, amPm);
    }

    public static String displayRange(Context context) {
        if (!isConfigured(context)) {
            return context.getString(R.string.business_hours_not_set);
        }
        return formatMinutes(openingMinutes(context)) + " – " + formatMinutes(closingMinutes(context));
    }

    public static String homeStatusLine(Context context) {
        if (!isConfigured(context)) {
            return "";
        }
        String range = displayRange(context);
        return (isOpenNow(context)
                ? context.getString(R.string.shop_open)
                : context.getString(R.string.shop_closed))
                + "  " + range;
    }

    public static boolean isOpenNow(Context context) {
        if (!isConfigured(context)) {
            return true;
        }
        int open = openingMinutes(context);
        int close = closingMinutes(context);
        Calendar now = Calendar.getInstance();
        int current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (open == close) {
            return true;
        }
        if (open < close) {
            return current >= open && current < close;
        }
        return current >= open || current < close;
    }

    private static void savePrefs(Context context, int openMinutes, int closeMinutes) {
        Common.saveUserData(context, KEY_OPEN, String.valueOf(openMinutes));
        Common.saveUserData(context, KEY_CLOSE, String.valueOf(closeMinutes));
    }

    private static String firstValidMinutes(String dbValue, Context context, String prefKey) {
        int fromDb = parseMinutes(dbValue);
        if (fromDb >= 0) {
            return String.valueOf(fromDb);
        }
        int fromPref = minutesOrEmpty(context, prefKey);
        if (fromPref >= 0) {
            return String.valueOf(fromPref);
        }
        return "";
    }

    private static int parseMinutes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return -1;
        }
        try {
            int minutes = Integer.parseInt(raw.trim());
            if (minutes < 0 || minutes > 1439) {
                return -1;
            }
            return minutes;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int minutesOrEmpty(Context context, String key) {
        return parseMinutes(Common.getSavedUserData(context, key));
    }
}
