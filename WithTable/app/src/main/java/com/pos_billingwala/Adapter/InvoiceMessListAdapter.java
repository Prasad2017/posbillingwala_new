package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Fragment.InvoiceMessMemberPaymentReport;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.databinding.InvoiceMessMemberListBinding;

import java.util.List;


public class InvoiceMessListAdapter extends RecyclerView.Adapter<InvoiceMessListAdapter.MyViewHolder> {

    Context context;
    List<MemberResponse> memberResponseList;

    public InvoiceMessListAdapter(Context context, List<MemberResponse> memberResponseList) {
        this.context = context;
        this.memberResponseList = memberResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(InvoiceMessMemberListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        MemberResponse memberResponse = memberResponseList.get(position);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.memberName.setText(memberResponse.getMemberName());

        holder.binding.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                InvoiceMessMemberPaymentReport invoiceMessMemberPaymentReport = new InvoiceMessMemberPaymentReport();
                Bundle bundle = new Bundle();
                bundle.putString("memberId", memberResponse.getMemberId());
                invoiceMessMemberPaymentReport.setArguments(bundle);
                ((MainActivity) context).loadFragment(invoiceMessMemberPaymentReport, true);
            }
        });


    }

    @Override
    public int getItemCount() {
        return memberResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        InvoiceMessMemberListBinding binding;

        public MyViewHolder(InvoiceMessMemberListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }

}
