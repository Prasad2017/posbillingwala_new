package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pos_billingwala.Model.ExpenseResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.ExpenseListBinding;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.MyViewHolder> {

    Context context;
    List<ExpenseResponse> expenseResponseList;

    public ExpenseAdapter(Context context, List<ExpenseResponse> expenseResponseList) {
        this.context = context;
        this.expenseResponseList = expenseResponseList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ExpenseListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ExpenseResponse expenseResponse = expenseResponseList.get(position);

        holder.binding.srNo.setText("" + (position + 1));
        holder.binding.expensesDate.setText(expenseResponse.getExpenseDate());
        holder.binding.expensesName.setText(expenseResponse.getExpenseName());
        holder.binding.expensesAmount.setText(context.getString(R.string.inr) + " " + expenseResponse.getExpenseAmount());

    }

    @Override
    public int getItemCount() {
        return expenseResponseList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        ExpenseListBinding binding;

        public MyViewHolder(ExpenseListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
