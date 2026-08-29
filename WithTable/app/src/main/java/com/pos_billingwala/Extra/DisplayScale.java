package com.pos_billingwala.Extra;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Locks UI scale for POS layouts. Most screens use {@code dp} for text (not {@code sp}),
 * so we scale {@code densityDpi} — not {@code fontScale} — to keep size consistent.
 */
public final class DisplayScale {

    /** 0.95 = slightly compact POS UI (dp text scales via densityDpi). */
    public static final float UI_SCALE = 0.95f;

    public static final float LOCKED_FONT_SCALE = 1.0f;

    private static volatile int cachedTargetDensityDpi = -1;

    private DisplayScale() {
    }

    public static int getTargetDensityDpi() {
        if (cachedTargetDensityDpi < 0) {
            int stable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    ? DisplayMetrics.DENSITY_DEVICE_STABLE
                    : DisplayMetrics.DENSITY_DEFAULT;
            cachedTargetDensityDpi = Math.max(120, Math.round(stable * UI_SCALE));
        }
        return cachedTargetDensityDpi;
    }

    @NonNull
    public static Context wrap(@NonNull Context context) {
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (!isLocked(config)) {
            applyLock(config);
            context = context.createConfigurationContext(config);
        }
        return context;
    }

    public static void applyLock(@NonNull Configuration configuration) {
        configuration.fontScale = LOCKED_FONT_SCALE;
        configuration.densityDpi = getTargetDensityDpi();
    }

    public static boolean isLocked(@NonNull Configuration configuration) {
        return configuration.fontScale == LOCKED_FONT_SCALE
                && configuration.densityDpi == getTargetDensityDpi();
    }

    /**
     * Use from Activity/Application {@code getResources()} — works on Android 13+ where
     * {@code updateConfiguration} no longer affects rendered text.
     */
    @NonNull
    public static Resources adjustResources(@NonNull Context context, @NonNull Resources base) {
        Configuration config = new Configuration(base.getConfiguration());
        DisplayMetrics metrics = base.getDisplayMetrics();
        boolean densityOk = Math.abs(metrics.density
                - getTargetDensityDpi() / (float) DisplayMetrics.DENSITY_DEFAULT) < 0.01f;
        if (isLocked(config) && densityOk && AppLanguage.configHasSavedLocale(context, config)) {
            return base;
        }
        applyLock(config);
        AppLanguage.preserveLocaleOnConfig(context, config);
        Resources adjusted = context.createConfigurationContext(config).getResources();
        syncMetrics(adjusted.getDisplayMetrics());
        return adjusted;
    }

    /** Re-apply after language change or system display-size change. */
    @SuppressWarnings("deprecation")
    public static void refresh(@NonNull Context context) {
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        applyLock(config);
        AppLanguage.preserveLocaleOnConfig(context, config);
        DisplayMetrics metrics = resources.getDisplayMetrics();
        syncMetrics(metrics);
        resources.updateConfiguration(config, metrics);
    }

    static void syncMetrics(@NonNull DisplayMetrics metrics) {
        metrics.densityDpi = getTargetDensityDpi();
        metrics.density = getTargetDensityDpi() / (float) DisplayMetrics.DENSITY_DEFAULT;
        metrics.scaledDensity = metrics.density * LOCKED_FONT_SCALE;
    }

    /** Clear cached Resources when configuration changes (Activity). */
    public static void clearCachedResources(@Nullable Object host) {
        if (host instanceof ResourcesHost) {
            ((ResourcesHost) host).clearAdjustedResources();
        }
    }

    public interface ResourcesHost {
        void clearAdjustedResources();
    }
}
