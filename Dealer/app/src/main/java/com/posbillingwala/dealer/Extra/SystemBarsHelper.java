package com.posbillingwala.dealer.Extra;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.posbillingwala.dealer.R;

/** Keep content below the system status bar on all screens. */
public final class SystemBarsHelper {
    private SystemBarsHelper() {}

    public static void applyBelowStatusBar(@NonNull Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        int statusColor = ContextCompat.getColor(activity, R.color.colorPrimary);
        window.setStatusBarColor(statusColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.WHITE);
        }
        View decor = window.getDecorView();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decor);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }
    }
}
