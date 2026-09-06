package com.posbillingwala.dealer.Extra;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.posbillingwala.dealer.R;

/**
 * Shared empty-list UI: title "No data found" + screen-specific subtitle.
 */
public final class EmptyListUi {

    private EmptyListUi() {
    }

    public static void bind(@Nullable View emptyRoot, boolean hasData, @StringRes int subtitleRes) {
        if (emptyRoot == null) {
            return;
        }
        if (hasData) {
            emptyRoot.setVisibility(View.GONE);
            return;
        }
        emptyRoot.setVisibility(View.VISIBLE);
        TextView title = emptyRoot.findViewById(R.id.emptyTitle);
        TextView subtitle = emptyRoot.findViewById(R.id.emptySubtitle);
        if (title != null) {
            title.setText(R.string.ui_no_data_found);
        }
        if (subtitle != null) {
            subtitle.setText(subtitleRes);
            subtitle.setVisibility(View.VISIBLE);
        } else if (emptyRoot instanceof TextView) {
            CharSequence sub = emptyRoot.getResources().getText(subtitleRes);
            ((TextView) emptyRoot).setText(
                    emptyRoot.getResources().getString(R.string.ui_no_data_found) + "\n" + sub);
            ((TextView) emptyRoot).setGravity(android.view.Gravity.CENTER);
        }
    }

    public static void bind(@Nullable View emptyRoot, boolean hasData, CharSequence subtitleText) {
        if (emptyRoot == null) {
            return;
        }
        if (hasData) {
            emptyRoot.setVisibility(View.GONE);
            return;
        }
        emptyRoot.setVisibility(View.VISIBLE);
        TextView title = emptyRoot.findViewById(R.id.emptyTitle);
        TextView subtitle = emptyRoot.findViewById(R.id.emptySubtitle);
        if (title != null) {
            title.setText(R.string.ui_no_data_found);
        }
        if (subtitle != null) {
            subtitle.setText(subtitleText != null ? subtitleText : "");
            subtitle.setVisibility(View.VISIBLE);
        } else if (emptyRoot instanceof TextView) {
            String titleText = emptyRoot.getResources().getString(R.string.ui_no_data_found);
            ((TextView) emptyRoot).setText(titleText + "\n" + (subtitleText != null ? subtitleText : ""));
            ((TextView) emptyRoot).setGravity(android.view.Gravity.CENTER);
        }
    }
}
