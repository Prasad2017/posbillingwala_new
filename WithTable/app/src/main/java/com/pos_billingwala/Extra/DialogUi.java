package com.pos_billingwala.Extra;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

/**
 * Tablet-aware sizing for AlertDialog and other floating dialog windows.
 */
public final class DialogUi {

    private DialogUi() {
    }

    public static void applyTabletWindow(@Nullable Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        Context ctx = dialog.getContext();
        if (!ResponsiveUi.isWideLayout(ctx)) {
            return;
        }
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        int horizontalInset = TabletUi.dpToPx(ctx, TabletUi.horizontalInsetDp(ctx));
        int maxWidth = TabletUi.dpToPx(ctx, TabletUi.bottomSheetMaxWidthDp(ctx));
        int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        lp.width = Math.min(maxWidth, screenWidth - horizontalInset * 2);
        lp.gravity = Gravity.CENTER;
        window.setAttributes(lp);
    }
}
