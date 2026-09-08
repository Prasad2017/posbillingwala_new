package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessMemberEditFragment extends Fragment {

    private Activity activity;
    private String licenceId;
    private String memberId = "";
    private String memberNetworkStatus = "";
    private TextInputEditText memberName, memberMobile, memberAltMobile, memberAddress;
    private TextInputEditText rollNo, college, studentYear, company, registrationNo;
    private LinearLayout studentFields;
    private TextInputLayout companyLayout;
    private RadioButton typeStudent, typeWorking;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_member_edit, container, false);
        activity = getActivity();

        Bundle args = getArguments();
        if (args != null) {
            licenceId = args.getString("licenceId", "");
            memberId = args.getString("memberId", "");
            memberNetworkStatus = args.getString("memberNetworkStatus", "");
        }

        ImageView back = view.findViewById(R.id.backBtn);
        TextView title = view.findViewById(R.id.titleTxt);
        memberName = view.findViewById(R.id.memberName);
        memberMobile = view.findViewById(R.id.memberMobile);
        memberAltMobile = view.findViewById(R.id.memberAltMobile);
        memberAddress = view.findViewById(R.id.memberAddress);
        rollNo = view.findViewById(R.id.rollNo);
        college = view.findViewById(R.id.college);
        studentYear = view.findViewById(R.id.studentYear);
        company = view.findViewById(R.id.company);
        registrationNo = view.findViewById(R.id.registrationNo);
        studentFields = view.findViewById(R.id.studentFields);
        companyLayout = view.findViewById(R.id.companyLayout);
        typeStudent = view.findViewById(R.id.typeStudent);
        typeWorking = view.findViewById(R.id.typeWorking);
        RadioGroup typeGroup = view.findViewById(R.id.memberTypeGroup);
        Button btnSave = view.findViewById(R.id.btnSave);

        if (memberId != null && !memberId.isEmpty()) {
            title.setText(R.string.mess_edit_member);
            if (args != null) {
                memberName.setText(args.getString("memberName", ""));
                memberMobile.setText(args.getString("memberMobileNumber", ""));
                memberAltMobile.setText(args.getString("memberAltenetMobileNumber", ""));
                memberAddress.setText(args.getString("memberAddress", ""));
                rollNo.setText(args.getString("rollNo", ""));
                college.setText(args.getString("college", ""));
                studentYear.setText(args.getString("studentYear", ""));
                company.setText(args.getString("company", ""));
                registrationNo.setText(args.getString("registrationNo", ""));
                String type = args.getString("memberType", "student");
                if ("working".equalsIgnoreCase(type)) {
                    typeWorking.setChecked(true);
                } else {
                    typeStudent.setChecked(true);
                }
            }
        }

        typeGroup.setOnCheckedChangeListener((group, checkedId) -> updateTypeUi());
        updateTypeUi();

        back.setOnClickListener(v -> ((MainActivity) activity).removeCurrentFragmentAndMoveBack());
        btnSave.setOnClickListener(v -> save());
        return view;
    }

    private void updateTypeUi() {
        boolean working = typeWorking.isChecked();
        studentFields.setVisibility(working ? View.GONE : View.VISIBLE);
        companyLayout.setVisibility(working ? View.VISIBLE : View.GONE);
    }

    private String text(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private void save() {
        String name = text(memberName);
        String mobile = text(memberMobile);
        if (name.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(activity, "Name and Mobile are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mobile.length() != 10) {
            Toast.makeText(activity, "Mobile must be 10 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        String type = typeWorking.isChecked() ? "working" : "student";
        Api.getClient().saveMessMemberManage(
                MainActivity.userId,
                licenceId,
                memberId != null ? memberId : "",
                name,
                mobile,
                text(memberAltMobile),
                text(memberAddress),
                type,
                text(rollNo),
                text(college),
                text(studentYear),
                text(company),
                text(registrationNo),
                "active",
                memberNetworkStatus != null ? memberNetworkStatus : ""
        ).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().getStatus())) {
                    Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
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
