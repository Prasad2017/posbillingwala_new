package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Toolbar overflow menus are shown as bottom sheets.
 * Anchored form dropdowns still use {@link PopupWindow}.
 */
public final class PopupUi {

    private PopupUi() {
    }

    /** Compact action/filter menu — prepared as a bottom sheet (not shown yet). */
    public static BottomSheetDialog create(@NonNull Activity activity, @NonNull View content) {
        return BottomSheetUi.prepare(activity, content, true);
    }

    /** Anchored popups only (searchable dropdowns). Prefer {@link #create(Activity, View)} for menus. */
    @Deprecated
    public static PopupWindow create(@NonNull Context context, @NonNull View content) {
        return createPopup(context, content, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Dropdown list matched to anchor width (searchable spinners, etc.). */
    public static PopupWindow create(@NonNull Context context, @NonNull View content, int widthPx) {
        return createPopup(context, content, widthPx);
    }

    private static PopupWindow createPopup(@NonNull Context context, @NonNull View content, int widthPx) {
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

    /** Toolbar overflow menu — shown as a bottom sheet (anchor kept for call-site compatibility). */
    public static void showAsToolbarMenu(@NonNull BottomSheetDialog sheet, @NonNull View anchor) {
        BottomSheetUi.present(sheet);
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
