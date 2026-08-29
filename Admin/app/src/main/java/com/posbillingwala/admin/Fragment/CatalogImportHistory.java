package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.CatalogImportHistoryAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.CatalogImportHistoryItem;
import com.posbillingwala.admin.Model.CatalogImportHistoryResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCatalogImportHistoryBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n")
public class CatalogImportHistory extends Fragment {

    private Activity activity;
    private FragmentCatalogImportHistoryBinding binding;
    private String customerId;
    private String importTypeFilter;
    private String typeLabel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCatalogImportHistoryBinding.inflate(inflater, container, false);
        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
            importTypeFilter = bundle.getString("importType");
            typeLabel = bundle.getString("typeLabel", "Catalog");
        }

        MainActivity.title.setText(typeLabel + " Import History");
        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadHistory();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadHistory() {
        binding.historyProgressBar.setVisibility(View.VISIBLE);
        binding.historyRecyclerView.setVisibility(View.GONE);
        binding.noHistoryFound.setVisibility(View.GONE);

        Api.getClient().catalogImportHistory(customerId, importTypeFilter).enqueue(new Callback<CatalogImportHistoryResponse>() {
            @Override
            public void onResponse(Call<CatalogImportHistoryResponse> call, Response<CatalogImportHistoryResponse> response) {
                binding.historyProgressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    binding.noHistoryFound.setVisibility(View.VISIBLE);
                    return;
                }
                List<CatalogImportHistoryItem> all = response.body().getHistory();
                List<CatalogImportHistoryItem> filtered = new ArrayList<>();
                if (all != null) {
                    for (CatalogImportHistoryItem item : all) {
                        if (importTypeFilter == null || importTypeFilter.equals(item.getImportType())) {
                            filtered.add(item);
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    binding.noHistoryFound.setVisibility(View.VISIBLE);
                } else {
                    binding.historyRecyclerView.setVisibility(View.VISIBLE);
                    binding.historyRecyclerView.setLayoutManager(new GridLayoutManager(activity, 1));
                    binding.historyRecyclerView.setAdapter(new CatalogImportHistoryAdapter(activity, filtered));
                }
            }

            @Override
            public void onFailure(Call<CatalogImportHistoryResponse> call, Throwable t) {
                binding.historyProgressBar.setVisibility(View.GONE);
                binding.noHistoryFound.setVisibility(View.VISIBLE);
                Toast.makeText(activity, "Unable to load history.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
