package com.pos_billingwala.NetworkToOffline;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.SplashScreen;
import com.pos_billingwala.Extra.Common;
import com.pos_billingwala.Fragment.CloudSyncStatus;
import com.pos_billingwala.Fragment.UserSetting;
import com.pos_billingwala.R;

/**
 * Opens Settings → cloud sync status from notifications and in-app taps.
 */
public final class CloudSyncNav {

    public static final String EXTRA_OPEN = "openCloudSync";
    public static final String ACTION_OPEN = "com.pos_billingwala.OPEN_CLOUD_SYNC";
    private static final String PREF_OPEN = "openCloudSync";

    private CloudSyncNav() {
    }

    public static void markPending(@NonNull Context context) {
        Common.saveUserData(context, PREF_OPEN, "1");
    }

    public static void copyOpenFlag(@Nullable Intent from, @NonNull Intent to) {
        if (from != null && (from.getBooleanExtra(EXTRA_OPEN, false) || ACTION_OPEN.equals(from.getAction()))) {
            to.putExtra(EXTRA_OPEN, true);
            to.setAction(ACTION_OPEN);
        }
    }

    public static boolean consumeOpen(@NonNull Context context, @Nullable Intent intent) {
        boolean fromIntent = intent != null
                && (intent.getBooleanExtra(EXTRA_OPEN, false) || ACTION_OPEN.equals(intent.getAction()));
        boolean fromPref = "1".equals(Common.getSavedUserData(context, PREF_OPEN));
        if (!fromIntent && !fromPref) {
            return false;
        }
        Common.saveUserData(context, PREF_OPEN, "0");
        if (intent != null) {
            intent.removeExtra(EXTRA_OPEN);
        }
        return true;
    }

    @NonNull
    public static PendingIntent contentIntent(@NonNull Context context) {
        String userId = Common.getSavedUserData(context, "userId");
        Intent intent;
        if (userId != null && !userId.trim().isEmpty()) {
            intent = new Intent(context, MainActivity.class);
        } else {
            intent = new Intent(context, SplashScreen.class);
        }
        intent.setAction(ACTION_OPEN);
        intent.putExtra(EXTRA_OPEN, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 4103, intent, flags);
    }

    public static void openFromUi(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).openCloudSyncStatus();
        }
    }

    public static void openOn(@NonNull MainActivity activity) {
        Fragment current = activity.getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (current instanceof CloudSyncStatus) {
            return;
        }
        FragmentManager fm = activity.getSupportFragmentManager();
        if (!(current instanceof UserSetting)) {
            // commitNow* cannot be used with addToBackStack — commit + executePending instead
            FragmentTransaction settingTx = fm.beginTransaction();
            settingTx.replace(R.id.frameLayout, new UserSetting());
            settingTx.addToBackStack("userSetting");
            settingTx.commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
        activity.loadFragment(new CloudSyncStatus(), true);
    }
}
