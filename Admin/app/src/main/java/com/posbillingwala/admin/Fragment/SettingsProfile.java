package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.ItemReportMenuRowBinding;

/** Settings profile + nested public static child screens (required by FragmentManager). */
public class SettingsProfile extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Profile");
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = form(a);
        SharedPreferences p = a.getSharedPreferences("admin_prefs", 0);
        EditText name = field(a, root, "Full Name", p.getString("admin_name", "Admin User"));
        EditText email = field(a, root, "Email", p.getString("admin_email", "admin@billingwala.com"));
        EditText mobile = field(a, root, "Mobile Number", p.getString("admin_mobile", ""));
        EditText desig = field(a, root, "Designation", p.getString("admin_designation", "Super Admin"));
        primary(a, root, "Update Profile").setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String e = email.getText().toString().trim();
            if (n.isEmpty() || e.isEmpty()) {
                Toast.makeText(a, "Name and email are required", Toast.LENGTH_SHORT).show();
                return;
            }
            p.edit().putString("admin_name", n)
                    .putString("admin_email", e)
                    .putString("admin_mobile", mobile.getText().toString().trim())
                    .putString("admin_designation", desig.getText().toString().trim()).apply();
            Toast.makeText(a, "Profile updated", Toast.LENGTH_SHORT).show();
            ((MainActivity) a).removeCurrentFragmentAndMoveBack();
        });
        scroll.addView(root);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
    }

    static LinearLayout form(Activity a) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 40);
        root.setBackgroundColor(0xFFF7F9FC);
        return root;
    }

    static EditText field(Activity a, LinearLayout root, String label, String value) {
        TextView l = new TextView(a);
        l.setText(label);
        l.setTextColor(ContextCompat.getColor(a, R.color.colorTextSecondary));
        l.setPadding(0, 16, 0, 6);
        EditText e = new EditText(a);
        e.setText(value);
        e.setBackgroundResource(R.drawable.bg_input);
        e.setPadding(28, 24, 28, 24);
        root.addView(l);
        root.addView(e);
        return e;
    }

    static Button primary(Activity a, LinearLayout root, String text) {
        Button b = new Button(a);
        b.setText(text);
        b.setAllCaps(false);
        b.setBackgroundResource(R.drawable.bg_button_primary);
        b.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 32;
        b.setLayoutParams(lp);
        root.addView(b);
        return b;
    }

    static void toggle(Activity a, LinearLayout root, SharedPreferences p, String label, String key, boolean def) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);
        TextView t = new TextView(a);
        t.setText(label);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        t.setTextColor(ContextCompat.getColor(a, R.color.colorTextPrimary));
        Switch s = new Switch(a);
        s.setChecked(p.getBoolean(key, def));
        s.setOnCheckedChangeListener((b, checked) -> p.edit().putBoolean(key, checked).apply());
        row.addView(t);
        row.addView(s);
        root.addView(row);
    }

    static void note(Activity a, LinearLayout root, String text) {
        TextView n = new TextView(a);
        n.setText(text);
        n.setTextColor(ContextCompat.getColor(a, R.color.colorTextSecondary));
        n.setPadding(0, 8, 0, 16);
        root.addView(n);
    }

    public static class Security extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Activity a = getActivity();
            ((MainActivity) a).setScreenTitle("Security");
            SharedPreferences p = a.getSharedPreferences("admin_prefs", 0);
            ScrollView scroll = new ScrollView(a);
            LinearLayout root = form(a);
            TextView banner = new TextView(a);
            banner.setText("Your account is secure\nLast password change: "
                    + p.getString("admin_password_updated", "Not set yet"));
            banner.setBackgroundResource(R.drawable.bg_card);
            banner.setPadding(28, 28, 28, 28);
            banner.setTextColor(ContextCompat.getColor(a, R.color.statusActive));
            root.addView(banner);
            EditText current = field(a, root, "Current Password", "");
            current.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            EditText next = field(a, root, "New Password", "");
            next.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            EditText confirm = field(a, root, "Confirm New Password", "");
            confirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggle(a, root, p, "Require password on app open", "pref_lock_on_open", false);
            toggle(a, root, p, "Two-step verification reminder", "pref_2fa_reminder", true);
            primary(a, root, "Update Password").setOnClickListener(v -> {
                String cur = current.getText().toString();
                String nw = next.getText().toString();
                String cf = confirm.getText().toString();
                String saved = p.getString("admin_password", "");
                if (nw.length() < 6) {
                    Toast.makeText(a, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!nw.equals(cf)) {
                    Toast.makeText(a, "New passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!saved.isEmpty() && !saved.equals(cur)) {
                    Toast.makeText(a, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    return;
                }
                String stamp = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                        .format(new java.util.Date());
                p.edit().putString("admin_password", nw)
                        .putString("admin_password_updated", stamp).apply();
                Toast.makeText(a, "Password updated", Toast.LENGTH_SHORT).show();
                ((MainActivity) a).removeCurrentFragmentAndMoveBack();
            });
            scroll.addView(root);
            return scroll;
        }

        @Override
        public void onStart() {
            super.onStart();
            ((MainActivity) getActivity()).lockUnlockDrawer(1);
        }
    }

    public static class Language extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Activity a = getActivity();
            ((MainActivity) a).setScreenTitle("Language");
            SharedPreferences p = a.getSharedPreferences("admin_prefs", 0);
            final String[] current = {p.getString("pref_lang", "English")};
            ScrollView scroll = new ScrollView(a);
            LinearLayout root = form(a);
            String[] langs = {"English", "Marathi", "Hindi", "Gujarati", "Kannada", "Tamil", "Telugu", "Punjabi", "Bengali"};
            final TextView[] rows = new TextView[langs.length];
            for (int i = 0; i < langs.length; i++) {
                final String lang = langs[i];
                TextView row = new TextView(a);
                rows[i] = row;
                row.setText((lang.equals(current[0]) ? "●  " : "○  ") + lang);
                row.setPadding(16, 28, 16, 28);
                row.setTextColor(ContextCompat.getColor(a, R.color.colorTextPrimary));
                row.setOnClickListener(v -> {
                    current[0] = lang;
                    for (int j = 0; j < langs.length; j++) {
                        rows[j].setText((langs[j].equals(current[0]) ? "●  " : "○  ") + langs[j]);
                    }
                });
                root.addView(row);
            }
            primary(a, root, "Apply Language").setOnClickListener(v -> {
                p.edit().putString("pref_lang", current[0]).apply();
                Toast.makeText(a, current[0] + " applied for this device", Toast.LENGTH_SHORT).show();
                ((MainActivity) a).removeCurrentFragmentAndMoveBack();
            });
            scroll.addView(root);
            return scroll;
        }

        @Override
        public void onStart() {
            super.onStart();
            ((MainActivity) getActivity()).lockUnlockDrawer(1);
        }
    }

    public static class System extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return listScreen(getActivity(), inflater, "System Settings", new String[][]{
                    {"General Settings", "Company defaults and app behaviour"},
                    {"Backup Settings", "Automatic backup and restore options"},
                    {"Email Settings", "Outgoing email / SMTP preferences"},
                    {"SMS Settings", "SMS gateway and alert templates"},
                    {"Maintenance Mode", "Temporarily lock POS operations"},
                    {"System Logs", "Retention and diagnostics"}
            });
        }

        static View listScreen(Activity a, LayoutInflater inflater, String title, String[][] items) {
            ((MainActivity) a).setScreenTitle(title);
            ScrollView scroll = new ScrollView(a);
            LinearLayout root = form(a);
            for (String[] item : items) {
                ItemReportMenuRowBinding row = ItemReportMenuRowBinding.inflate(inflater, root, false);
                row.menuIcon.setBackgroundResource(R.drawable.bg_quick_action_blue);
                row.menuIcon.setImageResource(R.drawable.ic_settings);
                row.menuIcon.setColorFilter(ContextCompat.getColor(a, R.color.colorPrimary));
                row.menuTitle.setText(item[0]);
                row.menuSubtitle.setText(item[1]);
                row.getRoot().setOnClickListener(v ->
                        ((MainActivity) a).navigateDetail(NestedDetail.newInstance(item[0]), item[0]));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = 10;
                root.addView(row.getRoot(), lp);
            }
            scroll.addView(root);
            return scroll;
        }

        @Override
        public void onStart() {
            super.onStart();
            ((MainActivity) getActivity()).lockUnlockDrawer(1);
        }
    }

    public static class LicensePrefs extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return System.listScreen(getActivity(), inflater, "License Settings", new String[][]{
                    {"License Overview", "Status summary and grace defaults"},
                    {"License Alerts", "Push and email license alerts"},
                    {"Expiry Notifications", "When to notify before expiry"},
                    {"Grace Period Settings", "Days allowed after expiry"}
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            ((MainActivity) getActivity()).lockUnlockDrawer(1);
        }
    }

    public static class Permissions extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return System.listScreen(getActivity(), inflater, "Permissions", new String[][]{
                    {"Admin Users", "Who can access the admin app"},
                    {"Roles & Permissions", "Role templates and capabilities"},
                    {"Module Access", "Enable or disable modules"},
                    {"Activity Audit", "Track sensitive admin actions"}
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            ((MainActivity) getActivity()).lockUnlockDrawer(1);
        }
    }

    public static class NestedDetail extends Fragment {
        public static NestedDetail newInstance(String title) {
            NestedDetail f = new NestedDetail();
            Bundle b = new Bundle();
            b.putString("title", title);
            f.setArguments(b);
            return f;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Activity a = getActivity();
            String title = getArguments() != null ? getArguments().getString("title", "Settings") : "Settings";
            ((MainActivity) a).setScreenTitle(title);
            SharedPreferences p = a.getSharedPreferences("admin_prefs", 0);
            ScrollView scroll = new ScrollView(a);
            LinearLayout root = form(a);
            note(a, root, "Configure " + title.toLowerCase() + ". Changes save automatically.");

            switch (title) {
                case "General Settings":
                    toggle(a, root, p, "Show greeting on dashboard", "sys_show_greeting", true);
                    toggle(a, root, p, "Confirm before logout", "sys_confirm_logout", true);
                    field(a, root, "Company display name", p.getString("sys_company_name", "POS Billingwala"));
                    break;
                case "Backup Settings":
                    toggle(a, root, p, "Auto daily backup", "sys_auto_backup", true);
                    toggle(a, root, p, "Include media files", "sys_backup_media", false);
                    field(a, root, "Backup retention (days)", p.getString("sys_backup_days", "30"));
                    break;
                case "Email Settings":
                    field(a, root, "SMTP host", p.getString("sys_smtp_host", ""));
                    field(a, root, "From email", p.getString("sys_smtp_from", ""));
                    toggle(a, root, p, "Send license emails", "sys_email_licenses", true);
                    break;
                case "SMS Settings":
                    field(a, root, "SMS sender ID", p.getString("sys_sms_sender", "BILLWA"));
                    toggle(a, root, p, "SMS on license expiry", "sys_sms_expiry", true);
                    toggle(a, root, p, "SMS on new customer", "sys_sms_customer", false);
                    break;
                case "Maintenance Mode":
                    toggle(a, root, p, "Enable maintenance mode", "sys_maintenance", false);
                    field(a, root, "Maintenance message",
                            p.getString("sys_maintenance_msg", "System under maintenance"));
                    break;
                case "System Logs":
                    toggle(a, root, p, "Keep crash logs", "sys_keep_crash", true);
                    toggle(a, root, p, "Keep API error logs", "sys_keep_api", true);
                    field(a, root, "Log retention (days)", p.getString("sys_log_days", "14"));
                    break;
                case "License Overview":
                    toggle(a, root, p, "Show license badges on lists", "lic_show_badges", true);
                    toggle(a, root, p, "Highlight expiring soon", "lic_highlight_expiring", true);
                    break;
                case "License Alerts":
                    toggle(a, root, p, "Push license alerts", "lic_push", true);
                    toggle(a, root, p, "Email license alerts", "lic_email", true);
                    break;
                case "Expiry Notifications":
                    field(a, root, "Notify days before expiry", p.getString("lic_notify_days", "7"));
                    toggle(a, root, p, "Notify on trial end", "lic_notify_trial", true);
                    break;
                case "Grace Period Settings":
                    field(a, root, "Grace period (days)", p.getString("lic_grace_days", "3"));
                    toggle(a, root, p, "Allow login during grace", "lic_grace_login", true);
                    break;
                case "Admin Users":
                    note(a, root, "Primary admin: " + p.getString("admin_email", "admin@billingwala.com"));
                    toggle(a, root, p, "Allow additional admin logins", "perm_multi_admin", false);
                    break;
                case "Roles & Permissions":
                    toggle(a, root, p, "Super Admin full access", "perm_super", true);
                    toggle(a, root, p, "Support role can reply tickets", "perm_support_reply", true);
                    toggle(a, root, p, "Dealer managers can edit customers", "perm_dealer_edit", false);
                    break;
                case "Module Access":
                    toggle(a, root, p, "Sales module", "mod_sales", true);
                    toggle(a, root, p, "Reports module", "mod_reports", true);
                    toggle(a, root, p, "Crash logs module", "mod_crash", true);
                    toggle(a, root, p, "Catalog export module", "mod_catalog", true);
                    break;
                case "Activity Audit":
                    toggle(a, root, p, "Log profile changes", "audit_profile", true);
                    toggle(a, root, p, "Log license updates", "audit_license", true);
                    toggle(a, root, p, "Log logout events", "audit_logout", false);
                    break;
                default:
                    note(a, root, "No extra options for this section.");
                    break;
            }

            primary(a, root, "Save").setOnClickListener(v -> {
                for (int i = 0; i < root.getChildCount(); i++) {
                    View child = root.getChildAt(i);
                    if (!(child instanceof EditText) || i == 0) continue;
                    View prev = root.getChildAt(i - 1);
                    if (!(prev instanceof TextView)) continue;
                    String label = ((TextView) prev).getText().toString();
                    String key = keyForLabel(label);
                    if (key != null) p.edit().putString(key, ((EditText) child).getText().toString().trim()).apply();
                }
                Toast.makeText(a, title + " saved", Toast.LENGTH_SHORT).show();
                ((MainActivity) a).removeCurrentFragmentAndMoveBack();
            });
            scroll.addView(root);
            return scroll;
        }

        private static String keyForLabel(String label) {
            if ("Company display name".equals(label)) return "sys_company_name";
            if ("Backup retention (days)".equals(label)) return "sys_backup_days";
            if ("SMTP host".equals(label)) return "sys_smtp_host";
            if ("From email".equals(label)) return "sys_smtp_from";
            if ("SMS sender ID".equals(label)) return "sys_sms_sender";
            if ("Maintenance message".equals(label)) return "sys_maintenance_msg";
            if ("Log retention (days)".equals(label)) return "sys_log_days";
            if ("Notify days before expiry".equals(label)) return "lic_notify_days";
            if ("Grace period (days)".equals(label)) return "lic_grace_days";
            return null;
        }

        @Override
        public void onStart() {
            super.onStart();
            ((MainActivity) getActivity()).lockUnlockDrawer(1);
        }
    }
}
