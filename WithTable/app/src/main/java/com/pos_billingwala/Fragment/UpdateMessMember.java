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
    String[] memberTypeList;
    String memberType;
    List<MemberResponse> memberResponseList = new ArrayList<>();
    FragmentUpdateMessMemberBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUpdateMessMemberBinding.inflate(inflater, container, false);
        view = binding.getRoot();

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

        memberTypeList = activity.getResources().getStringArray(R.array.mess_member_type);
        try {
            binding.memberTypeSpinner.setItems(memberTypeList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        binding.memberTypeSpinner.setOnItemSelectedListener((position, label) -> {
            try {
                memberType = memberTypeList[position];
                toggleTypeFields();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        binding.studentFieldsLayout.setVisibility(View.GONE);
        binding.workingFieldsLayout.setVisibility(View.GONE);

        binding.backToMess.setOnClickListener(this);
        binding.updateMember.setOnClickListener(this);

        TabletFormUi.applyTwoColumnFields(activity, binding.messFormContainer);

        return view;

    }

    private void toggleTypeFields() {
        if (memberType == null) {
            binding.studentFieldsLayout.setVisibility(View.GONE);
            binding.workingFieldsLayout.setVisibility(View.GONE);
            return;
        }
        boolean isStudent = "Student".equalsIgnoreCase(memberType);
        binding.studentFieldsLayout.setVisibility(isStudent ? View.VISIBLE : View.GONE);
        binding.workingFieldsLayout.setVisibility(isStudent ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToMess) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.updateMember) {
            String name = binding.memberName.getText() != null
                    ? binding.memberName.getText().toString().trim() : "";
            String mobile = binding.memberMobileNumber.getText() != null
                    ? binding.memberMobileNumber.getText().toString().trim() : "";
            String altMobile = binding.memberAlternetMobileNumber.getText() != null
                    ? binding.memberAlternetMobileNumber.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_name), Toast.LENGTH_SHORT).show();
                return;
            }
            if (mobile.length() != 10) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_mobile_number), Toast.LENGTH_SHORT).show();
                return;
            }
            if (memberType == null || memberType.isEmpty()) {
                Toast.makeText(activity, getString(R.string.toast_please_select_member_type), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!altMobile.isEmpty() && altMobile.length() != 10) {
                Toast.makeText(activity, getString(R.string.toast_please_enter_member_mobile_number), Toast.LENGTH_SHORT).show();
                return;
            }
            updateMessMember();
        }
    }

    public void updateMessMember() {
        String mobile = binding.memberMobileNumber.getText().toString().trim();
        String altMobile = binding.memberAlternetMobileNumber.getText() != null
                ? binding.memberAlternetMobileNumber.getText().toString().trim() : "";
        String address = binding.memberAddress.getText() != null
                ? binding.memberAddress.getText().toString().trim() : "";

        boolean isStudent = "Student".equalsIgnoreCase(memberType);
        String typeValue = isStudent ? "student" : "working";

        String rollNo = "";
        String college = "";
        String studentYear = "";
        String company = "";

        if (isStudent) {
            rollNo = binding.rollNo.getText() != null ? binding.rollNo.getText().toString().trim() : "";
            college = binding.college.getText() != null ? binding.college.getText().toString().trim() : "";
            studentYear = binding.studentYear.getText() != null ? binding.studentYear.getText().toString().trim() : "";
            company = "";
        } else {
            company = binding.company.getText() != null ? binding.company.getText().toString().trim() : "";
            rollNo = "";
            college = "";
            studentYear = "";
        }

        String registrationNo = (isStudent && !rollNo.isEmpty()) ? rollNo : mobile;

        posBillingWalaDatabase.updateMessMemberProfile(
                memberId,
                binding.memberName.getText().toString().trim(),
                mobile,
                altMobile,
                address,
                registrationNo,
                typeValue,
                rollNo,
                college,
                studentYear,
                company,
                0);

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
            MemberResponse member = memberResponseList.get(0);

            binding.memberName.setText(member.getMemberName());
            binding.memberMobileNumber.setText(member.getMemberMobileNumber());
            binding.memberAlternetMobileNumber.setText(member.getMemberAlternetMobileNumber());
            binding.memberAddress.setText(member.getMemberAddress());

            String storedType = member.getMemberType() != null ? member.getMemberType().trim() : "";
            if ("working".equalsIgnoreCase(storedType)) {
                memberType = "Working";
                binding.memberTypeSpinner.setSelectedIndex(1);
            } else if ("student".equalsIgnoreCase(storedType) || !storedType.isEmpty()) {
                memberType = "Student";
                binding.memberTypeSpinner.setSelectedIndex(0);
            } else {
                memberType = null;
            }
            toggleTypeFields();

            if (binding.rollNo != null) {
                binding.rollNo.setText(member.getRollNo() != null ? member.getRollNo() : "");
            }
            if (binding.college != null) {
                binding.college.setText(member.getCollege() != null ? member.getCollege() : "");
            }
            if (binding.studentYear != null) {
                binding.studentYear.setText(member.getStudentYear() != null ? member.getStudentYear() : "");
            }
            if (binding.company != null) {
                binding.company.setText(member.getCompany() != null ? member.getCompany() : "");
            }
            if (binding.registrationNo != null) {
                binding.registrationNo.setText(member.getRegistrationNo() != null ? member.getRegistrationNo() : "");
            }
        }

    }
}
