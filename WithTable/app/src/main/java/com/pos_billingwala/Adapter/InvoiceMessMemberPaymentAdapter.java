package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Extra.RowDividerUi;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.databinding.InvoiceMessPaymentReportListBinding;

import java.util.List;


@SuppressLint("SetTextI18n, NonConstantResourceId")
public class InvoiceMessMemberPaymentAdapter extends RecyclerView.Adapter<InvoiceMessMemberPaymentAdapter.MyViewHolder> {

    Context context;
    List<MemberResponse> memberResponseList;

    public InvoiceMessMemberPaymentAdapter(Context context, List<MemberResponse> memberResponseList) {
        this.context = context;
        this.memberResponseList = memberResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(InvoiceMessPaymentReportListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        MemberResponse memberResponse = memberResponseList.get(position);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.invoiceDate.setText(memberResponse.getPaymentDate());
        holder.binding.messDays.setText(memberResponse.getMessTotalDays());
        holder.binding.messAmount.setText(memberResponse.getPaymentMessAmount());
        holder.binding.paidAmount.setText(MainActivity.currencyName + " " + memberResponse.getPaymentPaidAmount());

        RowDividerUi.bindLastItem(holder.binding.rowDivider, position, getItemCount());
    }

    @Override
    public int getItemCount() {
        return memberResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {


        InvoiceMessPaymentReportListBinding binding;

        public MyViewHolder(InvoiceMessPaymentReportListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }
    }

}
