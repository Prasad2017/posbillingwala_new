package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;

public class SettingsNotifications extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Notifications");
        SharedPreferences p = a.getSharedPreferences("admin_prefs", 0);
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = SettingsProfile.form(a);
        toggle(a, root, p, "Enable Notifications", "pref_notify");
        toggle(a, root, p, "Sound", "pref_sound");
        toggle(a, root, p, "Vibration", "pref_vibrate");
        toggle(a, root, p, "License Alerts", "pref_license_alerts");
        toggle(a, root, p, "Customer Alerts", "pref_customer_alerts");
        toggle(a, root, p, "Dealer Alerts", "pref_dealer_alerts");
        toggle(a, root, p, "System Alerts", "pref_system_alerts");
        toggle(a, root, p, "Activity Alerts", "pref_activity_alerts");
        scroll.addView(root);
        return scroll;
    }

    private void toggle(Activity a, LinearLayout root, SharedPreferences p, String label, String key) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);
        TextView t = new TextView(a);
        t.setText(label);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch s = new Switch(a);
        s.setChecked(p.getBoolean(key, true));
        s.setOnCheckedChangeListener((b, checked) -> p.edit().putBoolean(key, checked).apply());
        row.addView(t);
        row.addView(s);
        root.addView(row);
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean detail = getParentFragmentManager().getBackStackEntryCount() > 0;
        ((MainActivity) getActivity()).lockUnlockDrawer(detail ? 1 : 0);
    }
}
