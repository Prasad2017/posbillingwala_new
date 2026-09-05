package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.MessSessionCount;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessTokenTodayFragment extends Fragment {

    private Activity activity;
    private TextView countsText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_token_today, container, false);
        activity = getActivity();
        countsText = view.findViewById(R.id.countsText);
        ImageView back = view.findViewById(R.id.backBtn);
        if (back != null) {
            back.setOnClickListener(v -> {
                if (activity != null) {
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                }
            });
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadCounts();
    }

    private void loadCounts() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String userId = MainActivity.userId;
        Api.getClient().getMessMealTokenToday(userId, today).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (countsText == null) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    countsText.setText("Unable to load Mess token stats.");
                    return;
                }
                StringBuilder sb = new StringBuilder("TODAY\n\n");
                if (response.body().messSessionCounts != null && !response.body().messSessionCounts.isEmpty()) {
                    for (MessSessionCount c : response.body().messSessionCounts) {
                        sb.append(c.sessionName != null ? c.sessionName : "Session").append("\n")
                                .append("Generated: ").append(c.generated).append("\n")
                                .append("Printed: ").append(c.printed).append("\n")
                                .append("Pending: ").append(c.pending).append("\n")
                                .append("Failed: ").append(c.failed).append("\n\n");
                    }
                } else {
                    sb.append("No tokens generated yet today.");
                }
                countsText.setText(sb.toString().trim());
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                if (countsText != null) {
                    countsText.setText("Unable to load Mess token stats.");
                }
            }
        });
    }
}
