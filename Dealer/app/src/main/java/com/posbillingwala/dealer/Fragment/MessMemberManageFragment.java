package com.posbillingwala.dealer.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.posbillingwala.dealer.Activity.MainActivity;
import com.posbillingwala.dealer.Adapter.MessMemberManageAdapter;
import com.posbillingwala.dealer.Model.AllApiResponse;
import com.posbillingwala.dealer.Model.CustomerResponse;
import com.posbillingwala.dealer.Model.LicenseResponse;
import com.posbillingwala.dealer.Model.MessMemberResponse;
import com.posbillingwala.dealer.R;
import com.posbillingwala.dealer.Retrofit.Api;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessMemberManageFragment extends Fragment implements MessMemberManageAdapter.Listener {

    private Activity activity;
    private String customerId;
    private Spinner outletSpinner;
    private TextView emptyView;
    private final List<LicenseResponse> outlets = new ArrayList<>();
    private LicenseResponse selectedOutlet;
    private MessMemberManageAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_member_manage, container, false);
        activity = getActivity();

        Bundle args = getArguments();
        if (args != null) {
            customerId = args.getString("customerId", "");
        }

        outletSpinner = view.findViewById(R.id.outletSpinner);
        RecyclerView recyclerMembers = view.findViewById(R.id.recyclerMembers);
        emptyView = view.findViewById(R.id.emptyView);
        ImageView back = view.findViewById(R.id.backBtn);
        TextView btnAdd = view.findViewById(R.id.btnAddMember);
        TextView btnExcel = view.findViewById(R.id.btnExcel);

        adapter = new MessMemberManageAdapter(this);
        recyclerMembers.setLayoutManager(new LinearLayoutManager(activity));
        recyclerMembers.setAdapter(adapter);

        back.setOnClickListener(v -> ((MainActivity) activity).removeCurrentFragmentAndMoveBack());
        btnAdd.setOnClickListener(v -> openEdit(null));
        btnExcel.setOnClickListener(v -> {
            if (selectedOutlet == null) {
                Toast.makeText(activity, R.string.mess_select_outlet, Toast.LENGTH_SHORT).show();
                return;
            }
            MessMemberExcelFragment excel = new MessMemberExcelFragment();
            Bundle b = new Bundle();
            b.putString("customerId", customerId);
            b.putString("licenceId", selectedOutlet.getLicensesId());
            b.putString("outletLabel", outletLabel(selectedOutlet));
            excel.setArguments(b);
            ((MainActivity) activity).loadFragment(excel, true);
        });

        outletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position >= 0 && position < outlets.size()) {
                    selectedOutlet = outlets.get(position);
                    loadMembers();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadOutlets();
        return view;
    }

    private String outletLabel(LicenseResponse lic) {
        String name = lic.getShopName1() != null && !lic.getShopName1().isEmpty()
                ? lic.getShopName1() : "Outlet";
        String branch = lic.getBranchLabel() != null && !lic.getBranchLabel().isEmpty()
                ? " · " + lic.getBranchLabel() : "";
        return name + branch + " (#" + lic.getLicensesId() + ")";
    }

    private void loadOutlets() {
        Api.getClient().getCustomerDetails(customerId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                outlets.clear();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getCustomerResponseList() != null
                        && !response.body().getCustomerResponseList().isEmpty()) {
                    CustomerResponse c = response.body().getCustomerResponseList().get(0);
                    if (c.getLicenseResponseList() != null) {
                        outlets.addAll(c.getLicenseResponseList());
                    }
                }
                List<String> labels = new ArrayList<>();
                for (LicenseResponse lic : outlets) {
                    labels.add(outletLabel(lic));
                }
                if (activity == null) return;
                outletSpinner.setAdapter(new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_dropdown_item, labels));
                if (outlets.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("No outlets found");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(activity, "Unable to load outlets", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMembers() {
        if (selectedOutlet == null || selectedOutlet.getLicensesId() == null) return;
        Api.getClient().getMessMemberManageList(MainActivity.userId, customerId, selectedOutlet.getLicensesId())
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        List<MessMemberResponse> list = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getMessMemberResponseList() != null) {
                            list = response.body().getMessMemberResponseList();
                        }
                        adapter.setItems(list);
                        emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        Toast.makeText(activity, "Unable to load members", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openEdit(MessMemberResponse member) {
        if (selectedOutlet == null) {
            Toast.makeText(activity, R.string.mess_select_outlet, Toast.LENGTH_SHORT).show();
            return;
        }
        MessMemberEditFragment edit = new MessMemberEditFragment();
        Bundle b = new Bundle();
        b.putString("customerId", customerId);
        b.putString("licenceId", selectedOutlet.getLicensesId());
        if (member != null) {
            b.putString("memberId", member.getMemberId());
            b.putString("memberName", member.getMemberName());
            b.putString("memberMobileNumber", member.getMemberMobileNumber());
            b.putString("memberAltenetMobileNumber", member.getMemberAltenetMobileNumber());
            b.putString("memberAddress", member.getMemberAddress());
            b.putString("memberType", member.getMemberType());
            b.putString("rollNo", member.getRollNo());
            b.putString("college", member.getCollege());
            b.putString("studentYear", member.getStudentYear());
            b.putString("company", member.getCompany());
            b.putString("registrationNo", member.getRegistrationNo());
            b.putString("memberNetworkStatus", member.memberNetworkStatus);
        }
        edit.setArguments(b);
        ((MainActivity) activity).loadFragment(edit, true);
    }

    @Override
    public void onEdit(MessMemberResponse member) {
        openEdit(member);
    }

    @Override
    public void onPayments(MessMemberResponse member) {
        MessMemberPaymentFragment pay = new MessMemberPaymentFragment();
        Bundle b = new Bundle();
        b.putString("customerId", customerId);
        b.putString("licenceId", selectedOutlet.getLicensesId());
        b.putString("memberId", member.getMemberId());
        b.putString("memberName", member.getMemberName());
        pay.setArguments(b);
        ((MainActivity) activity).loadFragment(pay, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedOutlet != null) loadMembers();
    }
}
