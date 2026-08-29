package com.posbillingwala.owner.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
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
import com.posbillingwala.owner.Adapter.PortionAdapter;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.PortionMasterResponse;
import com.posbillingwala.owner.Model.ProductPortionResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentManageCustomerProductPortionsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, StaticFieldLeak")
public class ManageCustomerProductPortions extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static RecyclerView portionRecyclerview;
    public static View portionListCardView;
    public static View noDataFound;

    FragmentManageCustomerProductPortionsBinding binding;
    public static Runnable refreshPortions;

    String productId;
    String productName;
    String[] portionMasterIdList;
    String[] portionMasterNameList;
    String selectedPortionMasterId;
    String selectedPortionName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageCustomerProductPortionsBinding.inflate(inflater, container, false);

        activity = getActivity();

        Bundle bundle = getArguments();
        if (bundle != null) {
            productId = bundle.getString("productId");
            productName = bundle.getString("productName");
        }

        binding.productInfo.setText("Product: " + productName);
        portionRecyclerview = binding.portionRecyclerview;
        portionListCardView = binding.portionListCardView;
        noDataFound = binding.noDataFound;

        refreshPortions = this::getPortionList;

        binding.backToHome.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            ((MainActivity) activity).loadFragment(new AllCustomerProductList(), true);
        });

        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        binding.getRoot().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                binding.backToHome.performClick();
                return true;
            }
            return false;
        });

        binding.portionMasterSpinner.setOnItemSelectedListener((position, item) -> {
            if (portionMasterIdList != null && position >= 0 && position < portionMasterIdList.length) {
                selectedPortionMasterId = portionMasterIdList[position];
                selectedPortionName = portionMasterNameList[position];
            }
        });

        binding.managePortionMasterLink.setOnClickListener(v -> {
            AddCustomerPortionMaster fragment = new AddCustomerPortionMaster();
            Bundle args = new Bundle();
            args.putString("returnTo", "managePortions");
            fragment.setArguments(args);
            ((MainActivity) activity).loadFragment(fragment, true);
        });

        binding.addPortion.setOnClickListener(this);

        return binding.getRoot();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addPortion) {
            if (selectedPortionMasterId == null || selectedPortionMasterId.isEmpty()) {
                Toast.makeText(activity, "Please select portion master", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.portionPrice.getText().toString().trim().length() == 0) {
                Toast.makeText(activity, "Please enter portion price", Toast.LENGTH_SHORT).show();
                return;
            }
            addPortion();
        }
    }

    private void addPortion() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String sortOrder = binding.portionSortOrder.getText().toString().trim();
        if (sortOrder.length() == 0) {
            sortOrder = "0";
        }

        Call<AllApiResponse> call = Api.getClient().savePortion(
                MainActivity.userId,
                productId,
                selectedPortionMasterId,
                selectedPortionName != null ? selectedPortionName : "",
                binding.portionPrice.getText().toString().trim(),
                sortOrder,
                getRandomString(10));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && "1".equalsIgnoreCase(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    binding.portionPrice.setText("");
                    getPortionList();
                } else if (response.body() != null) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                pDialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                Log.e("portionError", "" + t.getMessage());
            }
        });
    }

    private void getPortionList() {
        Call<AllApiResponse> call = Api.getClient().getPortionList(MainActivity.userId, productId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductPortionResponse> list = response.body().getPortionResponseList();
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    if (!list.isEmpty()) {
                        PortionAdapter adapter = new PortionAdapter(activity, list);
                        portionRecyclerview.setLayoutManager(new GridLayoutManager(activity, 1));
                        portionRecyclerview.setAdapter(adapter);
                        portionListCardView.setVisibility(View.VISIBLE);
                        noDataFound.setVisibility(View.GONE);
                        binding.portionSortOrder.setText(String.valueOf(list.size()));
                    } else {
                        portionListCardView.setVisibility(View.GONE);
                        noDataFound.setVisibility(View.VISIBLE);
                        binding.portionSortOrder.setText("0");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Log.e("portionListError", "" + t.getMessage());
            }
        });
    }

    private void loadPortionMasters() {
        Call<AllApiResponse> call = Api.getClient().getPortionMasterList(MainActivity.userId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PortionMasterResponse> list = response.body().getPortionMasterResponseList();
                    if (list != null && !list.isEmpty()) {
                        portionMasterIdList = new String[list.size()];
                        portionMasterNameList = new String[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            portionMasterIdList[i] = list.get(i).getPortionMasterId();
                            portionMasterNameList[i] = list.get(i).getPortionName();
                        }
                        binding.portionMasterSpinner.setItems(portionMasterNameList);
                        binding.portionMasterSpinner.setSelectedIndex(0);
                        selectedPortionMasterId = portionMasterIdList[0];
                        selectedPortionName = portionMasterNameList[0];
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
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
        MainActivity.drawerLayout.closeDrawers();
        if (DetectConnection.checkInternetConnection(activity)) {
            loadPortionMasters();
            getPortionList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
