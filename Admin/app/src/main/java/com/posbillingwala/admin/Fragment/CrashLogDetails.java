package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.data.PieEntry;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ReportRankItem;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentGenericReportBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class CrashLogDetails extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        ((MainActivity) a).setScreenTitle("Crash Details");
        String crashId = getArguments() != null ? getArguments().getString("crashId") : "";
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = SettingsProfile.form(a);
        TextView body = new TextView(a);
        body.setTextIsSelectable(true);
        root.addView(body);
        Button resolve = SettingsProfile.primary(a, root, "Mark as Resolved");
        Button share = SettingsProfile.primary(a, root, "Share");
        Api.getClient().getCrashDetails(crashId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || response.body() == null) return;
                AllApiResponse b = response.body();
                body.setText(b.getErrorTitle() + "\n" + b.getErrorClass() + "\nStatus: " + b.getStatus()
                        + "\nApp: " + b.getAppName() + "\nVersion: " + b.getAppVersion()
                        + "\nDevice: " + b.getDeviceName() + "\nAndroid: " + b.getAndroidVersion()
                        + "\nUser: " + b.getUserName() + " (" + b.getOccurrences() + " times)"
                        + "\nOccurred: " + b.getCreatedAt()
                        + "\n\nStack Trace:\n" + ReportUiHelper.nz(b.getStackTrace()));
                share.setOnClickListener(v -> {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, body.getText().toString());
                    a.startActivity(Intent.createChooser(i, "Share crash"));
                });
            }
            @Override public void onFailure(Call<AllApiResponse> call, Throwable t) {}
        });
        resolve.setOnClickListener(v -> Api.getClient().updateCrashStatus(crashId, "Resolved")
                .enqueue(new Callback<AllApiResponse>() {
                    @Override public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        Toast.makeText(a, "Marked resolved", Toast.LENGTH_SHORT).show();
                        ((MainActivity) a).removeCurrentFragmentAndMoveBack();
                    }
                    @Override public void onFailure(Call<AllApiResponse> call, Throwable t) {}
                }));
        scroll.addView(root);
        return scroll;
    }
    @Override public void onStart() { super.onStart(); ((MainActivity) getActivity()).lockUnlockDrawer(1); }
}