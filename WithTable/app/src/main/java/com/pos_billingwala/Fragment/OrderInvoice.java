package com.pos_billingwala.Fragment;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.InvoiceAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentOrderInvoiceBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class OrderInvoice extends Fragment implements View.OnClickListener {

    public static Activity activity;
    static int pageNumber = 0, totalPages, limit = 25;
    View view;
    List<InvoiceResponse> invoiceResponseList = new ArrayList<>();
    POSBillingWalaDatabase posBillingWalaDatabase;
    InvoiceAdapter adapter;
    boolean isLoading = false, isDateMonthWise = false;
    FragmentOrderInvoiceBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrderInvoiceBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);


        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

        binding.nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    if (!isLoading && pageNumber < totalPages) {
                        new LoadMoreInvoices().execute();
                    }
                }
            }
        });

        binding.backToSetting.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getInvoiceList();
    }

    public void getInvoiceList() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        pageNumber = 0;
        new LoadInitialInvoices().execute();
    }

    private void removeLoadingFooter() {
        if (!invoiceResponseList.isEmpty()
                && invoiceResponseList.get(invoiceResponseList.size() - 1) == null) {
            int idx = invoiceResponseList.size() - 1;
            invoiceResponseList.remove(idx);
            if (adapter != null) {
                adapter.notifyItemRemoved(idx);
            }
        }
    }

    private class LoadInitialInvoices extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        private int count;
        private SweetAlertDialog loader;

        @Override
        protected void onPreExecute() {
            loader = ListLoader.show(activity);
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            count = posBillingWalaDatabase.getInvoiceCount("");
            if (count <= 0) {
                return new ArrayList<>();
            }
            return posBillingWalaDatabase.getInvoiceList("", 0);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            try {
                if (!isAdded()) {
                    isLoading = false;
                    return;
                }
                totalPages = count;
                invoiceResponseList.clear();
                if (page != null && !page.isEmpty()) {
                    invoiceResponseList.addAll(page);
                    adapter = new InvoiceAdapter(activity, invoiceResponseList);
                    binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    binding.recyclerView.setAdapter(adapter);
                    binding.nestedScrollView.setVisibility(View.VISIBLE);
                    binding.noDataFound.setVisibility(View.GONE);
                    pageNumber = page.size();
                } else {
                    binding.nestedScrollView.setVisibility(View.GONE);
                    binding.noDataFound.setVisibility(View.VISIBLE);
                    pageNumber = 0;
                }
                isLoading = false;
            } finally {
                ListLoader.dismiss(loader);
            }
        }
    }

    /** Loads exactly one more page on scroll — never chains all pages. */
    private class LoadMoreInvoices extends AsyncTask<Void, Void, List<InvoiceResponse>> {
        @Override
        protected void onPreExecute() {
            if (isLoading || pageNumber >= totalPages || adapter == null) {
                cancel(true);
                return;
            }
            isLoading = true;
            invoiceResponseList.add(null);
            adapter.notifyItemInserted(invoiceResponseList.size() - 1);
        }

        @Override
        protected List<InvoiceResponse> doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }
            return posBillingWalaDatabase.getInvoiceList("", pageNumber);
        }

        @Override
        protected void onPostExecute(List<InvoiceResponse> page) {
            if (!isAdded()) {
                isLoading = false;
                return;
            }
            removeLoadingFooter();
            if (page != null && !page.isEmpty()) {
                int start = invoiceResponseList.size();
                invoiceResponseList.addAll(page);
                adapter.notifyItemRangeInserted(start, page.size());
                pageNumber += page.size();
            }
            isLoading = false;
        }

        @Override
        protected void onCancelled() {
            isLoading = false;
        }
    }

}
