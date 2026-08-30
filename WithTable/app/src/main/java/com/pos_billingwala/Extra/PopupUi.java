package com.pos_billingwala.Extra;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

/**
 * Tablet-aware popup menus (report filters, share sheets, mess menus, etc.).
 */
public final class PopupUi {

    private PopupUi() {
    }

    public static PopupWindow create(@NonNull Context context, @NonNull View content) {
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (TabletUi.isTablet(context)) {
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int horizontalInset = TabletUi.dpToPx(context, TabletUi.horizontalInsetDp(context));
            int maxWidth = TabletUi.dpToPx(context, TabletUi.bottomSheetMaxWidthDp(context));
            width = Math.min(maxWidth, screenWidth - horizontalInset * 2);
        }
        PopupWindow popup = new PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        return popup;
    }
}
