package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Adapter.PortionMasterAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.PortionMasterResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.Utils.CatalogImportExportHelper;
import com.posbillingwala.owner.databinding.FragmentAddCustomerPortionMasterBinding;

import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, StaticFieldLeak")
public class AddCustomerPortionMaster extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static RecyclerView portionMasterRecyclerview;
    public static View portionMasterListCardView;
    public static View noDataFound;

    FragmentAddCustomerPortionMasterBinding binding;
    CatalogImportExportHelper catalogImportExportHelper;
    private String returnTo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddCustomerPortionMasterBinding.inflate(inflater, container, false);
        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            returnTo = bundle.getString("returnTo");
        }

        binding.portionMasterName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        portionMasterRecyclerview = binding.portionMasterRecyclerview;
        portionMasterListCardView = binding.portionMasterListCardView;
        noDataFound = binding.noDataFound;

        catalogImportExportHelper = new CatalogImportExportHelper(
                this, MainActivity.userId, "portions", "Portions", AddCustomerPortionMaster::getPortionMasterList);
        catalogImportExportHelper.bindBar(binding.catalogImportExportBar.getRoot());

        binding.backToHome.setOnClickListener(v -> navigateBack());
        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        binding.getRoot().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                navigateBack();
                return true;
            }
            return false;
        });

        binding.addPortionMaster.setOnClickListener(this);
        return binding.getRoot();
    }

    private void navigateBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        if ("product".equals(returnTo)) {
            ((MainActivity) activity).loadFragment(new AddCustomerProduct(), true);
        } else if ("updateProduct".equals(returnTo)) {
            // Caller should re-open update with args; fall back to product list
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        } else if ("managePortions".equals(returnTo)) {
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        } else {
            ((MainActivity) activity).loadFragment(new AddCustomerProductCategory(), true);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addPortionMaster) {
            if (binding.portionMasterName.getText().toString().trim().isEmpty()) {
                Toast.makeText(activity, "Please enter portion name", Toast.LENGTH_SHORT).show();
                return;
            }
            addPortionMaster();
        }
    }

    private void addPortionMaster() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().savePortionMaster(
                MainActivity.userId,
                binding.portionMasterName.getText().toString().trim(),
                "0",
                getRandomString(10));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && "1".equalsIgnoreCase(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    binding.portionMasterName.setText("");
                    getPortionMasterList();
                } else if (response.body() != null) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Log.e("portionMasterError", "" + t.getMessage());
            }
        });
    }

    public static void getPortionMasterList() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Call<AllApiResponse> call = Api.getClient().getPortionMasterList(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PortionMasterResponse> list = response.body().getPortionMasterResponseList();
                    if (list != null && !list.isEmpty()) {
                        PortionMasterAdapter adapter = new PortionMasterAdapter(activity, list);
                        portionMasterRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
                        portionMasterRecyclerview.setAdapter(adapter);
                        portionMasterListCardView.setVisibility(View.VISIBLE);
                        noDataFound.setVisibility(View.GONE);
                    } else {
                        portionMasterListCardView.setVisibility(View.GONE);
                        noDataFound.setVisibility(View.VISIBLE);
                    }
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
            }
        });
    }

    private String getRandomString(final int sizeOfRandomString) {
        String allowed = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
        return sb.toString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (catalogImportExportHelper != null) {
            catalogImportExportHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getPortionMasterList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
