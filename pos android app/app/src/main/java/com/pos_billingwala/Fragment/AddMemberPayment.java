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
import com.pos_billingwala.databinding.FragmentAddMemberPaymentBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@SuppressLint("ClickableViewAccessibility, NonConstantResourceId, StaticFieldLeak, NotifyDataSetChanged, SetTextI18n")
public class AddMemberPayment extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String[] messDaysList;
    String messDays, memberId;
    List<MemberResponse> memberResponseList = new ArrayList<>();
    boolean alreadyPaidThisMonth;
    FragmentAddMemberPaymentBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddMemberPaymentBinding.inflate(inflater, container, false);
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
        binding.messDaySpinner.setOnItemSelectedListener((position, label) -> {
            try {
                messDays = messDaysList[position];
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        binding.addPayment.setOnClickListener(this);
        binding.backToMess.setOnClickListener(this);
        TabletFormUi.applyTwoColumnFields(activity, binding.messFormContainer);
        return view;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToMess) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.addPayment) {
            if (messDays == null) {
                Toast.makeText(activity, getString(R.string.toast_please_select_mess_days), Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.memberName.getText() == null || binding.memberName.getText().toString().trim().isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_name), Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.memberMobileNumber.getText() == null
                    || binding.memberMobileNumber.getText().toString().length() != 10) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_mobile_number), Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.messAmount.getText() == null || binding.messAmount.getText().toString().trim().isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_mobile_number), Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.messPaidAmount.getText() == null || binding.messPaidAmount.getText().toString().trim().isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_paid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                if (Float.parseFloat(binding.messAmount.getText().toString())
                        < Float.parseFloat(binding.messPaidAmount.getText().toString())) {
                    Toast.makeText(activity, getString(R.string.toast_please_enter_member_paid_amount_smalled_), Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_paid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            addMessPayment();
        }
    }

    public void addMessPayment() {
        if (alreadyPaidThisMonth) {
            Toast.makeText(activity, getString(R.string.toast_payment_already_done_for_this_month), Toast.LENGTH_SHORT).show();
            return;
        }
        if (memberResponseList.isEmpty()) {
            Toast.makeText(activity, getString(R.string.toast_please_enter_member_name), Toast.LENGTH_SHORT).show();
            return;
        }
        posBillingWalaDatabase.updateMessPendingPayment(
                memberId,
                binding.memberName.getText().toString().trim(),
                messDays,
                binding.messAmount.getText().toString().trim(),
                binding.messPaidAmount.getText().toString().trim(),
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

        alreadyPaidThisMonth = posBillingWalaDatabase.hasMemberPaymentForMonth(memberId, paymentDate);
        memberResponseList.clear();
        memberResponseList = posBillingWalaDatabase.getMemberPaymentDetails(memberId, paymentDate);
        if (!memberResponseList.isEmpty()) {
            binding.memberName.setText(memberResponseList.get(0).getMemberName());
            binding.memberMobileNumber.setText(memberResponseList.get(0).getMemberMobileNumber());
            binding.memberName.setEnabled(false);
            binding.memberMobileNumber.setEnabled(false);
        }
        if (alreadyPaidThisMonth) {
            Toast.makeText(activity, getString(R.string.toast_payment_already_done_for_this_month), Toast.LENGTH_SHORT).show();
            binding.addPayment.setEnabled(false);
        }
    }
}
