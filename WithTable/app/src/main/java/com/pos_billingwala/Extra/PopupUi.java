package com.pos_billingwala.Extra;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

/**
 * Centralized popup menus and dropdowns — consistent sizing, positioning, and elevation
 * across toolbar filters, action menus, and form dropdowns.
 */
public final class PopupUi {

    private PopupUi() {
    }

    /** Compact action/filter menu (wrap content). */
    public static PopupWindow create(@NonNull Context context, @NonNull View content) {
        return create(context, content, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Dropdown list matched to anchor width (searchable spinners, etc.). */
    public static PopupWindow create(@NonNull Context context, @NonNull View content, int widthPx) {
        PopupWindow popup = new PopupWindow(content, widthPx, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        applyDefaults(context, popup);
        return popup;
    }

    private static void applyDefaults(@NonNull Context context, @NonNull PopupWindow popup) {
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        float density = context.getResources().getDisplayMetrics().density;
        popup.setElevation(12f * density);
    }

    /**
     * Toolbar filter / overflow menu — right-aligned to the anchor, clamped on screen.
     */
    public static void showAsToolbarMenu(@NonNull PopupWindow popup, @NonNull View anchor) {
        showAnchored(popup, anchor, true);
    }

    /**
     * Form-field dropdown — left-aligned below the anchor (searchable dropdowns, inline cells).
     */
    public static void showBelowAnchor(@NonNull PopupWindow popup, @NonNull View anchor) {
        showAnchored(popup, anchor, false);
    }

    private static void showAnchored(@NonNull PopupWindow popup, @NonNull View anchor, boolean alignEnd) {
        View content = popup.getContentView();
        if (content == null) {
            return;
        }

        content.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupW = content.getMeasuredWidth();
        int popupH = content.getMeasuredHeight();

        float density = anchor.getResources().getDisplayMetrics().density;
        int gap = (int) (4 * density);
        int margin = (int) (8 * density);

        int xOff = alignEnd ? anchor.getWidth() - popupW : 0;

        int[] anchorLoc = new int[2];
        anchor.getLocationOnScreen(anchorLoc);
        int screenW = anchor.getResources().getDisplayMetrics().widthPixels;
        int screenH = anchor.getResources().getDisplayMetrics().heightPixels;

        int absLeft = anchorLoc[0] + xOff;
        if (absLeft < margin) {
            xOff += margin - absLeft;
        }
        int absRight = anchorLoc[0] + xOff + popupW;
        if (absRight > screenW - margin) {
            xOff -= absRight - (screenW - margin);
        }

        int spaceBelow = screenH - (anchorLoc[1] + anchor.getHeight());
        int yOff;
        if (spaceBelow >= popupH + gap) {
            yOff = gap;
        } else if (anchorLoc[1] >= popupH + gap) {
            yOff = -(popupH + gap);
        } else {
            yOff = gap;
        }

        popup.showAsDropDown(anchor, xOff, yOff);
    }
}
