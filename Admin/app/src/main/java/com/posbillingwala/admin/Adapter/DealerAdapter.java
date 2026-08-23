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
import com.posbillingwala.admin.Fragment.DealerProfile;
import com.posbillingwala.admin.Model.DealerResponse;
import com.posbillingwala.admin.R;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;

public class DealerAdapter extends RecyclerView.Adapter<DealerAdapter.MyViewHolder> {

    Context context;
    List<DealerResponse> dealerResponseList;

    public DealerAdapter(Context context, List<DealerResponse> dealerResponseList) {
        this.context = context;
        this.dealerResponseList = dealerResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dealer_list, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        DealerResponse dealerResponse = dealerResponseList.get(position);

        String customerName = "<b>Name: </b>" + dealerResponse.getName();
        holder.textViews.get(0).setText(Html.fromHtml(customerName));
        String customerNumber = "<b>Mobile Number: </b>" + dealerResponse.getContactNumber();
        holder.textViews.get(1).setText(Html.fromHtml(customerNumber));
        String customerAddress = "<b>Address: </b>" + dealerResponse.getAddress();
        holder.textViews.get(2).setText(Html.fromHtml(customerAddress));
        String shopName = "<b>Aadhaar Number: </b>" + dealerResponse.getAadharNumber();
        holder.textViews.get(3).setText(Html.fromHtml(shopName));

        holder.textViews.get(4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                DealerProfile dealerProfile = new DealerProfile();
                Bundle bundle = new Bundle();
                bundle.putString("dealerId", dealerResponse.getId());
                dealerProfile.setArguments(bundle);
                ((MainActivity) context).loadFragment(dealerProfile, true);

            }
        });

        holder.textViews.get(5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



            }
        });

    }

    @Override
    public int getItemCount() {
        return dealerResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindViews({R.id.dealerName, R.id.dealerNumber, R.id.dealerAddress, R.id.dealerAadhaarNumber,
                R.id.editDealer, R.id.deleteDealer})
        List<TextView> textViews;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

