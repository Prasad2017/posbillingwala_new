package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.SupportUiHelper;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.SupportTicketItem;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MySupportTickets extends Fragment {
    LinearLayout list;
    LinearLayout ticketListContainer;
    Activity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        list = SupportUiHelper.form(activity);
        SupportUiHelper.addScreenHeader(activity, list, getString(R.string.support_my_tickets),
                () -> ((MainActivity) activity).navigateBack());
        SupportUiHelper.notice(activity, list, getString(R.string.support_online_only_notice));
        SupportUiHelper.primary(activity, list, getString(R.string.support_refresh))
                .setOnClickListener(v -> loadTickets());
        ticketListContainer = SupportUiHelper.createTicketListContainer(activity);
        list.addView(ticketListContainer);
        return SupportUiHelper.wrapScreen(activity, list);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        loadTickets();
    }

    private void loadTickets() {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        SupportUiHelper.clearTicketList(ticketListContainer);
        TextView loading = new TextView(activity);
        loading.setText(R.string.support_loading);
        ticketListContainer.addView(loading);

        Api.getClient(activity).getSupportTickets(MainActivity.userId, "all").enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (!isAdded() || ticketListContainer == null) return;
                SupportUiHelper.clearTicketList(ticketListContainer);
                List<SupportTicketItem> tickets = response.body() != null ? response.body().getTickets() : null;
                if (tickets == null || tickets.isEmpty()) {
                    TextView empty = new TextView(activity);
                    empty.setText(R.string.support_no_tickets);
                    ticketListContainer.addView(empty);
                    return;
                }
                List<TextView> cards = new ArrayList<>(tickets.size());
                for (SupportTicketItem t : tickets) {
                    String status = t.getStatus() != null ? t.getStatus() : "";
                    TextView tv = SupportUiHelper.buildTicketCard(
                            activity,
                            t.getTicketNo(),
                            status,
                            t.getSubject(),
                            nz(t.getCreatedAt()),
                            () -> openDetails(t.getId()));
                    cards.add(tv);
                }
                SupportUiHelper.populateTicketGrid(activity, ticketListContainer, cards);
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDetails(String ticketId) {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        SupportTicketDetails d = new SupportTicketDetails();
        Bundle b = new Bundle();
        b.putString("ticketId", ticketId);
        d.setArguments(b);
        ((MainActivity) activity).loadFragment(d, true);
    }

    private static String nz(String v) {
        return v == null || v.isEmpty() ? "—" : v;
    }
}
