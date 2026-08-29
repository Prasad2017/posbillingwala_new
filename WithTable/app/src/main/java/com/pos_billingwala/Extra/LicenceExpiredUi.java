package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.pos_billingwala.R;

import java.util.Locale;

/**
 * Blocking UI when the shop licence is expired — user cannot enter the app.
 * All user-facing copy lives in {@code res/values/strings.xml} for translation.
 */
public final class LicenceExpiredUi {

    public static final String EXTRA_SHOW_LICENCE_EXPIRED = "showLicenceExpired";

    private LicenceExpiredUi() {
    }

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        BottomSheetUi.showAction(
                activity,
                activity.getString(R.string.licence_expired_title),
                activity.getString(
                        R.string.licence_expired_message,
                        activity.getString(R.string.support_phone_display)),
                activity.getString(R.string.licence_expired_call),
                activity.getString(R.string.licence_expired_ok),
                R.mipmap.ic_launcher,
                false,
                () -> BottomSheetUi.dialSupport(activity),
                null);
    }

    /**
     * Detects expiry / disable wording from the server so we can show our friendly local dialog.
     */
    public static boolean isExpiredMessage(@Nullable String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String m = message.toLowerCase(Locale.ENGLISH);
        return m.contains("expir");
    }

    public static boolean isDisabledMessage(@Nullable String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String m = message.toLowerCase(Locale.ENGLISH);
        return m.contains("disable") || m.contains("disabled");
    }

    /**
     * Maps server / technical messages to a single user-friendly string from resources.
     */
    public static String friendlyLoginMessage(Context context, @Nullable String serverMessage) {
        if (isExpiredMessage(serverMessage)) {
            return context.getString(R.string.licence_msg_expired);
        }
        if (isDisabledMessage(serverMessage)) {
            return context.getString(
                    R.string.licence_msg_disabled,
                    context.getString(R.string.support_phone_display));
        }
        return context.getString(
                R.string.licence_msg_login_blocked,
                context.getString(R.string.support_phone_display));
    }

    public static void showForServerMessage(Activity activity, @Nullable String serverMessage) {
        if (isExpiredMessage(serverMessage)) {
            show(activity);
            return;
        }
        if (activity == null || activity.isFinishing()) {
            return;
        }
        showInfoDialog(activity, friendlyLoginMessage(activity, serverMessage));
    }

    public static void showInfoDialog(Activity activity, @StringRes int messageRes) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        showInfoDialog(activity, activity.getString(messageRes));
    }

    public static void showInfoDialog(Activity activity, String message) {
        if (activity == null || activity.isFinishing() || message == null) {
            return;
        }
        BottomSheetUi.showAction(
                activity,
                activity.getString(R.string.app_name),
                message,
                activity.getString(R.string.licence_expired_call),
                activity.getString(R.string.licence_expired_ok),
                R.mipmap.ic_launcher,
                false,
                () -> BottomSheetUi.dialSupport(activity),
                null);
    }
}
