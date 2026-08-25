package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.ComboAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ComboResponse;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCustomerCombosBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerCombos extends Fragment {

    Activity activity;
    FragmentCustomerCombosBinding binding;
    String customerId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCustomerCombosBinding.inflate(inflater, container, false);
        activity = getActivity();
        MainActivity.title.setText("Combos");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
        }

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            CustomerDetails details = new CustomerDetails();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            details.setArguments(b);
            ((MainActivity) activity).loadFragment(details, true);
        });

        binding.addCombo.setOnClickListener(v -> {
            String name = binding.comboName.getText() != null ? binding.comboName.getText().toString().trim() : "";
            String price = binding.comboPrice.getText() != null ? binding.comboPrice.getText().toString().trim() : "";
            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(activity, "Enter combo name and price", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!DetectConnection.checkInternetConnection(activity)) {
                DetectConnection.noInternetConnection(activity);
                return;
            }
            saveCombo(name, price);
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadCombos();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadCombos() {
        Call<AllApiResponse> call = Api.getClient().getCustomerComboList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                List<ComboResponse> list = response.body() != null ? response.body().getComboResponseList() : null;
                if (list == null) {
                    list = new ArrayList<>();
                }
                binding.emptyCombos.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                binding.recyclerView.setAdapter(new ComboAdapter(list));
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                binding.emptyCombos.setVisibility(View.VISIBLE);
            }
        });
    }

    private void saveCombo(String name, String price) {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Saving");
        pDialog.setCancelable(false);
        pDialog.show();

        String networkKey = "ADM" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Call<AllApiResponse> call = Api.getClient().insertCustomerCombo(
                customerId, name, price, networkKey, "", "0", "0", "1", "0");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && "1".equals(response.body().getStatus())) {
                    Toast.makeText(activity, "Combo saved", Toast.LENGTH_SHORT).show();
                    binding.comboName.setText("");
                    binding.comboPrice.setText("");
                    loadCombos();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Save failed";
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "Unable to save combo", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
