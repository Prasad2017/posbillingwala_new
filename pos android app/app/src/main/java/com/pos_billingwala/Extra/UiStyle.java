package com.pos_billingwala.Extra;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;

/**
 * Shared typography and card styling — orientation and layout may change,
 * but text sizes and visual tokens stay the same everywhere.
 */
public final class UiStyle {

    private UiStyle() {
    }

    public static void applyTextSize(@NonNull TextView view, @DimenRes int dimenRes) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                view.getResources().getDimension(dimenRes));
    }

    public static int dimenPx(@NonNull Context context, @DimenRes int dimenRes) {
        return Math.round(context.getResources().getDimension(dimenRes));
    }
}
