package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Shared loading UI for list screens and async DB loads.
 * Prefer {@link #show(Context)} for full-screen list loads;
 * use {@link #setVisible(ProgressBar, boolean)} for in-layout loaders (e.g. CreatePos).
 */
public final class ListLoader {

    private static final String BAR_COLOR = "#2D7FED";

    private ListLoader() {
    }

    @Nullable
    public static SweetAlertDialog show(@Nullable Context context) {
        return show(context, "Loading");
    }

    @Nullable
    public static SweetAlertDialog show(@Nullable Context context, @Nullable String title) {
        if (context == null) {
            return null;
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing()) {
                return null;
            }
        }
        try {
            SweetAlertDialog dialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
            dialog.getProgressHelper().setBarColor(Color.parseColor(BAR_COLOR));
            dialog.setTitleText(title != null && !title.trim().isEmpty() ? title : "Loading");
            dialog.setCancelable(false);
            dialog.show();
            return dialog;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void dismiss(@Nullable SweetAlertDialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Show loader only if fragment is still added; returns null otherwise.
     */
    @Nullable
    public static SweetAlertDialog showForFragment(@Nullable Fragment fragment) {
        if (fragment == null || !fragment.isAdded() || fragment.getContext() == null) {
            return null;
        }
        return show(fragment.requireContext());
    }

    public static void setVisible(@Nullable ProgressBar progressBar, boolean visible) {
        if (progressBar == null) {
            return;
        }
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
