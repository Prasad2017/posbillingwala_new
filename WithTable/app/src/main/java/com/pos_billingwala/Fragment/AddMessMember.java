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
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentAddMessMemberBinding;

import java.util.Random;

@SuppressLint("ClickableViewAccessibility, NonConstantResourceId, StaticFieldLeak, NotifyDataSetChanged")
public class AddMessMember extends Fragment implements View.OnClickListener {

    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String[] messDaysList;
    String messDays;
    FragmentAddMessMemberBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddMessMemberBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.i("tag", "onKey Back listener is working!!!");
                    ((MainActivity) activity).navigateBack();
                    return true;
                }
                return false;
            }
        });

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

        binding.backToMess.setOnClickListener(this);
        binding.addMember.setOnClickListener(this);

        TabletFormUi.applyTwoColumnFields(activity, binding.messFormContainer);

        return view;

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToMess) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.addMember) {
            if (messDays != null) {
                if (!binding.memberName.getText().toString().isEmpty()) {
                    if (binding.memberMobileNumber.getText().toString().length() == 10) {
                        if (binding.memberAlternetMobileNumber.getText().toString().length() == 10) {
                            if (!binding.memberAddress.getText().toString().isEmpty()) {
                                if (!binding.messAmount.getText().toString().isEmpty()) {
                                    if (!binding.messPaidAmount.getText().toString().isEmpty()) {
                                        if (Float.parseFloat(binding.messAmount.getText().toString()) >= Float.parseFloat(binding.messPaidAmount.getText().toString())) {
                                            addMessMember();
                                        } else {
                                            Toast.makeText(activity, getString(R.string.toast_please_enter_member_paid_amount_smalled_), Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(activity, getString(R.string.toast_please_enter_member_paid_amount), Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(activity, getString(R.string.toast_please_enter_mess_amount), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(activity, getString(R.string.toast_please_enter_member_address), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(activity, getString(R.string.toast_please_enter_member_mobile_number), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, getString(R.string.toast_please_enter_member_name), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, getString(R.string.toast_please_select_mess_days), Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    public void addMessMember() {

        posBillingWalaDatabase.insertMessMember(MainActivity.ownerId, binding.memberName.getText().toString(), binding.memberMobileNumber.getText().toString(), binding.memberAlternetMobileNumber.getText().toString(),
                binding.memberAddress.getText().toString(), binding.messAmount.getText().toString(), binding.messPaidAmount.getText().toString(), messDays, 0, getRandomString(10));

        Toast.makeText(activity, getString(R.string.toast_member_added_successfully), Toast.LENGTH_SHORT).show();

        ((MainActivity) activity).navigateBack();

    }

    public String getRandomString(final int sizeOfRandomString) {

        String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }


    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);

    }

}