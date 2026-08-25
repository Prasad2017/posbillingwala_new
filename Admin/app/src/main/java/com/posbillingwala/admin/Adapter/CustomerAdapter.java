package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Fragment.AddCustomerProduct;
import com.posbillingwala.admin.Fragment.AddCustomerProductCategory;
import com.posbillingwala.admin.Fragment.AddCustomerSubcategory;
import com.posbillingwala.admin.Fragment.AllCustomerProductList;
import com.posbillingwala.admin.Fragment.CustomerDetails;
import com.posbillingwala.admin.Fragment.NewLicenceRegistration;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.databinding.CustomerListBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.MyViewHolder> implements Filterable {

    Context context;
    List<CustomerResponse> customerResponseList;
    List<CustomerResponse> customerResponseListFull;

    public CustomerAdapter(Context context, List<CustomerResponse> customerResponseList) {
        this.context = context;
        this.customerResponseList = customerResponseList != null ? customerResponseList : new ArrayList<>();
        this.customerResponseListFull = new ArrayList<>(this.customerResponseList);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CustomerListBinding binding = CustomerListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        CustomerResponse customerResponse = customerResponseList.get(position);

        String customerName = "<b>Owner: </b>" + customerResponse.getName();
        holder.binding.customerName.setText(Html.fromHtml(customerName));

        String customerNumber = "<b>Mobile: </b>" + customerResponse.getContactNumber();
        holder.binding.customerNumber.setText(Html.fromHtml(customerNumber));

        String customerAddress = "<b>Address: </b>" + customerResponse.getAddress();
        holder.binding.customerAddress.setText(Html.fromHtml(customerAddress));

        String shopName = "<b>Restaurant: </b>" + customerResponse.getShopName();
        holder.binding.customerShopName.setText(Html.fromHtml(shopName));

        holder.binding.category.setOnClickListener(v -> {
            AddCustomerProductCategory addCustomerProductCategory = new AddCustomerProductCategory();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", "" + customerResponse.getId());
            addCustomerProductCategory.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(addCustomerProductCategory, true);
        });

        holder.binding.subcategory.setOnClickListener(v -> {
            AddCustomerSubcategory addCustomerSubcategory = new AddCustomerSubcategory();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", "" + customerResponse.getId());
            addCustomerSubcategory.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(addCustomerSubcategory, true);
        });

        holder.binding.product.setOnClickListener(v -> {
            AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", "" + customerResponse.getId());
            allCustomerProductList.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(allCustomerProductList, true);
        });

        holder.binding.editCustomer.setOnClickListener(v -> {
            CustomerDetails customerDetails = new CustomerDetails();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", "" + customerResponse.getId());
            customerDetails.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(customerDetails, true);
        });

        holder.binding.addProduct.setOnClickListener(v -> {
            AddCustomerProduct addCustomerProduct = new AddCustomerProduct();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", "" + customerResponse.getId());
            addCustomerProduct.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(addCustomerProduct, true);
        });

        holder.binding.newLicenceRegistration.setOnClickListener(v -> {
            NewLicenceRegistration newLicenceRegistration = new NewLicenceRegistration();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", "" + customerResponse.getId());
            newLicenceRegistration.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(newLicenceRegistration, true);
        });
    }

    @Override
    public int getItemCount() {
        return customerResponseList.size();
    }

    @Override
    public Filter getFilter() {
        return customerFilter;
    }

    private final Filter customerFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CustomerResponse> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(customerResponseListFull);
            } else {
                String q = constraint.toString().toLowerCase(Locale.US).trim();
                for (CustomerResponse c : customerResponseListFull) {
                    if (matches(c, q)) {
                        filtered.add(c);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filtered;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            customerResponseList.clear();
            if (results.values != null) {
                //noinspection unchecked
                customerResponseList.addAll((List<CustomerResponse>) results.values);
            }
            notifyDataSetChanged();
        }
    };

    private boolean matches(CustomerResponse c, String q) {
        if (contains(c.getId(), q) || contains(c.getName(), q) || contains(c.getShopName(), q)
                || contains(c.getContactNumber(), q) || contains(c.getEmail(), q)) {
            return true;
        }
        if (c.getLicenseResponseList() != null) {
            for (LicenseResponse lic : c.getLicenseResponseList()) {
                if (contains(lic.getLicenseKey(), q)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.US).contains(q);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final CustomerListBinding binding;

        public MyViewHolder(@NonNull CustomerListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
