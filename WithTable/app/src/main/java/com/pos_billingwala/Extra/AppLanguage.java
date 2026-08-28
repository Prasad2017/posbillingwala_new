package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.R;

import java.util.Locale;

/**
 * App language: English / Hindi / Marathi.
 * <p>
 * Driven by SharedPreferences + {@link #wrap(Context)}. Live calls to
 * {@code AppCompatDelegate.setApplicationLocales} recreate the Activity and look
 * like a crash, so language changes apply in-place instead.
 */
public final class AppLanguage {

    public static final String KEY_APP_LANGUAGE = "appLanguage";

    /** @deprecated Kept for clearing any leftover flag from older builds. */
    @Deprecated
    public static final String KEY_REOPEN_USER_SETTING = "reopenUserSetting";

    public static final String EN = "en";
    public static final String HI = "hi";
    public static final String MR = "mr";

    private AppLanguage() {
    }

    @NonNull
    public static String getSavedCode(@NonNull Context context) {
        String code = Common.getSavedUserData(context, KEY_APP_LANGUAGE);
        if (code == null || code.trim().isEmpty()) {
            return EN;
        }
        code = code.trim().toLowerCase(Locale.ROOT);
        if (HI.equals(code) || MR.equals(code) || EN.equals(code)) {
            return code;
        }
        return EN;
    }

    @NonNull
    public static Locale localeFor(@NonNull String languageCode) {
        if (HI.equals(languageCode)) {
            return new Locale("hi");
        }
        if (MR.equals(languageCode)) {
            return new Locale("mr");
        }
        return Locale.ENGLISH;
    }

    /** Wrap activity context so inflate uses the right strings.xml. */
    @NonNull
    public static Context wrap(@NonNull Context context) {
        String code = getSavedCode(context);
        Locale locale = localeFor(code);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        applyLocaleToConfig(config, locale);
        DisplayScale.applyLock(config);
        return context.createConfigurationContext(config);
    }

    public static void applyLocaleToConfig(@NonNull Configuration configuration, @NonNull Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.setLocale(locale);
        }
    }

    /**
     * Call when updating Configuration so locale is not lost (see {@link DisplayScale#refresh}).
     */
    public static void preserveLocaleOnConfig(@NonNull Context context, @NonNull Configuration configuration) {
        applyLocaleToConfig(configuration, localeFor(getSavedCode(context)));
    }

    /**
     * Cold start — set JVM default from prefs and clear any prior AppCompat per-app
     * locales (from older builds) so {@link #wrap} alone controls language.
     * Safe only from Application.onCreate (no Activity on screen yet).
     */
    public static void applyStored(@NonNull Context context) {
        Locale.setDefault(localeFor(getSavedCode(context)));
        try {
            if (!AppCompatDelegate.getApplicationLocales().isEmpty()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            }
        } catch (Exception ignored) {
            // Ignore — wrap() still applies the saved language.
        }
    }

    /**
     * Save language and apply in-place without closing/recreating the Activity.
     */
    @SuppressWarnings("deprecation")
    public static void setLanguage(@NonNull Activity activity, @NonNull String languageCode) {
        String code = languageCode.trim().toLowerCase(Locale.ROOT);
        if (!EN.equals(code) && !HI.equals(code) && !MR.equals(code)) {
            code = EN;
        }
        if (code.equals(getSavedCode(activity))) {
            return;
        }
        Common.saveUserData(activity, KEY_APP_LANGUAGE, code);
        // Clear legacy recreate flag if present from older builds
        Common.saveUserData(activity, KEY_REOPEN_USER_SETTING, "0");
        Common.saveUserData(activity, "languageToastPending", "0");

        Locale locale = localeFor(code);
        Locale.setDefault(locale);
        applyLocaleToResources(activity, locale);
        applyLocaleToResources(activity.getApplicationContext(), locale);
        DisplayScale.clearCachedResources(activity);

        if (activity instanceof MainActivity) {
            ((MainActivity) activity).reloadAfterLanguageChange();
        }
        Toast.makeText(activity, R.string.language_changed, Toast.LENGTH_SHORT).show();
    }

    /** Update Resources so reinflated layouts pick values-hi / values-mr immediately. */
    @SuppressWarnings("deprecation")
    private static void applyLocaleToResources(@NonNull Context context, @NonNull Locale locale) {
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        applyLocaleToConfig(config, locale);
        DisplayScale.applyLock(config);
        DisplayMetrics metrics = resources.getDisplayMetrics();
        DisplayScale.syncMetrics(metrics);
        resources.updateConfiguration(config, metrics);
        DisplayScale.clearCachedResources(context instanceof DisplayScale.ResourcesHost
                ? (DisplayScale.ResourcesHost) context : null);
    }

    @SuppressWarnings("deprecation")
    public static boolean consumeReopenUserSetting(@NonNull Context context) {
        if ("1".equals(Common.getSavedUserData(context, KEY_REOPEN_USER_SETTING))) {
            Common.saveUserData(context, KEY_REOPEN_USER_SETTING, "0");
            return true;
        }
        return false;
    }

    @NonNull
    public static String displayName(@NonNull Context context, @NonNull String code) {
        switch (code) {
            case HI:
                return context.getString(R.string.language_hindi);
            case MR:
                return context.getString(R.string.language_marathi);
            case EN:
            default:
                return context.getString(R.string.language_english);
        }
    }

    public static int selectedIndex(@NonNull Context context) {
        String code = getSavedCode(context);
        if (HI.equals(code)) {
            return 1;
        }
        if (MR.equals(code)) {
            return 2;
        }
        return 0;
    }

    @NonNull
    public static String codeForIndex(int index) {
        if (index == 1) {
            return HI;
        }
        if (index == 2) {
            return MR;
        }
        return EN;
    }
}
