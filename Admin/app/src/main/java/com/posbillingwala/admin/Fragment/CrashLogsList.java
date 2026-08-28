package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.CrashLogItem;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.IncludeReportKpiCardBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrashLogsList extends Fragment {
    Activity activity;
    LinearLayout list;
    IncludeReportKpiCardBinding kpi1, kpi2, kpi3;
    EditText search;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Crash Logs");
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 40);
        root.setBackgroundColor(Color.parseColor("#F7F9FC"));

        LinearLayout kpis = new LinearLayout(activity);
        kpis.setOrientation(LinearLayout.HORIZONTAL);
        kpi1 = IncludeReportKpiCardBinding.inflate(inflater, kpis, true);
        kpi2 = IncludeReportKpiCardBinding.inflate(inflater, kpis, true);
        root.addView(kpis);
        LinearLayout kpis2 = new LinearLayout(activity);
        kpis2.setOrientation(LinearLayout.HORIZONTAL);
        kpi3 = IncludeReportKpiCardBinding.inflate(inflater, kpis2, true);
        TextView analytics = new TextView(activity);
        analytics.setText("Analytics →");
        analytics.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary));
        analytics.setPadding(24, 24, 24, 24);
        analytics.setOnClickListener(v ->
                ((MainActivity) activity).navigateDetail(new CrashAnalytics(), "Crash Analytics"));
        kpis2.addView(analytics);
        root.addView(kpis2);

        search = new EditText(activity);
        search.setHint("Search crashes...");
        search.setBackgroundResource(R.drawable.bg_input);
        search.setPadding(28, 24, 28, 24);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = 16;
        search.setLayoutParams(slp);
        root.addView(search);

        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { load(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        scroll.addView(root);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean detail = getParentFragmentManager().getBackStackEntryCount() > 0;
        ((MainActivity) activity).lockUnlockDrawer(detail ? 1 : 0);
        if (!detail) {
            MainActivity.drawerLayout.closeDrawers();
        }
        if (DetectConnection.checkInternetConnection(activity)) load(search != null ? search.getText().toString() : "");
        else DetectConnection.noInternetConnection(activity);
    }

    private void load(String q) {
        Api.getClient().getCrashList(q, "all", "all").enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || list == null || response.body() == null) return;
                AllApiResponse b = response.body();
                setKpiWeight(kpi1);
                setKpiWeight(kpi2);
                setKpiWeight(kpi3);
                ReportUiHelper.bindKpi(kpi1, "Total Crashes", ReportUiHelper.nz(b.getTotalCrashes()), b.getTotalCrashesTrend());
                ReportUiHelper.bindKpi(kpi2, "Affected Users", ReportUiHelper.nz(b.getAffectedUsers()), b.getAffectedUsersTrend());
                ReportUiHelper.bindKpi(kpi3, "Resolved", ReportUiHelper.nz(b.getResolved()), b.getResolvedTrend());
                list.removeAllViews();
                List<CrashLogItem> crashes = b.getCrashes();
                if (crashes == null || crashes.isEmpty()) {
                    TextView empty = new TextView(activity);
                    empty.setText("No crash logs");
                    empty.setPadding(0, 32, 0, 32);
                    list.addView(empty);
                    return;
                }
                for (CrashLogItem c : crashes) {
                    TextView row = new TextView(activity);
                    row.setBackgroundResource(R.drawable.bg_card);
                    row.setPadding(28, 24, 28, 24);
                    row.setText(c.getErrorTitle() + "\n" + c.getErrorClass() + "  ·  " + c.getCreatedAt()
                            + "\n" + c.getAppName() + "  ·  " + c.getStatus());
                    row.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = 12;
                    row.setLayoutParams(lp);
                    row.setOnClickListener(v -> {
                        CrashLogDetails d = new CrashLogDetails();
                        Bundle args = new Bundle();
                        args.putString("crashId", c.getId());
                        d.setArguments(args);
                        ((MainActivity) activity).navigateDetail(d, "Crash Details");
                    });
                    list.addView(row);
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
            }
        });
    }

    private void setKpiWeight(IncludeReportKpiCardBinding card) {
        if (card == null) return;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(0, 0, 8, 0);
        card.getRoot().setLayoutParams(lp);
    }
}
