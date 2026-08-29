package com.posbillingwala.dealer.Fragment;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Adapter.PortionMasterAdapter;
import com.posbillingwala.dealer.Extra.DetectConnection;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.PortionMasterResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;
import com.posbillingwala.dealer.Utils.CatalogImportExportHelper;
import com.posbillingwala.dealer.databinding.FragmentAddCustomerPortionMasterBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, StaticFieldLeak")
public class AddCustomerPortionMaster extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static String customerId;
    public static RecyclerView portionMasterRecyclerview;
    public static CardView portionMasterListCardView;
    public static TextView noDataFound;

    View view;
    FragmentAddCustomerPortionMasterBinding binding;

    String returnTo;
    String productId;
    String productName;
    CatalogImportExportHelper catalogImportExportHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddCustomerPortionMasterBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Portion Master");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
            returnTo = bundle.getString("returnTo");
            productId = bundle.getString("productId");
            productName = bundle.getString("productName");
        }

        if (customerId != null) {
            catalogImportExportHelper = new CatalogImportExportHelper(
                    this, customerId, "portions", "Portions", AddCustomerPortionMaster::getPortionMasterList);
            catalogImportExportHelper.bindBar(binding.catalogImportExportBar.getRoot());
        }

        binding.portionMasterName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        portionMasterRecyclerview = binding.portionMasterRecyclerview;
        portionMasterListCardView = binding.portionMasterListCardView;
        noDataFound = binding.noDataFound;

        MainActivity.back.setOnClickListener(v -> navigateBack());

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                MainActivity.back.performClick();
                return true;
            }
            return false;
        });

        binding.addPortionMaster.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (catalogImportExportHelper != null) {
            catalogImportExportHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void navigateBack() {
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        if ("productPortions".equals(returnTo)) {
            ManageCustomerProductPortions fragment = new ManageCustomerProductPortions();
            Bundle backBundle = new Bundle();
            backBundle.putString("customerId", customerId);
            backBundle.putString("productId", productId);
            backBundle.putString("productName", productName);
            fragment.setArguments(backBundle);
            ((MainActivity) activity).loadFragment(fragment, true);
        } else if ("addProduct".equals(returnTo)) {
            AddCustomerProduct fragment = new AddCustomerProduct();
            Bundle backBundle = new Bundle();
            backBundle.putString("customerId", customerId);
            fragment.setArguments(backBundle);
            ((MainActivity) activity).loadFragment(fragment, true);
        } else if ("updateProduct".equals(returnTo)) {
            UpdateProduct fragment = new UpdateProduct();
            Bundle backBundle = new Bundle();
            backBundle.putString("customerId", customerId);
            backBundle.putString("productId", productId);
            fragment.setArguments(backBundle);
            ((MainActivity) activity).loadFragment(fragment, true);
        } else {
            AddCustomerProductCategory fragment = new AddCustomerProductCategory();
            Bundle backBundle = new Bundle();
            backBundle.putString("customerId", customerId);
            fragment.setArguments(backBundle);
            ((MainActivity) activity).loadFragment(fragment, true);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addPortionMaster) {
            if (binding.portionMasterName.getText().toString().trim().length() == 0) {
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
                customerId,
                binding.portionMasterName.getText().toString().trim(),
                getRandomString(10),
                "0");
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
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
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
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

        Call<AllApiResponse> call = Api.getClient().getPortionMasterList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PortionMasterResponse> list = response.body().getPortionMasterResponseList();
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    if (!list.isEmpty()) {
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
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("portionMasterList", "" + t.getMessage());
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
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(1);
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            getPortionMasterList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
