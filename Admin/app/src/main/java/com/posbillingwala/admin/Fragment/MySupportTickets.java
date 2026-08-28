package com.posbillingwala.admin.Fragment;

import android.app.Activity;
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
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Extra.ReportUiHelper;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.SupportTicketItem;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MySupportTickets extends Fragment {
    LinearLayout list;
    Activity a;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        a = getActivity();
        ((MainActivity) a).setScreenTitle("My Tickets");
        ScrollView scroll = new ScrollView(a);
        list = SettingsProfile.form(a);
        scroll.addView(list);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) a).lockUnlockDrawer(1);
        if (!DetectConnection.checkInternetConnection(a)) {
            DetectConnection.noInternetConnection(a);
            return;
        }
        Api.getClient().getSupportTickets("all").enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || list == null) return;
                list.removeAllViews();
                List<SupportTicketItem> tickets = response.body() != null ? response.body().getTickets() : null;
                if (tickets == null || tickets.isEmpty()) {
                    TextView empty = new TextView(a);
                    empty.setText("No tickets yet");
                    list.addView(empty);
                    return;
                }
                for (SupportTicketItem t : tickets) {
                    TextView tv = new TextView(a);
                    tv.setBackgroundResource(R.drawable.bg_card);
                    tv.setPadding(28, 24, 28, 24);
                    tv.setText(t.getTicketNo() + "  ·  " + t.getStatus() + "\n" + t.getSubject()
                            + (t.getShopName() != null && !t.getShopName().isEmpty() ? "\n" + t.getShopName() : "")
                            + "\n" + t.getAppName() + "  ·  " + ReportUiHelper.nz(t.getCreatedAt()));
                    tv.setTextColor(ContextCompat.getColor(a, R.color.colorTextPrimary));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = 12;
                    tv.setLayoutParams(lp);
                    tv.setOnClickListener(v -> {
                        SupportTicketDetails d = new SupportTicketDetails();
                        Bundle b = new Bundle();
                        b.putString("ticketId", t.getId());
                        d.setArguments(b);
                        ((MainActivity) a).navigateDetail(d, "Ticket Details");
                    });
                    list.addView(tv);
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
            }
        });
    }
}
