package com.pos_billingwala.Utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DisplayUtils {
    public static DisplayMetrics metrics;
    public static WindowManager wm;

    public static void setDisplaySettings(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        // Get the current screen layout
        int screenLayout = resources.getConfiguration().screenLayout;
        // Check the screen size using Configuration constants
        int screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        // Output the information
        switch (screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                // Undefined screen size
                configuration.fontScale = 1.2f;
                configuration.screenLayout &= ~Configuration.SCREENLAYOUT_SIZE_MASK; // Clear the size bits
                configuration.screenLayout |= Configuration.SCREENLAYOUT_SIZE_SMALL; // Set the desired size
                metrics = resources.getDisplayMetrics();
                //  metrics.scaledDensity = configuration.fontScale * metrics.density;
                resources.updateConfiguration(configuration, metrics);
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
            default:
                // Undefined screen size
                configuration.fontScale = 1f;
                configuration.screenLayout &= ~Configuration.SCREENLAYOUT_SIZE_MASK; // Clear the size bits
                configuration.screenLayout |= Configuration.SCREENLAYOUT_SIZE_SMALL; // Set the desired size
                metrics = resources.getDisplayMetrics();
                // metrics.scaledDensity = configuration.fontScale * metrics.density;
                resources.updateConfiguration(configuration, metrics);
                break;
        }
    }

}
