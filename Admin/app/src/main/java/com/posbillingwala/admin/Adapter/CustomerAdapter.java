package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.LicenseStatusHelper;
import com.posbillingwala.admin.Fragment.AddCustomerProduct;
import com.posbillingwala.admin.Fragment.AddCustomerProductCategory;
import com.posbillingwala.admin.Fragment.AddCustomerSubcategory;
import com.posbillingwala.admin.Fragment.AllCustomerProductList;
import com.posbillingwala.admin.Fragment.CustomerDetails;
import com.posbillingwala.admin.Fragment.NewLicenceRegistration;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.Model.LicenseResponse;
import com.posbillingwala.admin.R;
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

        holder.binding.customerShopName.setText(
                customerResponse.getShopName() != null ? customerResponse.getShopName() : "—");
        holder.binding.customerName.setText(
                "Owner: " + (customerResponse.getName() != null ? customerResponse.getName() : "—"));
        holder.binding.customerNumber.setText(
                customerResponse.getContactNumber() != null ? customerResponse.getContactNumber() : "—");
        holder.binding.customerAddress.setText(
                customerResponse.getAddress() != null ? customerResponse.getAddress() : "");

        LicenseResponse primary = primaryLicense(customerResponse);
        String status = LicenseStatusHelper.displayStatus(primary);
        LicenseStatusHelper.applyBadge(holder.binding.licenseStatusBadge, status);

        if (primary != null) {
            String expiry = primary.getExpiryDate() != null ? primary.getExpiryDate() : "—";
            int branches = 0;
            if (customerResponse.getBranchCount() != null && !customerResponse.getBranchCount().isEmpty()) {
                try {
                    branches = Integer.parseInt(customerResponse.getBranchCount());
                } catch (Exception ignored) {
                    branches = customerResponse.getLicenseResponseList() != null
                            ? customerResponse.getLicenseResponseList().size() : 0;
                }
            } else {
                branches = customerResponse.getLicenseResponseList() != null
                        ? customerResponse.getLicenseResponseList().size() : 0;
            }
            holder.binding.licenseMeta.setText(
                    "Branches: " + branches + "  ·  Exp: " + expiry);
        } else {
            holder.binding.licenseMeta.setText("No license linked");
        }

        holder.binding.editCustomer.setOnClickListener(v -> openDetails(customerResponse));
        holder.binding.cardView.setOnClickListener(v -> openDetails(customerResponse));

        holder.binding.newLicenceRegistration.setOnClickListener(v -> {
            NewLicenceRegistration fragment = new NewLicenceRegistration();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", "" + customerResponse.getId());
            fragment.setArguments(bundle);
            ((MainActivity) context).navigateDetail(fragment, "Add License");
        });

        holder.binding.moreActions.setOnClickListener(v -> showMoreMenu(v, customerResponse));

        // Legacy hidden buttons keep catalog navigation reachable via More menu
        holder.binding.category.setOnClickListener(v -> openCatalog(new AddCustomerProductCategory(), customerResponse));
        holder.binding.subcategory.setOnClickListener(v -> openCatalog(new AddCustomerSubcategory(), customerResponse));
        holder.binding.product.setOnClickListener(v -> openCatalog(new AllCustomerProductList(), customerResponse));
        holder.binding.addProduct.setOnClickListener(v -> openCatalog(new AddCustomerProduct(), customerResponse));
    }

    private void showMoreMenu(View anchor, CustomerResponse customer) {
        PopupMenu menu = new PopupMenu(context, anchor);
        menu.getMenu().add(0, 1, 0, "Categories");
        menu.getMenu().add(0, 2, 1, "Subcategories");
        menu.getMenu().add(0, 3, 2, "Products");
        menu.getMenu().add(0, 4, 3, "Add Product");
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) openCatalog(new AddCustomerProductCategory(), customer);
            else if (id == 2) openCatalog(new AddCustomerSubcategory(), customer);
            else if (id == 3) openCatalog(new AllCustomerProductList(), customer);
            else if (id == 4) openCatalog(new AddCustomerProduct(), customer);
            return true;
        });
        menu.show();
    }

    private void openDetails(CustomerResponse customerResponse) {
        CustomerDetails customerDetails = new CustomerDetails();
        Bundle bundle = new Bundle();
        bundle.putString("customerId", "" + customerResponse.getId());
        customerDetails.setArguments(bundle);
        ((MainActivity) context).navigateDetail(customerDetails, "Customer Details");
    }

    private void openCatalog(androidx.fragment.app.Fragment fragment, CustomerResponse customer) {
        Bundle bundle = new Bundle();
        bundle.putString("customerId", "" + customer.getId());
        fragment.setArguments(bundle);
        ((MainActivity) context).navigateDetail(fragment, "Catalog");
    }

    private LicenseResponse primaryLicense(CustomerResponse customer) {
        if (customer.getLicenseResponseList() == null || customer.getLicenseResponseList().isEmpty()) {
            return null;
        }
        return customer.getLicenseResponseList().get(0);
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
