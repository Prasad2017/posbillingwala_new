package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Fragment.AddMemberPayment;
import com.pos_billingwala.Fragment.MessMemberList;
import com.pos_billingwala.Fragment.UpdateMessMember;
import com.pos_billingwala.Fragment.UpdateMessPayment;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.databinding.MemberListBinding;

import java.util.List;
import java.util.Locale;


@SuppressLint("SetTextI18n")
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MyViewHolder> {

    List<MemberResponse> memberResponseList;
    Context context;
    POSBillingWalaDatabase posBillingWalaDatabase;

    public MemberAdapter(Context context, List<MemberResponse> memberResponseList) {
        this.context = context;
        this.memberResponseList = memberResponseList;
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(MemberListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        MemberResponse memberResponse = memberResponseList.get(position);
        String memberName = "<b>Member Name: </b>" + memberResponse.getMemberName();
        holder.binding.memberName.setText(Html.fromHtml(memberName));
        String memberNumber = "<b>Member Mobile Number: </b>" + memberResponse.getMemberMobileNumber();
        holder.binding.memberMobileNumber.setText(Html.fromHtml(memberNumber));
        String memberAddress = "<b>Member Address: </b>" + memberResponse.getMemberAddress();
        holder.binding.memberAddress.setText(Html.fromHtml(memberAddress));
        String messAmount = "<b>Mess Amount: </b>" + MainActivity.currencyName + " " + memberResponse.getPaymentMessAmount();
        holder.binding.memberMessAmount.setText(Html.fromHtml(messAmount));
        String messPaidAmount = "<b>Paid Amount: </b>" + MainActivity.currencyName + " " + memberResponse.getPaymentPaidAmount();
        holder.binding.memberPaidAmount.setText(Html.fromHtml(messPaidAmount));

        try {
            float pendingAmount = Float.parseFloat(memberResponse.getPaymentMessAmount()) - Float.parseFloat(memberResponse.getPaymentPaidAmount());
            if (pendingAmount > 0) {
                String messPendingAmount = "<b>Pending Amount: </b>" + MainActivity.currencyName + " " + String.format(Locale.US, "%.2f", pendingAmount);
                holder.binding.memberPendingAmount.setText(Html.fromHtml(messPendingAmount));

                holder.binding.memberPendingAmount.setVisibility(View.VISIBLE);
                holder.binding.paymentMember.setVisibility(View.VISIBLE);
            } else {
                holder.binding.memberPendingAmount.setVisibility(View.GONE);
                holder.binding.paymentMember.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.binding.deleteMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMember(memberResponse);
            }
        });

        holder.binding.updateMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                UpdateMessMember updateMessMember = new UpdateMessMember();
                Bundle bundle = new Bundle();
                bundle.putString("memberId", memberResponse.getMemberId());
                updateMessMember.setArguments(bundle);
                ((MainActivity) context).loadFragment(updateMessMember, true);
            }
        });

        holder.binding.paymentMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                UpdateMessPayment updateMessPayment = new UpdateMessPayment();
                Bundle bundle = new Bundle();
                bundle.putString("memberId", memberResponse.getMemberId());
                updateMessPayment.setArguments(bundle);
                ((MainActivity) context).loadFragment(updateMessPayment, true);
            }
        });

        holder.binding.paymentNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).removeCurrentFragmentAndMoveBack();
                AddMemberPayment addMemberPayment = new AddMemberPayment();
                Bundle bundle = new Bundle();
                bundle.putString("memberId", memberResponse.getMemberId());
                addMemberPayment.setArguments(bundle);
                ((MainActivity) context).loadFragment(addMemberPayment, true);
            }
        });

    }

    public void deleteMember(MemberResponse memberResponse) {
        posBillingWalaDatabase.deleteMember(memberResponse);
        Toast.makeText(context, "Member Deleted Successfully", Toast.LENGTH_SHORT).show();
        MessMemberList.getAllMessMemberList();
    }

    @Override
    public int getItemCount() {
        return memberResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public final MemberListBinding binding;

        public MyViewHolder(MemberListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}