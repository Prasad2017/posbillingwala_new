package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentSupportHubBinding;

public class SupportHub extends Fragment {
    Activity activity;
    FragmentSupportHubBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        binding = FragmentSupportHubBinding.inflate(inflater, container, false);

        binding.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());
        binding.createTicketRow.setOnClickListener(v -> openIfOnline(new CreateSupportTicket()));
        binding.myTicketsRow.setOnClickListener(v -> openIfOnline(new MySupportTickets()));
        binding.getRoot().findViewById(R.id.supportCallButton).setOnClickListener(v -> dialSupport());

        TabletFormUi.applyCenteredPanel(binding.supportContent);
        return binding.getRoot();
    }

    private void openIfOnline(Fragment target) {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        ((MainActivity) activity).loadFragment(target, true);
    }

    private void dialSupport() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
    }
}
