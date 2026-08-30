package com.posbillingwala.owner.Extra;

import android.view.View;

public final class RowDividerUi {

    private RowDividerUi() {
    }

    public static void bindLastItem(View divider, int position, int itemCount) {
        if (divider != null) {
            divider.setVisibility(position >= itemCount - 1 ? View.GONE : View.VISIBLE);
        }
    }
}
