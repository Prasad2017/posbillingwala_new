package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.PortionAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.PortionMasterResponse;
import com.posbillingwala.admin.Model.ProductPortionResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentManageCustomerProductPortionsBinding;

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
    public static CardView portionListCardView;
    public static TextView noDataFound;

    View view;
    FragmentManageCustomerProductPortionsBinding binding;

    String customerId;
    String productId;
    String productName;
    String[] portionMasterIdList;
    String[] portionMasterNameList;
    String selectedPortionMasterId;
    String selectedPortionMasterName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageCustomerProductPortionsBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        MainActivity.title.setText("Product Portions");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
            productId = bundle.getString("productId");
            productName = bundle.getString("productName");
        }

        binding.productInfo.setText("Product: " + productName);
        portionRecyclerview = binding.portionRecyclerview;
        portionListCardView = binding.portionListCardView;
        noDataFound = binding.noDataFound;

        binding.portionMasterSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                if (portionMasterIdList != null && position >= 0 && position < portionMasterIdList.length) {
                    selectedPortionMasterId = portionMasterIdList[position];
                    selectedPortionMasterName = portionMasterNameList[position];
                }
            }
        });

        MainActivity.back.setOnClickListener(v -> {
            ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
            AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
            Bundle backBundle = new Bundle();
            backBundle.putString("customerId", customerId);
            allCustomerProductList.setArguments(backBundle);
            ((MainActivity) activity).loadFragment(allCustomerProductList, true);
        });

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                MainActivity.back.performClick();
                return true;
            }
            return false;
        });

        binding.addPortion.setOnClickListener(this);
        binding.managePortionMaster.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addPortion) {
            if (selectedPortionMasterId == null || selectedPortionMasterId.isEmpty()) {
                Toast.makeText(activity, "Please select a portion master", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.portionPrice.getText().toString().trim().length() == 0) {
                Toast.makeText(activity, "Please enter portion price", Toast.LENGTH_SHORT).show();
                return;
            }
            addPortion();
        } else if (view.getId() == R.id.managePortionMaster) {
            openPortionMaster();
        }
    }

    private void openPortionMaster() {
        AddCustomerPortionMaster fragment = new AddCustomerPortionMaster();
        Bundle bundle = new Bundle();
        bundle.putString("customerId", customerId);
        bundle.putString("returnTo", "productPortions");
        bundle.putString("productId", productId);
        bundle.putString("productName", productName);
        fragment.setArguments(bundle);
        ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
        ((MainActivity) activity).loadFragment(fragment, true);
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
                customerId,
                productId,
                selectedPortionMasterName,
                binding.portionPrice.getText().toString().trim(),
                sortOrder,
                getRandomString(10),
                selectedPortionMasterId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
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
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Log.e("portionError", "" + t.getMessage());
            }
        });
    }

    private void loadPortionMasters() {
        Call<AllApiResponse> call = Api.getClient().getPortionMasterList(customerId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PortionMasterResponse> list = response.body().getPortionMasterResponse();
                    if (list != null && !list.isEmpty()) {
                        binding.portionMasterSection.setVisibility(View.VISIBLE);
                        portionMasterIdList = new String[list.size()];
                        portionMasterNameList = new String[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            portionMasterIdList[i] = list.get(i).getPortionMasterId();
                            portionMasterNameList[i] = list.get(i).getPortionName();
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, portionMasterNameList);
                        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                        binding.portionMasterSpinner.setAdapter(adapter);
                        selectedPortionMasterId = portionMasterIdList[0];
                        selectedPortionMasterName = portionMasterNameList[0];
                    } else {
                        binding.portionMasterSection.setVisibility(View.GONE);
                        selectedPortionMasterId = null;
                        selectedPortionMasterName = null;
                        Toast.makeText(activity, "Please add portion masters first", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("portionMasterError", "" + t.getMessage());
            }
        });
    }

    private void getPortionList() {
        Call<AllApiResponse> call = Api.getClient().getPortionList(customerId, productId);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
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
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.e("portionListError", "" + t.getMessage());
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
            loadPortionMasters();
            getPortionList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
