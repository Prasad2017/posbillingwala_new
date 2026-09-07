package com.pos_billingwala.Extra;

import android.content.Context;

/**
 * Process-wide application context for engines that may be called with only a DB handle.
 */
public final class AppContexts {

    private static volatile Context app;

    private AppContexts() {
    }

    public static void init(Context context) {
        if (context == null) {
            return;
        }
        app = context.getApplicationContext();
    }

    public static Context get() {
        return app;
    }

    public static Context or(Context preferred) {
        return preferred != null ? preferred : app;
    }
}
