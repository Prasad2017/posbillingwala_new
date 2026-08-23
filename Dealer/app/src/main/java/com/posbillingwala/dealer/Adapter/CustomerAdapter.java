package com.posbillingwala.dealer.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Fragment.AddCustomerProduct;
import com.posbillingwala.dealer.Fragment.AddCustomerProductCategory;
import com.posbillingwala.dealer.Fragment.AllCustomerProductList;
import com.posbillingwala.dealer.Fragment.CustomerDetails;
import com.posbillingwala.dealer.Fragment.NewLicenceRegistration;
import com.posbillingwala.dealer.Model.CustomerResponse;
import com.posbillingwala.dealer.databinding.CustomerListBinding;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.MyViewHolder> {

    private Context context;
    private List<CustomerResponse> customerResponseList;

    public CustomerAdapter(Context context, List<CustomerResponse> customerResponseList) {
        this.context = context;
        this.customerResponseList = customerResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CustomerListBinding binding = CustomerListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerAdapter.MyViewHolder holder, int position) {
        CustomerResponse customerResponse = customerResponseList.get(position);

        String customerName = "<b>Customer Name: </b>" + customerResponse.getName();
        holder.binding.customerName.setText(Html.fromHtml(customerName));

        String customerNumber = "<b>Mobile Number: </b>" + customerResponse.getContactNumber();
        holder.binding.customerNumber.setText(Html.fromHtml(customerNumber));

        String customerAddress = "<b>Customer Address: </b>" + customerResponse.getAddress();
        holder.binding.customerAddress.setText(Html.fromHtml(customerAddress));

        String shopName = "<b>Shop Name: </b>" + customerResponse.getShopName();
        holder.binding.customerShopName.setText(Html.fromHtml(shopName));

        String customerType;
        if (customerResponse.getRoleId().equalsIgnoreCase("2")) {
            customerType = "<b>User Type: </b> Dealer";
            holder.binding.category.setVisibility(View.GONE);
            holder.binding.product.setVisibility(View.GONE);
            holder.binding.addProduct.setVisibility(View.GONE);
            if (customerResponse.getLicenseResponseList().isEmpty()) {
                holder.binding.newLicenceRegistration.setVisibility(View.VISIBLE);
            } else {
                holder.binding.newLicenceRegistration.setVisibility(View.GONE);
            }
        } else {
            customerType = "<b>User Type: </b> Customer";
            holder.binding.category.setVisibility(View.VISIBLE);
            holder.binding.product.setVisibility(View.VISIBLE);
            holder.binding.addProduct.setVisibility(View.VISIBLE);
        }
        holder.binding.customerType.setText(Html.fromHtml(customerType));

        holder.binding.editCustomer.setOnClickListener(v -> {
            CustomerDetails customerDetails = new CustomerDetails();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", customerResponse.getId());
            bundle.putString("customerType", customerResponse.getRoleId());
            customerDetails.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(customerDetails, true);
        });

        holder.binding.category.setOnClickListener(v -> {
            AddCustomerProductCategory addCustomerProductCategory = new AddCustomerProductCategory();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", customerResponse.getId());
            addCustomerProductCategory.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(addCustomerProductCategory, true);
        });

        holder.binding.product.setOnClickListener(v -> {
            AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", customerResponse.getId());
            allCustomerProductList.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(allCustomerProductList, true);
        });

        holder.binding.addProduct.setOnClickListener(v -> {
            AddCustomerProduct addCustomerProduct = new AddCustomerProduct();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", customerResponse.getId());
            addCustomerProduct.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(addCustomerProduct, true);
        });

        holder.binding.newLicenceRegistration.setOnClickListener(v -> {
            NewLicenceRegistration newLicenceRegistration = new NewLicenceRegistration();
            Bundle bundle = new Bundle();
            bundle.putString("customerId", customerResponse.getId());
            bundle.putString("customerName", customerResponse.getName());
            bundle.putString("customerNumber", customerResponse.getContactNumber());
            bundle.putString("customerAddress", customerResponse.getAddress());
            bundle.putString("shopName", customerResponse.getShopName());
            newLicenceRegistration.setArguments(bundle);
            ((MainActivity) context).removeCurrentFragmentAndMoveBack();
            ((MainActivity) context).loadFragment(newLicenceRegistration, true);
        });

    }

    @Override
    public int getItemCount() {
        return customerResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private CustomerListBinding binding;

        public MyViewHolder(@NonNull CustomerListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
