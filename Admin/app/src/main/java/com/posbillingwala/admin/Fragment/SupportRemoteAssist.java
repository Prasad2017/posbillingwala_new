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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportRemoteAssist extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Remote Assistance");
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 40);
        root.setBackgroundColor(Color.parseColor("#F7F9FC"));

        TextView info = new TextView(a);
        info.setText("Describe the issue and we’ll create a high-priority support ticket for remote help.");
        info.setTextColor(ContextCompat.getColor(a, R.color.colorTextSecondary));
        info.setPadding(0, 0, 0, 16);
        root.addView(info);

        TextView label = new TextView(a);
        label.setText("Issue details");
        label.setTextColor(ContextCompat.getColor(a, R.color.colorTextSecondary));
        root.addView(label);

        android.widget.EditText details = new android.widget.EditText(a);
        details.setMinLines(4);
        details.setBackgroundResource(R.drawable.bg_input);
        details.setPadding(28, 24, 28, 24);
        details.setHint("What should our team look at?");
        root.addView(details);

        android.widget.Button send = new android.widget.Button(a);
        send.setText("Request Remote Help");
        send.setAllCaps(false);
        send.setBackgroundResource(R.drawable.bg_button_primary);
        send.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = 24;
        send.setLayoutParams(blp);
        send.setOnClickListener(v -> {
            String msg = details.getText() != null ? details.getText().toString().trim() : "";
            if (msg.isEmpty()) {
                Toast.makeText(a, "Please describe the issue", Toast.LENGTH_SHORT).show();
                return;
            }
            Api.getClient().createSupportTicket("Admin App", "Remote Assistance",
                            "Remote assistance request", msg)
                    .enqueue(new Callback<AllApiResponse>() {
                        @Override
                        public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                            Toast.makeText(a, "Remote help request submitted", Toast.LENGTH_SHORT).show();
                            ((MainActivity) a).removeCurrentFragmentAndMoveBack();
                        }

                        @Override
                        public void onFailure(Call<AllApiResponse> call, Throwable t) {
                            Toast.makeText(a, "Could not submit request", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        root.addView(send);
        scroll.addView(root);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
    }
}
