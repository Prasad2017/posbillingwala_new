package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.SupportTicketAdapter;
import com.pos_billingwala.Extra.ActionButtonUi;
import com.pos_billingwala.Extra.DetectConnection;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.SupportTicketItem;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.databinding.FragmentMySupportTicketsBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MySupportTickets extends Fragment implements SupportTicketAdapter.Listener {
    Activity activity;
    FragmentMySupportTicketsBinding binding;
    SupportTicketAdapter adapter;
    String statusFilter = "all";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        binding = FragmentMySupportTicketsBinding.inflate(inflater, container, false);

        binding.backButton.setOnClickListener(v -> ((MainActivity) activity).navigateBack());
        binding.refreshTicketsButton.getRoot().setOnClickListener(v -> loadTickets());
        ActionButtonUi.bind(binding.refreshTicketsButton.getRoot(), R.drawable.ic_refresh, R.string.support_refresh_tickets);
        binding.getRoot().findViewById(R.id.supportCallButton).setOnClickListener(v -> dialSupport());

        ((android.widget.TextView) binding.getRoot().findViewById(R.id.noticeTitle))
                .setText(R.string.support_stay_connected);

        adapter = new SupportTicketAdapter(this);
        binding.ticketRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.ticketRecyclerView.setAdapter(adapter);

        setupStatusFilter();

        TabletFormUi.applyCenteredPanel(binding.supportContent);
        return binding.getRoot();
    }

    private void setupStatusFilter() {
        String[] labels = getResources().getStringArray(R.array.support_status_filters);
        binding.statusFilter.setItems(labels);
        binding.statusFilter.setOnItemSelectedListener((position, label) -> {
            statusFilter = statusFilterValue(position);
            loadTickets();
        });
    }

    private String statusFilterValue(int position) {
        if (position == 1) {
            return "open";
        }
        if (position == 2) {
            return "closed";
        }
        return "all";
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
        showLoading(true);
        Api.getClient(activity).getSupportTickets(MainActivity.userId, statusFilter)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        if (!isAdded()) {
                            return;
                        }
                        showLoading(false);
                        List<SupportTicketItem> tickets = response.body() != null
                                ? response.body().getTickets() : null;
                        int count = tickets != null ? tickets.size() : 0;
                        binding.ticketsHeading.setText(getString(R.string.support_your_tickets)
                                + " (" + count + ")");
                        if (tickets == null || tickets.isEmpty()) {
                            adapter.setTickets(null);
                            binding.emptyState.setVisibility(View.VISIBLE);
                            binding.ticketRecyclerView.setVisibility(View.GONE);
                            return;
                        }
                        binding.emptyState.setVisibility(View.GONE);
                        binding.ticketRecyclerView.setVisibility(View.VISIBLE);
                        adapter.setTickets(tickets);
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                        if (!isAdded()) {
                            return;
                        }
                        showLoading(false);
                        Toast.makeText(activity, R.string.support_load_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean loading) {
        binding.loadingState.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTicketClick(SupportTicketItem ticket) {
        if (!DetectConnection.checkInternetConnection(activity)) {
            DetectConnection.noInternetConnection(activity);
            return;
        }
        SupportTicketDetails details = new SupportTicketDetails();
        Bundle bundle = new Bundle();
        bundle.putString("ticketId", ticket.getId());
        details.setArguments(bundle);
        ((MainActivity) activity).loadFragment(details, true);
    }

    private void dialSupport() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + getString(R.string.support_phone_dial)));
        startActivity(intent);
    }
}
