package com.pos_billingwala.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Adapter.MemberAdapter;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.ListLoader;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.FragmentMessMemberListBinding;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;


@SuppressLint("StaticFieldLeak, ClickableViewAccessibility, NonConstantResourceId, NotifyDataSetChanged, SetTextI18n")
public class MessMemberList extends Fragment implements View.OnClickListener {

    public static Activity activity;
    public static RecyclerView recyclerView;
    public static LinearLayout linearLayout;
    public static TextInputEditText textInputEditText;
    public static TextView noDataFound;
    public static POSBillingWalaDatabase posBillingWalaDatabase;
    public static List<MemberResponse> memberResponseList = new ArrayList<>();
    public static List<MemberResponse> searchMemberResponseList = new ArrayList<>();
    View view;
    PopupWindow mypopupWindow;
    FragmentMessMemberListBinding binding;

    public static void getAllMessMemberList() {
        SweetAlertDialog loader = ListLoader.show(activity);
        try {
            memberResponseList = posBillingWalaDatabase.getMemberList();
            if (!memberResponseList.isEmpty()) {

                MemberAdapter adapter = new MemberAdapter(activity, memberResponseList);
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                // adapter.notifyItemInserted(memberResponseList.size() - 1);

                noDataFound.setVisibility(View.GONE);
                linearLayout.setVisibility(View.VISIBLE);
            } else {
                noDataFound.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.GONE);
            }
        } finally {
            ListLoader.dismiss(loader);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMessMemberListBinding.inflate(inflater, container, false);
        view = binding.getRoot(); //Root xml or viewGroup will be a part of converted view over here

        activity = getActivity();

        posBillingWalaDatabase = new POSBillingWalaDatabase(activity);
        initViews();

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

        textInputEditText.setSelection(textInputEditText.getText().toString().length());

        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchMessMember(s.toString());
            }
        });


        return view;
    }

    public void initViews() {
        recyclerView = view.findViewById(R.id.recyclerView);
        linearLayout = view.findViewById(R.id.linearLayout);
        textInputEditText = view.findViewById(R.id.searchMessMember);
        noDataFound = view.findViewById(R.id.noDataFound);

        binding.backToSetting.setOnClickListener(this);
        binding.menuIcon.setOnClickListener(this);

    }

    public void searchMessMember(String memberData) {

        searchMemberResponseList.clear();
        if (!memberData.isEmpty()) {
            for (int i = 0; i < memberResponseList.size(); i++)
                if ((memberResponseList.get(i).getMemberName() + memberResponseList.get(i).getMemberMobileNumber()).toLowerCase().contains(memberData.toLowerCase().trim())) {
                    searchMemberResponseList.add(memberResponseList.get(i));
                }
        } else {
            searchMemberResponseList = new ArrayList<>();
            searchMemberResponseList.addAll(memberResponseList);
        }

        if (!searchMemberResponseList.isEmpty()) {

            MemberAdapter adapter = new MemberAdapter(activity, searchMemberResponseList);
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            //  adapter.notifyItemInserted(searchMemberResponseList.size() - 1);

            noDataFound.setVisibility(View.GONE);
        } else {
            noDataFound.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.backToSetting) {
            ((MainActivity) activity).navigateBack();
        } else if (id == R.id.menuIcon) {
            setPopUpWindow();
        }
    }

    public void setPopUpWindow() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.add_member_dialog, null);
        mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

        LinearLayout addMemberLayout = view.findViewById(R.id.addMemberLayout);

        TextView addMemberTxt = view.findViewById(R.id.addMember);

        addMemberTxt.setText("Add New Member");

        addMemberLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypopupWindow.dismiss();
                                ((MainActivity) activity).loadFragment(new AddMessMember(), true);
            }
        });

        mypopupWindow.showAsDropDown(binding.menuIcon, 0, -75);

    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) activity).lockUnlockDrawer(1);
        getAllMessMemberList();

    }
}