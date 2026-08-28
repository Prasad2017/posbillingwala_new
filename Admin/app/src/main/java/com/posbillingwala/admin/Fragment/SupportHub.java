package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.databinding.ItemReportMenuRowBinding;

public class SupportHub extends Fragment {
    Activity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Support");
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 40);
        root.setBackgroundColor(Color.parseColor("#F7F9FC"));

        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setBackgroundResource(R.drawable.bg_quick_action_blue);
        banner.setPadding(28, 28, 28, 28);
        TextView need = new TextView(activity);
        need.setText("Need Help?");
        need.setTextSize(18f);
        need.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
        TextView sub = new TextView(activity);
        sub.setText("Our support team is here to help you.");
        sub.setTextColor(ContextCompat.getColor(activity, R.color.colorTextSecondary));
        banner.addView(need);
        banner.addView(sub);
        root.addView(banner);

        addRow(inflater, root, R.drawable.ic_support_create, R.drawable.bg_quick_action_green, R.color.statusActive,
                "Create Ticket", "Submit a new support request",
                () -> ((MainActivity) activity).navigateDetail(new CreateSupportTicket(), "Create Ticket"));
        addRow(inflater, root, R.drawable.ic_support_tickets, R.drawable.bg_quick_action_blue, R.color.colorPrimary,
                "My Tickets", "Track open and closed tickets",
                () -> ((MainActivity) activity).navigateDetail(new MySupportTickets(), "My Tickets"));
        addRow(inflater, root, R.drawable.ic_store, R.drawable.bg_quick_action_purple, R.color.deepPurple,
                "Website Enquiries", "Contact form messages from posbillingwala.com",
                () -> ((MainActivity) activity).navigateDetail(new WebsiteContactList(), "Website Enquiries"));
        addRow(inflater, root, R.drawable.ic_support_faq, R.drawable.bg_quick_action_orange, R.color.statusTrial,
                "FAQs", "Common questions and answers",
                () -> ((MainActivity) activity).navigateDetail(SupportContentScreen.newInstance(
                        "FAQs",
                        new String[]{
                                "How do I add a new customer?",
                                "Open Customers → Add Customer, fill business details, then save.",
                                "How do license renewals work?",
                                "Open a customer → Licenses → Update Licence to extend validity.",
                                "Where can I see sales?",
                                "Use Sales Dashboard for today, or Reports → Sales Overview for trends.",
                                "How do I contact support?",
                                "Create a ticket from Help & Support. Our team replies in My Tickets."
                        }), "FAQs"));
        addRow(inflater, root, R.drawable.ic_support_video, R.drawable.bg_quick_action_green, R.color.statusActive,
                "Video Tutorials", "Learn with short videos",
                () -> ((MainActivity) activity).navigateDetail(SupportContentScreen.newInstance(
                        "Video Tutorials",
                        new String[]{
                                "Getting started",
                                "Tour the dashboard KPIs, drawer menu, and quick actions.",
                                "Managing customers",
                                "Search, filter by license status, and open customer details.",
                                "Dealer workflow",
                                "Add dealers, review performance, and open dealer reports.",
                                "Reports & sales",
                                "Use Reports hub for analytics and Sales Dashboard for live bills."
                        }), "Video Tutorials"));
        addRow(inflater, root, R.drawable.ic_support_remote, R.drawable.bg_quick_action_purple, R.color.deepPurple,
                "Remote Assistance", "Request remote help",
                () -> ((MainActivity) activity).navigateDetail(new SupportRemoteAssist(), "Remote Assistance"));

        scroll.addView(root);
        return scroll;
    }

    private void addRow(LayoutInflater inflater, LinearLayout root, int icon, int bg, int tint,
                        String title, String sub, Runnable action) {
        ItemReportMenuRowBinding row = ItemReportMenuRowBinding.inflate(inflater, root, false);
        row.menuIcon.setBackgroundResource(bg);
        row.menuIcon.setImageResource(icon);
        row.menuIcon.setColorFilter(ContextCompat.getColor(activity, tint));
        row.menuTitle.setText(title);
        row.menuSubtitle.setText(sub);
        row.getRoot().setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 12;
        root.addView(row.getRoot(), lp);
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean detail = getParentFragmentManager().getBackStackEntryCount() > 0;
        ((MainActivity) activity).lockUnlockDrawer(detail ? 1 : 0);
    }
}
