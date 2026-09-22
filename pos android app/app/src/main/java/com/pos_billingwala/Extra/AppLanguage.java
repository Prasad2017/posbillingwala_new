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

import com.pos_billingwala.R;

import java.util.Locale;

/**
 * App language: English / Hindi / Marathi.
 * <p>
 * Android 13+ (this app targets API 37) applies per-app locales through
 * {@link AppCompatDelegate#setApplicationLocales}. In-place
 * {@code Resources.updateConfiguration} is ignored, so changing language must
 * persist the code, set application locales, and let the Activity restart.
 */
public final class AppLanguage {

    public static final String KEY_APP_LANGUAGE = "appLanguage";
    public static final String KEY_REOPEN_USER_SETTING = "reopenUserSetting";
    public static final String KEY_LANGUAGE_TOAST_PENDING = "languageToastPending";

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

    /** Wrap activity/application context so inflate uses the right strings.xml. */
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

    public static boolean configHasSavedLocale(@NonNull Context context, @NonNull Configuration configuration) {
        Locale wanted = localeFor(getSavedCode(context));
        Locale actual;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList locales = configuration.getLocales();
            actual = locales == null || locales.isEmpty() ? null : locales.get(0);
        } else {
            actual = configuration.locale;
        }
        return actual != null && wanted.getLanguage().equals(actual.getLanguage());
    }

    /**
     * Cold start — apply saved language through the official per-app locale API
     * so Android 13+ does not fall back to the device language.
     */
    public static void applyStored(@NonNull Context context) {
        String code = getSavedCode(context);
        Locale.setDefault(localeFor(code));
        syncApplicationLocales(code);
    }

    /**
     * Save language and apply it app-wide. AppCompat recreates activities so every
     * screen inflates from values-hi / values-mr.
     */
    public static void setLanguage(@NonNull Activity activity, @NonNull String languageCode) {
        String code = languageCode.trim().toLowerCase(Locale.ROOT);
        if (!EN.equals(code) && !HI.equals(code) && !MR.equals(code)) {
            code = EN;
        }
        if (code.equals(getSavedCode(activity))) {
            return;
        }
        Common.saveUserData(activity, KEY_APP_LANGUAGE, code);
        Common.saveUserData(activity, KEY_REOPEN_USER_SETTING, "1");
        Common.saveUserData(activity, KEY_LANGUAGE_TOAST_PENDING, "1");

        Locale locale = localeFor(code);
        Locale.setDefault(locale);
        DisplayScale.clearCachedResources(activity);
        DisplayScale.clearCachedResources(activity.getApplicationContext());

        boolean alreadyApplied = isApplicationLocale(code);
        syncApplicationLocales(code);

        if (alreadyApplied) {
            applyLocaleToResources(activity, locale);
            applyLocaleToResources(activity.getApplicationContext(), locale);
            activity.recreate();
        }
    }

    public static boolean shouldReopenUserSetting(@NonNull Context context) {
        return "1".equals(Common.getSavedUserData(context, KEY_REOPEN_USER_SETTING));
    }

    public static boolean consumeReopenUserSetting(@NonNull Context context) {
        if (shouldReopenUserSetting(context)) {
            Common.saveUserData(context, KEY_REOPEN_USER_SETTING, "0");
            return true;
        }
        return false;
    }

    public static void showPendingLanguageToast(@NonNull Context context) {
        if ("1".equals(Common.getSavedUserData(context, KEY_LANGUAGE_TOAST_PENDING))) {
            Common.saveUserData(context, KEY_LANGUAGE_TOAST_PENDING, "0");
            Toast.makeText(context, R.string.language_changed, Toast.LENGTH_SHORT).show();
        }
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

    private static boolean isApplicationLocale(@NonNull String code) {
        try {
            LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
            return !current.isEmpty() && code.equals(current.toLanguageTags());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void syncApplicationLocales(@NonNull String code) {
        try {
            LocaleListCompat locales = LocaleListCompat.forLanguageTags(code);
            if (!locales.equals(AppCompatDelegate.getApplicationLocales())) {
                AppCompatDelegate.setApplicationLocales(locales);
            }
        } catch (Exception ignored) {
            // wrap() still applies the saved language on older devices.
        }
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
}
