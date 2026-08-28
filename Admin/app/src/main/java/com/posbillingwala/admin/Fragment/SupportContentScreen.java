package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.R;

public class SupportContentScreen extends Fragment {
    public static SupportContentScreen newInstance(String title, String[] sections) {
        SupportContentScreen f = new SupportContentScreen();
        Bundle b = new Bundle();
        b.putString("title", title);
        b.putStringArray("sections", sections);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity a = getActivity();
        Bundle args = getArguments();
        String title = args != null ? args.getString("title", "Support") : "Support";
        String[] sections = args != null ? args.getStringArray("sections") : new String[0];
        if (sections == null) sections = new String[0];
        ((MainActivity) a).setScreenTitle(title);
        ScrollView scroll = new ScrollView(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 40);
        root.setBackgroundColor(Color.parseColor("#F7F9FC"));
        for (int i = 0; i + 1 < sections.length; i += 2) {
            TextView h = new TextView(a);
            h.setText(sections[i]);
            h.setTextSize(15f);
            h.setPadding(0, 20, 0, 6);
            h.setTextColor(ContextCompat.getColor(a, R.color.colorTextPrimary));
            TextView b = new TextView(a);
            b.setText(sections[i + 1]);
            b.setBackgroundResource(R.drawable.bg_card);
            b.setPadding(28, 24, 28, 24);
            b.setTextColor(ContextCompat.getColor(a, R.color.colorTextSecondary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 8;
            b.setLayoutParams(lp);
            root.addView(h);
            root.addView(b);
        }
        scroll.addView(root);
        return scroll;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MainActivity) getActivity()).lockUnlockDrawer(1);
    }
}
