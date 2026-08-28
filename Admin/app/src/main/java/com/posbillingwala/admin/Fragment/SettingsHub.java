package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.ItemReportMenuRowBinding;

public class SettingsHub extends Fragment {
    Activity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Settings");
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 40);
        root.setBackgroundColor(Color.parseColor("#F7F9FC"));

        LinearLayout profile = new LinearLayout(activity);
        profile.setOrientation(LinearLayout.VERTICAL);
        profile.setBackgroundResource(R.drawable.bg_card);
        profile.setPadding(28, 28, 28, 28);
        SharedPreferences prefs = activity.getSharedPreferences("admin_prefs", 0);
        TextView name = new TextView(activity);
        name.setText(prefs.getString("admin_name", "Admin User"));
        name.setTextSize(18f);
        name.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
        TextView email = new TextView(activity);
        email.setText(prefs.getString("admin_email", "admin@billingwala.com"));
        email.setTextColor(ContextCompat.getColor(activity, R.color.colorTextSecondary));
        TextView badge = new TextView(activity);
        badge.setText(prefs.getString("admin_designation", "Super Admin"));
        badge.setBackgroundResource(R.drawable.bg_badge_active);
        badge.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        badge.setPadding(24, 8, 24, 8);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = 12;
        badge.setLayoutParams(blp);
        profile.addView(name);
        profile.addView(email);
        profile.addView(badge);
        root.addView(profile);

        addSection(root, "SETTINGS");
        addRow(inflater, root, R.drawable.ic_person, R.drawable.bg_quick_action_blue, R.color.colorPrimary,
                "Profile", "Update your admin profile", () -> open(new SettingsProfile(), "Profile"));
        addRow(inflater, root, R.drawable.ic_lock, R.drawable.bg_quick_action_green, R.color.statusActive,
                "Security", "Password & account security", () -> open(new SettingsProfile.Security(), "Security"));

        addSection(root, "ADMIN");
        addRow(inflater, root, R.drawable.ic_report_licenses, R.drawable.bg_quick_action_navy, R.color.colorPrimaryDark,
                "Permissions", "Roles & module access", () -> open(new SettingsProfile.Permissions(), "Permissions"));

        addSection(root, "ACCOUNT");
        addRow(inflater, root, R.drawable.ic_logout, R.drawable.bg_quick_action_red, R.color.statusExpired,
                "Logout", "Sign out of admin app", () ->
                        new androidx.appcompat.app.AlertDialog.Builder(activity)
                                .setTitle("Logout")
                                .setMessage("Are you sure you want to logout?")
                                .setPositiveButton("Logout", (d, w) -> ((MainActivity) activity).performLogout())
                                .setNegativeButton("Cancel", null)
                                .show());

        scroll.addView(root);
        return scroll;
    }

    private void addSection(LinearLayout root, String title) {
        TextView tv = new TextView(activity);
        tv.setText(title);
        tv.setAllCaps(true);
        tv.setTextSize(11f);
        tv.setPadding(0, 28, 0, 12);
        tv.setTextColor(ContextCompat.getColor(activity, R.color.colorTextHint));
        root.addView(tv);
    }

    private void addRow(LayoutInflater inflater, LinearLayout root, int icon, int bg, int tint,
                        String title, String sub, Runnable action) {
        ItemReportMenuRowBinding row = ItemReportMenuRowBinding.inflate(inflater, root, false);
        row.menuIcon.setBackgroundResource(bg);
        row.menuIcon.setImageResource(icon);
        row.menuIcon.setColorFilter(ContextCompat.getColor(activity, tint));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(sub);
        if ("Logout".equals(title)) {
            row.menuTitle.setTextColor(ContextCompat.getColor(activity, R.color.statusExpired));
        }
        row.getRoot().setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 10;
        root.addView(row.getRoot(), lp);
    }

    private void open(Fragment f, String title) {
        ((MainActivity) activity).navigateDetail(f, title);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(0);
    }
}
