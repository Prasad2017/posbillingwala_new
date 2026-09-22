package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentUpdateMessPaymentBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@SuppressLint("ClickableViewAccessibility, NonConstantResourceId, StaticFieldLeak, NotifyDataSetChanged, SetTextI18n")
public class UpdateMessPayment extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String[] messDaysList;
    String messDays, memberId;
    List<MemberResponse> memberResponseList = new ArrayList<>();
    float pendingAmount;
    FragmentUpdateMessPaymentBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUpdateMessPaymentBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        activity = getActivity();
        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            memberId = bundle.getString("memberId");
        }

        messDaysList = activity.getResources().getStringArray(R.array.mess_days);
        try {
            binding.messDaySpinner.setItems(messDaysList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        binding.messDaySpinner.setOnItemSelectedListener((position, label) -> messDays = messDaysList[position]);

        binding.backToMess.setOnClickListener(this);
        binding.updatePayment.setOnClickListener(this);
        TabletFormUi.applyTwoColumnFields(activity, binding.messFormContainer);
        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToMess) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.updatePayment) {
            if (binding.messPendingAmount.getText() == null
                    || binding.messPendingAmount.getText().toString().trim().isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_enter_pending_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            float payNow;
            try {
                payNow = Float.parseFloat(binding.messPendingAmount.getText().toString().trim());
            } catch (Exception e) {
                Toast.makeText(activity, getString(R.string.toast_enter_pending_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            if (payNow <= 0 || payNow > pendingAmount + 0.009f) {
                Toast.makeText(activity, getString(R.string.toast_enter_pending_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            updateMessPayment(payNow);
        }
    }

    public void updateMessPayment(float payNow) {
        if (memberResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_payment_already_paid_for_this_month), Toast.LENGTH_SHORT).show();
            return;
        }
        String messAmount = binding.messAmount.getText() != null
                ? binding.messAmount.getText().toString().trim() : "0";
        if (messDays == null || messDays.trim().isEmpty()) {
            messDays = memberResponseList.get(0).getMessTotalDays();
            if (messDays == null || messDays.trim().isEmpty()) {
                messDays = messDaysList != null && messDaysList.length > 0 ? messDaysList[0] : "One Time";
            }
        }
        posBillingWalaDatabase.updateMessPendingPayment(
                memberId,
                binding.memberName.getText().toString().trim(),
                messDays,
                messAmount,
                String.format(Locale.US, "%.2f", payNow),
                0,
                getRandomString(10));
        Toast.makeText(activity, getString(R.string.toast_payment_paid_successfully), Toast.LENGTH_SHORT).show();
        ((MainActivity) activity).navigateBack();
    }

    public String getRandomString(final int sizeOfRandomString) {
        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i) {
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        }
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getMemberPaymentDetails();
    }

    public void getMemberPaymentDetails() {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String paymentDate = df.format(c);

        memberResponseList.clear();
        memberResponseList = posBillingWalaDatabase.getMemberPaymentDetails(memberId, paymentDate);

        if (memberResponseList.isEmpty()
                || !posBillingWalaDatabase.hasMemberPaymentForMonth(memberId, paymentDate)) {
            Toast.makeText(activity, "No mess payment found for this month. Use New Payment.", Toast.LENGTH_SHORT).show();
            ((MainActivity) activity).navigateBack();
            return;
        }

        MemberResponse m = memberResponseList.get(0);
        binding.memberName.setText(m.getMemberName());
        binding.memberMobileNumber.setText(m.getMemberMobileNumber());
        binding.messAmount.setText(m.getPaymentMessAmount() != null ? m.getPaymentMessAmount() : "0");
        binding.messPaidAmount.setText(m.getPaymentPaidAmount() != null ? m.getPaymentPaidAmount() : "0");

        if (m.getMessTotalDays() != null) {
            if (m.getMessTotalDays().equalsIgnoreCase("One Time")) {
                binding.messDaySpinner.setSelectedIndex(0);
                messDays = messDaysList[0];
            } else {
                binding.messDaySpinner.setSelectedIndex(1);
                messDays = messDaysList[1];
            }
        }

        try {
            float messAmt = Float.parseFloat(m.getPaymentMessAmount() != null ? m.getPaymentMessAmount() : "0");
            float paidAmt = Float.parseFloat(m.getPaymentPaidAmount() != null ? m.getPaymentPaidAmount() : "0");
            pendingAmount = messAmt - paidAmt;
            if (pendingAmount > 0.009f) {
                binding.messPendingAmount.setText(String.format(Locale.US, "%.2f", pendingAmount));
                binding.messPendingAmountLayout.setVisibility(View.VISIBLE);
                binding.updatePayment.setEnabled(true);
            } else {
                binding.messPendingAmountLayout.setVisibility(View.GONE);
                binding.updatePayment.setEnabled(false);
                Toast.makeText(activity, getString(R.string.toast_payment_already_paid_for_this_month), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            pendingAmount = 0;
            binding.messPendingAmountLayout.setVisibility(View.GONE);
        }
    }
}
