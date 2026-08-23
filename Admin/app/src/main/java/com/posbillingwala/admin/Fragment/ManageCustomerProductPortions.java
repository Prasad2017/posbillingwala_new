package com.posbillingwala.admin.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Adapter.PortionAdapter;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ProductPortionResponse;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n, NonConstantResourceId, StaticFieldLeak")
public class ManageCustomerProductPortions extends Fragment {

    public static Activity activity;
    public static RecyclerView portionRecyclerview;
    public static CardView portionListCardView;
    public static TextView noDataFound;

    View view;
    @BindView(R.id.productInfo)
    TextView productInfo;
    @BindView(R.id.portionName)
    TextInputEditText portionNameEdit;
    @BindView(R.id.portionPrice)
    TextInputEditText portionPriceEdit;
    @BindView(R.id.portionSortOrder)
    TextInputEditText portionSortOrderEdit;

    String customerId;
    String productId;
    String productName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_manage_customer_product_portions, container, false);
        ButterKnife.bind(this, view);

        activity = getActivity();
        MainActivity.title.setText("Product Portions");

        Bundle bundle = getArguments();
        if (bundle != null) {
            customerId = bundle.getString("customerId");
            productId = bundle.getString("productId");
            productName = bundle.getString("productName");
        }

        productInfo.setText("Product: " + productName);
        portionNameEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        portionRecyclerview = view.findViewById(R.id.portionRecyclerview);
        portionListCardView = view.findViewById(R.id.portionListCardView);
        noDataFound = view.findViewById(R.id.noDataFound);

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

        return view;
    }

    @OnClick(R.id.addPortion)
    public void onAddPortion() {
        if (portionNameEdit.getText().toString().trim().length() == 0) {
            Toast.makeText(activity, "Please enter portion name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (portionPriceEdit.getText().toString().trim().length() == 0) {
            Toast.makeText(activity, "Please enter portion price", Toast.LENGTH_SHORT).show();
            return;
        }
        addPortion();
    }

    private void addPortion() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        String sortOrder = portionSortOrderEdit.getText().toString().trim();
        if (sortOrder.length() == 0) {
            sortOrder = "0";
        }

        Call<AllApiResponse> call = Api.getClient().savePortion(
                customerId,
                productId,
                portionNameEdit.getText().toString().trim(),
                portionPriceEdit.getText().toString().trim(),
                sortOrder,
                getRandomString(10));
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && "1".equalsIgnoreCase(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    portionNameEdit.setText("");
                    portionPriceEdit.setText("");
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
                        portionSortOrderEdit.setText(String.valueOf(list.size()));
                    } else {
                        portionListCardView.setVisibility(View.GONE);
                        noDataFound.setVisibility(View.VISIBLE);
                        portionSortOrderEdit.setText("0");
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
            getPortionList();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }
}
