package com.posbillingwala.admin.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Fragment.AddCustomerProduct;
import com.posbillingwala.admin.Fragment.AddCustomerProductCategory;
import com.posbillingwala.admin.Fragment.AllCustomerProductList;
import com.posbillingwala.admin.Fragment.CustomerDetails;
import com.posbillingwala.admin.Fragment.NewLicenceRegistration;
import com.posbillingwala.admin.Model.CustomerResponse;
import com.posbillingwala.admin.R;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.MyViewHolder> {

    Context context;
    List<CustomerResponse> customerResponseList;

    public CustomerAdapter(Context context, List<CustomerResponse> customerResponseList) {
        this.context = context;
        this.customerResponseList = customerResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.customer_list, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        CustomerResponse customerResponse = customerResponseList.get(position);

        String customerName = "<b>Customer Name: </b>" + customerResponse.getName();
        holder.textViews.get(0).setText(Html.fromHtml(customerName));

        String customerNumber = "<b>Mobile Number: </b>" + customerResponse.getContactNumber();
        holder.textViews.get(1).setText(Html.fromHtml(customerNumber));

        String customerAddress = "<b>Customer Address: </b>" + customerResponse.getAddress();
        holder.textViews.get(2).setText(Html.fromHtml(customerAddress));

        String shopName = "<b>Shop Name: </b>" + customerResponse.getShopName();
        holder.textViews.get(3).setText(Html.fromHtml(shopName));

        holder.textViews.get(6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CustomerDetails customerDetails = new CustomerDetails();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", "" + customerResponse.getId());
                customerDetails.setArguments(bundle);
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                ((MainActivity) context).loadFragment(customerDetails, true);

            }
        });

        holder.textViews.get(4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AddCustomerProductCategory addCustomerProductCategory = new AddCustomerProductCategory();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", "" + customerResponse.getId());
                addCustomerProductCategory.setArguments(bundle);
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                ((MainActivity) context).loadFragment(addCustomerProductCategory, true);

            }
        });

        holder.textViews.get(5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AllCustomerProductList allCustomerProductList = new AllCustomerProductList();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", "" + customerResponse.getId());
                allCustomerProductList.setArguments(bundle);
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                ((MainActivity) context).loadFragment(allCustomerProductList, true);

            }
        });

        holder.textViews.get(7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AddCustomerProduct addCustomerProduct = new AddCustomerProduct();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", "" + customerResponse.getId());
                addCustomerProduct.setArguments(bundle);
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                ((MainActivity) context).loadFragment(addCustomerProduct, true);

            }
        });

        holder.textViews.get(8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                NewLicenceRegistration newLicenceRegistration = new NewLicenceRegistration();
                Bundle bundle = new Bundle();
                bundle.putString("customerId", "" + customerResponse.getId());
                newLicenceRegistration.setArguments(bundle);
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                ((MainActivity) context).loadFragment(newLicenceRegistration, true);

            }
        });


    }

    @Override
    public int getItemCount() {
        return customerResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindViews({R.id.customerName, R.id.customerNumber, R.id.customerAddress, R.id.customerShopName,
                R.id.category, R.id.product, R.id.editCustomer, R.id.addProduct, R.id.newLicenceRegistration})
        List<TextView> textViews;
        @BindView(R.id.cardView)
        CardView cardView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
