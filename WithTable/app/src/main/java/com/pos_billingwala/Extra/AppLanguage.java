package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.pos_billingwala.R;

import java.util.Locale;

/**
 * App language: English / Hindi / Marathi.
 * Uses AppCompat per-app locales (required so resources actually switch).
 */
public final class AppLanguage {

    public static final String KEY_APP_LANGUAGE = "appLanguage";
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

    /** Wrap activity context — backup so inflate uses the right strings.xml. */
    @NonNull
    public static Context wrap(@NonNull Context context) {
        String code = getSavedCode(context);
        Locale locale = localeFor(code);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        applyLocaleToConfig(config, locale);
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
     * Call from setScreenSizeSmall() so fontScale update does not wipe the app language.
     */
    public static void preserveLocaleOnConfig(@NonNull Context context, @NonNull Configuration configuration) {
        applyLocaleToConfig(configuration, localeFor(getSavedCode(context)));
    }

    /** Cold start — sync AppCompat locales with saved preference. */
    public static void applyStored(@NonNull Context context) {
        String code = getSavedCode(context);
        Locale.setDefault(localeFor(code));
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code));
    }

    /**
     * Save language and apply via AppCompat so @string / values-hi / values-mr load correctly.
     * MainActivity reopens User Setting via KEY_REOPEN_USER_SETTING.
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
        Common.saveUserData(activity, "languageToastPending", "1");

        Locale.setDefault(localeFor(code));
        // This is what actually switches resources to values-hi / values-mr
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code));
    }

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
