package com.posbillingwala.owner.Extra;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.posbillingwala.owner.R;

public final class BottomSheetUi {

    private BottomSheetUi() {
    }

    public static void showConfirm(Context context, CharSequence title, CharSequence message,
                                   CharSequence positive, CharSequence negative,
                                   boolean cancelable, Runnable onConfirm) {
        if (context == null) {
            return;
        }
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_confirm, null);
        BottomSheetDialog sheet = new BottomSheetDialog(context);
        sheet.setContentView(sheetView);
        sheet.setCancelable(cancelable);

        TextView titleView = sheetView.findViewById(R.id.sheetTitle);
        TextView messageView = sheetView.findViewById(R.id.sheetMessage);
        TextView btnPositive = sheetView.findViewById(R.id.btnSheetPositive);
        TextView btnNegative = sheetView.findViewById(R.id.btnSheetNegative);

        titleView.setText(title);
        messageView.setText(message);
        btnPositive.setText(positive);
        btnNegative.setText(negative);

        btnNegative.setOnClickListener(v -> sheet.dismiss());
        btnPositive.setOnClickListener(v -> {
            sheet.dismiss();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        present(sheet);
    }

    public static void showAction(Activity activity, CharSequence title, CharSequence message,
                                  CharSequence primaryText, @Nullable CharSequence secondaryText,
                                  @DrawableRes int iconRes, boolean cancelable,
                                  Runnable onPrimary, @Nullable Runnable onSecondary) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_info, null);
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        sheet.setContentView(sheetView);
        sheet.setCancelable(cancelable);

        ImageView iconView = sheetView.findViewById(R.id.sheetIcon);
        TextView titleView = sheetView.findViewById(R.id.sheetTitle);
        TextView messageView = sheetView.findViewById(R.id.sheetMessage);
        TextView btnPrimary = sheetView.findViewById(R.id.btnSheetPrimary);
        TextView btnSecondary = sheetView.findViewById(R.id.btnSheetSecondary);

        titleView.setText(title);
        messageView.setText(message);
        btnPrimary.setText(primaryText);

        if (iconRes != 0) {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageResource(iconRes);
        }

        btnPrimary.setOnClickListener(v -> {
            sheet.dismiss();
            if (onPrimary != null) {
                onPrimary.run();
            }
        });

        if (secondaryText != null) {
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText(secondaryText);
            btnSecondary.setOnClickListener(v -> {
                sheet.dismiss();
                if (onSecondary != null) {
                    onSecondary.run();
                }
            });
        }

        present(sheet);
    }

    public static void showCustom(Activity activity, CharSequence title, View content,
                                  @Nullable CharSequence positive, @Nullable CharSequence negative,
                                  boolean cancelable, @Nullable Runnable onPositive,
                                  @Nullable Runnable onNegative) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_custom, null);
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        sheet.setContentView(sheetView);
        sheet.setCancelable(cancelable);

        TextView titleView = sheetView.findViewById(R.id.sheetTitle);
        FrameLayout contentHost = sheetView.findViewById(R.id.sheetContent);
        TextView btnPositive = sheetView.findViewById(R.id.btnSheetPositive);
        TextView btnNegative = sheetView.findViewById(R.id.btnSheetNegative);

        titleView.setText(title);
        contentHost.addView(content);

        sheetView.findViewById(R.id.closeCustomSheet).setOnClickListener(v -> sheet.dismiss());

        if (negative != null) {
            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setText(negative);
            btnNegative.setOnClickListener(v -> {
                sheet.dismiss();
                if (onNegative != null) {
                    onNegative.run();
                }
            });
        }

        if (positive != null) {
            btnPositive.setVisibility(View.VISIBLE);
            btnPositive.setText(positive);
            btnPositive.setOnClickListener(v -> {
                sheet.dismiss();
                if (onPositive != null) {
                    onPositive.run();
                }
            });
        }

        present(sheet);
    }

    public interface ChoiceListener {
        void onChoice(int index);
    }

    public static BottomSheetDialog showContent(Context context, View content, boolean cancelable) {
        if (context instanceof Activity) {
            return showContent((Activity) context, content, cancelable);
        }
        return null;
    }

    public static void showSingleChoice(Activity activity, CharSequence title, CharSequence[] items,
                                        int selectedIndex, boolean showCancel, ChoiceListener listener) {
        if (activity == null || activity.isFinishing() || items == null || items.length == 0) {
            return;
        }
        View sheetView = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_single_choice, null);
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        sheet.setContentView(sheetView);

        TextView titleView = sheetView.findViewById(R.id.sheetTitle);
        LinearLayout choiceList = sheetView.findViewById(R.id.choiceList);
        TextView btnCancel = sheetView.findViewById(R.id.btnSheetCancel);

        titleView.setText(title);

        float density = activity.getResources().getDisplayMetrics().density;
        int dividerMargin = (int) (20 * density);

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            View row = LayoutInflater.from(activity).inflate(R.layout.item_bottom_sheet_choice_row, choiceList, false);
            TextView label = row.findViewById(R.id.choiceLabel);
            ImageView tick = row.findViewById(R.id.choiceTick);
            boolean selected = i == selectedIndex;

            label.setText(items[i]);
            if (selected) {
                tick.setVisibility(View.VISIBLE);
                label.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary));
                label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                tick.setVisibility(View.GONE);
                label.setTextColor(ContextCompat.getColor(activity, R.color.black));
            }

            row.setOnClickListener(v -> {
                sheet.dismiss();
                if (listener != null) {
                    listener.onChoice(index);
                }
            });
            choiceList.addView(row);

            if (i < items.length - 1) {
                View divider = new View(activity);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                dividerParams.setMarginStart(dividerMargin);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(ContextCompat.getColor(activity, R.color.light_black));
                divider.setAlpha(0.15f);
                choiceList.addView(divider);
            }
        }

        if (showCancel) {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> sheet.dismiss());
        }

        present(sheet);
    }

    public static void showSingleChoice(Activity activity, CharSequence title, java.util.List<String> items,
                                        int selectedIndex, ChoiceListener listener) {
        showSingleChoice(activity, title, items.toArray(new String[0]), selectedIndex, false, listener);
    }

    public static BottomSheetDialog showContent(Activity activity, View content, boolean cancelable) {
        if (activity == null || activity.isFinishing()) {
            return null;
        }
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        sheet.setContentView(content);
        sheet.setCancelable(cancelable);
        present(sheet);
        return sheet;
    }

    public static void showNoInternet(Context context) {
        if (context == null) {
            return;
        }
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_no_internet, null);
        BottomSheetDialog sheet = new BottomSheetDialog(context);
        sheet.setContentView(sheetView);
        sheet.setCancelable(false);

        sheetView.findViewById(R.id.retry).setOnClickListener(v -> {
            if (DetectConnection.checkInternetConnection(context)) {
                sheet.dismiss();
            }
        });

        present(sheet);
    }

    public static void dialSupport(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + activity.getString(R.string.support_phone_dial)));
        activity.startActivity(intent);
    }

    private static void present(BottomSheetDialog sheet) {
        sheet.setOnShowListener(d -> {
            View bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        sheet.show();
        applyFullWidth(sheet);
        ScreenshotConfig.applyDialog(sheet);
    }

    public static void applyFullWidth(BottomSheetDialog sheet) {
        if (sheet.getWindow() == null) {
            return;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(sheet.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        sheet.getWindow().setAttributes(lp);
    }
}
