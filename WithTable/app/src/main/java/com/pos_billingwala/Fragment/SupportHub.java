package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.SupportUiHelper;
import com.pos_billingwala.R;

public class SupportHub extends Fragment {
    Activity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = SupportUiHelper.form(activity);
        SupportUiHelper.addScreenHeader(activity, root, getString(R.string.setting_support),
                () -> ((MainActivity) activity).navigateBack());
        SupportUiHelper.notice(activity, root, getString(R.string.support_online_only_notice));
        SupportUiHelper.primary(activity, root, getString(R.string.support_create_ticket))
                .setOnClickListener(v -> openIfOnline(new CreateSupportTicket()));
        SupportUiHelper.primary(activity, root, getString(R.string.support_my_tickets))
                .setOnClickListener(v -> openIfOnline(new MySupportTickets()));
        scroll.addView(root);
        return scroll;
    }

    private void openIfOnline(Fragment target) {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        ((MainActivity) activity).loadFragment(target, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
