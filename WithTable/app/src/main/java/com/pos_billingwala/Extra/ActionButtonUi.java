package com.pos_billingwala.Extra;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Keeps action button icons on the left when label text is updated in code.
 */
public final class ActionButtonUi {

    private ActionButtonUi() {
    }

    public static void bind(View root, @DrawableRes int iconRes, @StringRes int labelRes) {
        if (root == null) {
            return;
        }
        ImageView icon = root.findViewById(com.pos_billingwala.R.id.actionButtonIcon);
        TextView label = root.findViewById(com.pos_billingwala.R.id.actionButtonLabel);
        if (icon != null) {
            icon.setImageResource(iconRes);
        }
        if (label != null) {
            label.setText(labelRes);
        }
    }

    public static void setLabel(View root, CharSequence text) {
        if (root == null) {
            return;
        }
        TextView label = root.findViewById(com.pos_billingwala.R.id.actionButtonLabel);
        if (label != null) {
            label.setText(text);
        }
    }

    public static CharSequence getLabel(View root) {
        if (root == null) {
            return "";
        }
        TextView label = root.findViewById(com.pos_billingwala.R.id.actionButtonLabel);
        return label != null && label.getText() != null ? label.getText() : "";
    }
}
