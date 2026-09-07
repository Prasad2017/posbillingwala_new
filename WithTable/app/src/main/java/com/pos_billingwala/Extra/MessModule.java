package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.MessTokenScanActivity;
import com.pos_billingwala.Activity.MessWalkInTokenActivity;
import com.pos_billingwala.Fragment.InvoiceMess;
import com.pos_billingwala.R;

/**
 * Mess / QR token module facade over the live mess domain.
 * Parallel to CreatePos cart billing — members, payments, walk-in tokens, scan/verify, meal queue.
 * Phase 07: extract / gate only; do not change QR payload format.
 */
public final class MessModule {

    /** Frozen QR payload prefix — see {@link MessTokenQrHelper}. */
    public static final String QR_PAYLOAD_PREFIX = "POSBILL|v1|";

    public static final String MEMBER_TYPE_WALK_IN = MessTokenQrHelper.MEMBER_TYPE_WALK_IN;
    public static final String MEMBER_TYPE_MEMBER = MessTokenQrHelper.MEMBER_TYPE_MEMBER;
    public static final String TOKEN_STATE_ACTIVE = MessTokenQrHelper.TOKEN_STATE_ACTIVE;
    public static final String TOKEN_STATE_VERIFIED = MessTokenQrHelper.TOKEN_STATE_VERIFIED;

    private MessModule() {
    }

    public static boolean isEnabled(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.MESS);
    }

    /**
     * Soft gate for Activities: finish if mess licence/template is off.
     * @return true if the caller should continue setup
     */
    public static boolean ensureEnabled(Activity activity) {
        if (activity == null) {
            return false;
        }
        if (isEnabled(activity)) {
            return true;
        }
        Toast.makeText(activity, R.string.toast_you_have_not_selected_mess_please_contact, Toast.LENGTH_SHORT).show();
        activity.finish();
        return false;
    }

    public static boolean openHub(Activity activity) {
        if (activity == null || !(activity instanceof MainActivity)) {
            return false;
        }
        if (!isEnabled(activity)) {
            Toast.makeText(activity, R.string.toast_you_have_not_selected_mess_please_contact, Toast.LENGTH_SHORT).show();
            return false;
        }
        ((MainActivity) activity).loadFragment(new InvoiceMess(), true);
        return true;
    }

    public static Fragment newHubFragment() {
        return new InvoiceMess();
    }

    public static Intent walkInTokenIntent(Context context) {
        return new Intent(context, MessWalkInTokenActivity.class);
    }

    public static Intent scanTokenIntent(Context context) {
        return new Intent(context, MessTokenScanActivity.class);
    }

    // --- QR helpers (delegate; format frozen) ---

    public static String generateTokenCode() {
        return MessTokenQrHelper.generateTokenCode();
    }

    public static String buildPayload(String tokenCode, String userId, String memberType) {
        return MessTokenQrHelper.buildPayload(tokenCode, userId, memberType);
    }

    @Nullable
    public static String[] parsePayload(String raw) {
        return MessTokenQrHelper.parsePayload(raw);
    }

    public static Bitmap generateTokenQrBitmap(String content, int sizePx) {
        return MessTokenQrHelper.generateQrBitmap(content, sizePx);
    }

    public static Bitmap generateBrandedMessQr(@Nullable Context context, String content, int sizePx) {
        return MessTokenQrHelper.generateBrandedMessQr(context, content, sizePx);
    }

    public static String moduleSummary(Context context) {
        return "Mess module: " + (isEnabled(context) ? "on" : "off")
                + "\nTables: member, member_payment, mess_invoice, mess_token, mess_meal_token_queue"
                + "\nQR: " + QR_PAYLOAD_PREFIX + "token|userId|memberType"
                + "\nHub: InvoiceMess → members / QR / today tokens / meal sessions";
    }
}
