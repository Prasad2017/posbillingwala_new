package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.MemberPaymentHistoryAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.MemberPaymentMonthItem;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentMessMemberPaymentHistoryBinding;

import java.util.ArrayList;
import java.util.List;
import com.pos_billingwala.Extra.EmptyListUi;

@SuppressLint("SetTextI18n")
public class MessMemberPaymentHistory extends Fragment {

    private Activity activity;
    private FragmentMessMemberPaymentHistoryBinding binding;
    private POSBillingWalaDatabase db;
    private String memberId = "";
    private String memberName = "";
    private String memberMobile = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMessMemberPaymentHistoryBinding.inflate(inflater, container, false);
        activity = getActivity();
        db = new POSBillingWalaDatabase(activity);

        Bundle args = getArguments();
        if (args != null) {
            memberId = args.getString("memberId", "");
            memberName = args.getString("memberName", "");
            memberMobile = args.getString("memberMobile", "");
        }

        binding.toolbarSubtitle.setText(memberName != null ? memberName : "");
        binding.memberInfo.setText(memberName != null && !memberName.isEmpty() ? memberName : "-");
        binding.memberMobileInfo.setText(memberMobile != null && !memberMobile.isEmpty() ? memberMobile : "-");
        binding.backBtn.setOnClickListener(v -> ((MainActivity) activity).navigateBack());

        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.i("tag", "onKey Back listener is working!!!");
                ((MainActivity) activity).navigateBack();
                return true;
            }
            return false;
        });

        loadHistory();
        return root;
    }

    private void loadHistory() {
        List<MemberPaymentMonthItem> items = db.getMemberPaymentMonthHistory(memberId);
        if (items == null) {
            items = new ArrayList<>();
        }
        boolean hasData = !items.isEmpty();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        binding.recyclerView.setAdapter(new MemberPaymentHistoryAdapter(activity, items));
        binding.recyclerView.setVisibility(hasData ? View.VISIBLE : View.GONE);
        EmptyListUi.bind(binding.noDataFound, hasData, R.string.empty_sub_member_payments);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (activity != null) {
            ((MainActivity) activity).lockUnlockDrawer(1);
        }
    }
}
