package com.pos_billingwala.Extra.dynamic;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import com.pos_billingwala.Extra.DynamicUiEngine;

/**
 * Report visibility resolver facade (Phase 11).
 */
public final class ReportResolver {

    private ReportResolver() {
    }

    public static boolean isVisible(Context context, String reportCode) {
        return DynamicUiEngine.isReportVisible(context, reportCode);
    }

    public static void apply(Context context, @Nullable View view, String reportCode) {
        DynamicUiEngine.applyReport(context, view, reportCode);
    }
}
