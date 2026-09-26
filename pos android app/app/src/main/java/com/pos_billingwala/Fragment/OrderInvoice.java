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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.pos_billingwala.Extra.EmptyListUi;


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

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || pageNumber >= totalPages || adapter == null) {
                    return;
                }
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (!(layoutManager instanceof LinearLayoutManager)) {
                    return;
                }
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                int lastVisible = linearLayoutManager.findLastVisibleItemPosition();
                if (lastVisible >= linearLayoutManager.getItemCount() - 2) {
                    new LoadMoreInvoices().execute();
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
                    // AutoFitGridRecyclerView sets span count from measured width.
                    binding.recyclerView.setAdapter(adapter);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    EmptyListUi.bind(binding.noDataFound, true, R.string.empty_sub_invoices);
                    pageNumber = page.size();
                } else {
                    binding.recyclerView.setVisibility(View.GONE);
                    EmptyListUi.bind(binding.noDataFound, false, R.string.empty_sub_invoices);
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
                int previousLast = invoiceResponseList.size() - 1;
                int start = invoiceResponseList.size();
                invoiceResponseList.addAll(page);
                adapter.notifyItemRangeInserted(start, page.size());
                if (previousLast >= 0) {
                    adapter.notifyItemChanged(previousLast);
                }
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
