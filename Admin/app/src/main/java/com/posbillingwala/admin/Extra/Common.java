package com.posbillingwala.admin.Extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by Prasad
 */

public class Common {

    public static final String SHARED_PREF = "userData";
    public static final String directory_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath() + "/POS Billingwala/";

    public static void saveUserData(Context context, String key, String value) {
        try {
            if (value != null) {
                SharedPreferences pref = context.getSharedPreferences(SHARED_PREF, 0);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(key, value);
                editor.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSavedUserData(Context context, String key) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF, 0);
        return pref.getString(key, "");

    }

    public static String capitalize(String capString) {
        StringBuffer capBuffer = new StringBuffer();
        Matcher capMatcher = Pattern.compile("([a-z])([a-z]*)", Pattern.CASE_INSENSITIVE).matcher(capString);
        while (capMatcher.find()) {
            capMatcher.appendReplacement(capBuffer, capMatcher.group(1).toUpperCase() + capMatcher.group(2).toLowerCase());
        }

        return capMatcher.appendTail(capBuffer).toString();
    }


}
