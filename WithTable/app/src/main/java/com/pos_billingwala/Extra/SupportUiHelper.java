package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.pos_billingwala.R;

public final class SupportUiHelper {

    private SupportUiHelper() {}

    public static LinearLayout form(Activity activity) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    public static TextView notice(Activity activity, LinearLayout root, String text) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(activity, R.color.light_black));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        tv.setBackgroundResource(R.drawable.button_rounded_border);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(activity, 12);
        tv.setLayoutParams(lp);
        root.addView(tv);
        return tv;
    }

    public static EditText field(Activity activity, LinearLayout root, String label, String hint) {
        TextView lbl = new TextView(activity);
        lbl.setText(label);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        lbl.setTextColor(ContextCompat.getColor(activity, R.color.black));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.bottomMargin = dp(activity, 4);
        lbl.setLayoutParams(ll);
        root.addView(lbl);

        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setBackgroundResource(R.drawable.button_rounded_border);
        input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(activity, 12);
        input.setLayoutParams(lp);
        root.addView(input);
        return input;
    }

    public static Button primary(Activity activity, LinearLayout root, String label) {
        Button btn = new Button(activity);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setBackgroundResource(R.drawable.button_rounded_border);
        btn.setBackgroundTintList(ContextCompat.getColorStateList(activity, R.color.colorPrimary));
        btn.setTextColor(ContextCompat.getColor(activity, R.color.white));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(activity, 8);
        btn.setLayoutParams(lp);
        root.addView(btn);
        return btn;
    }

    public static void addScreenHeader(Activity activity, LinearLayout root, String title, Runnable onBack) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, 0, 0, dp(activity, 8));
        TextView back = new TextView(activity);
        back.setText("←");
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        back.setTypeface(Typeface.DEFAULT_BOLD);
        back.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 12), dp(activity, 4));
        back.setOnClickListener(v -> {
            if (onBack != null) onBack.run();
        });
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        heading.setTextColor(ContextCompat.getColor(activity, R.color.black));
        bar.addView(back);
        bar.addView(heading);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(activity, 8);
        bar.setLayoutParams(lp);
        root.addView(bar, 0);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
