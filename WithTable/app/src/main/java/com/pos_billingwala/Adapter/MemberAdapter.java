package com.pos_billingwala.Adapter;

import com.pos_billingwala.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
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
import com.pos_billingwala.Fragment.MessMemberPaymentHistory;
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
        String currency = MainActivity.currencyName != null ? MainActivity.currencyName : "₹";

        holder.binding.memberName.setText(safe(memberResponse.getMemberName()));
        holder.binding.memberMobileNumber.setText(safe(memberResponse.getMemberMobileNumber()));

        String address = safe(memberResponse.getMemberAddress());
        if (!address.isEmpty()) {
            holder.binding.memberAddress.setVisibility(View.VISIBLE);
            holder.binding.memberAddress.setText(address);
        } else {
            holder.binding.memberAddress.setVisibility(View.GONE);
        }

        String messAmt = safeAmount(memberResponse.getPaymentMessAmount());
        String paidAmt = safeAmount(memberResponse.getPaymentPaidAmount());
        holder.binding.memberMessAmount.setText("This Month Mess: " + currency + " " + messAmt);
        holder.binding.memberPaidAmount.setText("This Month Paid: " + currency + " " + paidAmt);

        String tokens = memberResponse.getTokensGenerated() != null ? memberResponse.getTokensGenerated() : "0";
        holder.binding.memberTokensGenerated.setText("Tokens This Month: " + tokens);

        holder.binding.memberMonthsSummary.setVisibility(View.GONE);

        float pendingAmount = 0f;
        try {
            pendingAmount = Float.parseFloat(messAmt) - Float.parseFloat(paidAmt);
        } catch (Exception ignored) {
        }

        if (pendingAmount > 0.009f) {
            holder.binding.tableNumberCardView.setBackgroundResource(R.drawable.bg_mess_member_card_pending);
            holder.binding.memberPendingAmount.setText("Pending Amount: " + currency + " "
                    + String.format(Locale.US, "%.2f", pendingAmount));
            holder.binding.memberPendingAmount.setVisibility(View.VISIBLE);
            holder.binding.paymentMember.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tableNumberCardView.setBackgroundResource(R.drawable.bg_mess_member_card);
            holder.binding.memberPendingAmount.setVisibility(View.GONE);
            holder.binding.paymentMember.setVisibility(View.GONE);
        }

        holder.binding.deleteMember.setOnClickListener(v -> deleteMember(memberResponse));

        holder.binding.historyMember.setOnClickListener(v -> {
            MessMemberPaymentHistory history = new MessMemberPaymentHistory();
            Bundle bundle = new Bundle();
            bundle.putString("memberId", memberResponse.getMemberId());
            bundle.putString("memberName", memberResponse.getMemberName());
            bundle.putString("memberMobile", memberResponse.getMemberMobileNumber());
            history.setArguments(bundle);
            ((MainActivity) context).loadFragment(history, true);
        });

        holder.binding.updateMember.setOnClickListener(v -> {
            UpdateMessMember updateMessMember = new UpdateMessMember();
            Bundle bundle = new Bundle();
            bundle.putString("memberId", memberResponse.getMemberId());
            updateMessMember.setArguments(bundle);
            ((MainActivity) context).loadFragment(updateMessMember, true);
        });

        holder.binding.paymentMember.setOnClickListener(v -> {
            UpdateMessPayment updateMessPayment = new UpdateMessPayment();
            Bundle bundle = new Bundle();
            bundle.putString("memberId", memberResponse.getMemberId());
            updateMessPayment.setArguments(bundle);
            ((MainActivity) context).loadFragment(updateMessPayment, true);
        });

        holder.binding.paymentNew.setOnClickListener(v -> {
            AddMemberPayment addMemberPayment = new AddMemberPayment();
            Bundle bundle = new Bundle();
            bundle.putString("memberId", memberResponse.getMemberId());
            addMemberPayment.setArguments(bundle);
            ((MainActivity) context).loadFragment(addMemberPayment, true);
        });
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static String safeAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "0.00";
        }
        try {
            return String.format(Locale.US, "%.2f", Float.parseFloat(value.trim()));
        } catch (Exception e) {
            return value;
        }
    }

    public void deleteMember(MemberResponse memberResponse) {
        posBillingWalaDatabase.deleteMember(memberResponse);
        Toast.makeText(context, context.getString(R.string.toast_member_deleted_successfully), Toast.LENGTH_SHORT).show();
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
