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
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.Extra.TabletFormUi;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentUpdateMessMemberBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


@SuppressLint("ClickableViewAccessibility, NonConstantResourceId, StaticFieldLeak, NotifyDataSetChanged")
public class UpdateMessMember extends Fragment implements View.OnClickListener {


    public static Activity activity;
    View view;
    POSBillingWalaDatabase posBillingWalaDatabase;
    String memberId;
    List<MemberResponse> memberResponseList = new ArrayList<>();
    FragmentUpdateMessMemberBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUpdateMessMemberBinding.inflate(inflater, container, false);
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

        Bundle bundle = getArguments();
        if (bundle != null) {
            memberId = bundle.getString("memberId");
        }

        binding.backToMess.setOnClickListener(this);
        binding.updateMember.setOnClickListener(this);

        TabletFormUi.applyTwoColumnFields(activity, binding.messFormContainer);

        return view;

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToMess) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.updateMember) {
            if (!binding.memberName.getText().toString().isEmpty()) {
                if (binding.memberMobileNumber.getText().toString().length() == 10) {
                    if (binding.memberAlternetMobileNumber.getText().toString().length() == 10) {
                        if (!binding.memberAddress.getText().toString().isEmpty()) {
                            updateMessMember();
                        } else {
                            Toast.makeText(activity, getString(R.string.toast_please_enter_member_address), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, getString(R.string.toast_please_enter_member_mobile_number), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, getString(R.string.toast_please_enter_member_name), Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    public void updateMessMember() {

        posBillingWalaDatabase.updateMessMember(memberId, binding.memberName.getText().toString(), binding.memberMobileNumber.getText().toString(), binding.memberAlternetMobileNumber.getText().toString(),
                binding.memberAddress.getText().toString(), 0);

        Toast.makeText(activity, getString(R.string.toast_member_details_updated_successfully), Toast.LENGTH_SHORT).show();

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
        getMemberDetails();

    }

    public void getMemberDetails() {

        memberResponseList.clear();

        memberResponseList = posBillingWalaDatabase.getMemberDetails(memberId);
        if (memberResponseList.size() > 0) {

            binding.memberName.setText(memberResponseList.get(0).getMemberName());
            binding.memberMobileNumber.setText(memberResponseList.get(0).getMemberMobileNumber());
            binding.memberAlternetMobileNumber.setText(memberResponseList.get(0).getMemberAlternetMobileNumber());
            binding.memberAddress.setText(memberResponseList.get(0).getMemberAddress());

        }

    }
}