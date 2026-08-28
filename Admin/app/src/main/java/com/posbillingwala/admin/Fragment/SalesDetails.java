package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
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
import com.posbillingwala.admin.Model.ReportRankItem;
import com.posbillingwala.admin.R;
import com.posbillingwala.admin.Retrofit.Api;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesDetails extends Fragment {
    Activity activity;
    String invoiceId;
    LinearLayout root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        ((MainActivity) activity).setScreenTitle("Sales Details");
        if (getArguments() != null) invoiceId = getArguments().getString("invoiceId");
        ScrollView scroll = new ScrollView(activity);
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 32);
        root.setBackgroundColor(Color.parseColor("#F7F9FC"));
        scroll.addView(root);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        if (DetectConnection.checkInternetConnection(activity)) load();
        else DetectConnection.noInternetConnection(activity);
    }

    private void load() {
        SweetAlertDialog p = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        p.getProgressHelper().setBarColor(Color.parseColor("#2563EB"));
        p.setTitleText("Loading");
        p.setCancelable(false);
        p.show();
        Api.getClient().getInvoiceDetails(invoiceId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                p.dismiss();
                if (!isAdded() || root == null || response.body() == null) return;
                AllApiResponse b = response.body();
                root.removeAllViews();
                addLine("Invoice No", ReportUiHelper.nz(b.getInvoiceNumber()) + "  ·  " + ReportUiHelper.nz(b.getPaymentStatus()));
                addLine("Date", ReportUiHelper.nz(b.getInvoiceDate()));
                addLine("Customer", ReportUiHelper.nz(b.getDetailCustomerName() != null ? b.getDetailCustomerName() : b.getShopName()));
                addLine("Payment", ReportUiHelper.nz(b.getPaymentMethod()));
                addLine("Cashier", ReportUiHelper.nz(b.getCashierName()));
                TextView itemsTitle = new TextView(activity);
                itemsTitle.setText("Items");
                itemsTitle.setPadding(0, 24, 0, 12);
                itemsTitle.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
                root.addView(itemsTitle);
                List<ReportRankItem> items = b.getInvoiceItems();
                if (items == null || items.isEmpty()) {
                    TextView empty = new TextView(activity);
                    empty.setText("No line items available");
                    empty.setTextColor(ContextCompat.getColor(activity, R.color.colorTextHint));
                    root.addView(empty);
                } else {
                    for (ReportRankItem it : items) {
                        addLine(it.displayName(), it.displayValue());
                    }
                }
                addLine("Subtotal", ReportUiHelper.money(b.getSubtotal()));
                addLine("Tax", ReportUiHelper.money(b.getTax()));
                addLine("Total", ReportUiHelper.money(b.getTotalAmount()));
                addLine("Paid", ReportUiHelper.money(b.getPaidAmount()));
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                p.dismiss();
            }
        });
    }

    private void addLine(String label, String value) {
        TextView tv = new TextView(activity);
        tv.setText(label + ": " + value);
        tv.setPadding(0, 8, 0, 8);
        tv.setTextColor(ContextCompat.getColor(activity, R.color.colorTextPrimary));
        root.addView(tv);
    }
}
