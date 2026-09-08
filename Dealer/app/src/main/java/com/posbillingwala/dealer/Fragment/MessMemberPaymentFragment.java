package com.posbillingwala.dealer.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Adapter.MessPaymentManageAdapter;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.MessMemberResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessMemberPaymentFragment extends Fragment {

    private Activity activity;
    private String customerId, licenceId, memberId, memberName;
    private TextInputEditText messAmount, paidAmount, messDays, paymentDate;
    private MessPaymentManageAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_member_payment, container, false);
        activity = getActivity();
        Bundle args = getArguments();
        if (args != null) {
            customerId = args.getString("customerId", "");
            licenceId = args.getString("licenceId", "");
            memberId = args.getString("memberId", "");
            memberName = args.getString("memberName", "");
        }

        ImageView back = view.findViewById(R.id.backBtn);
        TextView memberInfo = view.findViewById(R.id.memberInfo);
        messAmount = view.findViewById(R.id.messAmount);
        paidAmount = view.findViewById(R.id.paidAmount);
        messDays = view.findViewById(R.id.messDays);
        paymentDate = view.findViewById(R.id.paymentDate);
        Button btnSave = view.findViewById(R.id.btnSavePayment);
        RecyclerView recycler = view.findViewById(R.id.recyclerPayments);

        memberInfo.setText((memberName != null ? memberName : "") + " (#" + memberId + ")");
        paymentDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        adapter = new MessPaymentManageAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(adapter);

        back.setOnClickListener(v -> ((MainActivity) activity).removeCurrentFragmentAndMoveBack());
        btnSave.setOnClickListener(v -> savePayment());
        loadPayments();
        return view;
    }

    private String text(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private void loadPayments() {
        Api.getClient().getMessMemberPaymentManageList(MainActivity.userId, customerId, licenceId, memberId)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        List<MessMemberResponse> list = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getMessMemberResponseList() != null) {
                            list = response.body().getMessMemberResponseList();
                        }
                        adapter.setItems(list);
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        Toast.makeText(activity, "Unable to load payments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void savePayment() {
        String mess = text(messAmount);
        String paid = text(paidAmount);
        if (mess.isEmpty() || paid.isEmpty()) {
            Toast.makeText(activity, "Mess Amount and Paid Amount required", Toast.LENGTH_SHORT).show();
            return;
        }
        Api.getClient().saveMessMemberPaymentManage(
                MainActivity.userId, customerId, licenceId, memberId,
                memberName != null ? memberName : "",
                mess, paid, text(messDays), text(paymentDate),
                "active", "", ""
        ).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    messAmount.setText("");
                    paidAmount.setText("");
                    messDays.setText("");
                    loadPayments();
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Save failed";
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
