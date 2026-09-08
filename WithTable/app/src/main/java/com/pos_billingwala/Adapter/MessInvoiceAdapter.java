package com.pos_billingwala.Adapter;

import com.pos_billingwala.Extra.PopupUi;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.CouponBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.MessTokenBluetoothPrint;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.MessPayerMode;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.Model.MemberResponse;
import com.pos_billingwala.Model.MessInvoiceResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.databinding.MemberInvoiceListBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


@SuppressLint("NonConstantResourceId, SetTextI18n")
public class MessInvoiceAdapter extends RecyclerView.Adapter<MessInvoiceAdapter.MyViewHolder> {

    List<MemberResponse> memberResponseList;
    Context context;
    View view;
    PopupWindow mypopupWindow;
    float pendingAmount = 0;
    List<MessInvoiceResponse> messInvoiceResponseList = new ArrayList<>();
    POSBillingWalaDatabase posBillingWalaDatabase;


    public MessInvoiceAdapter(Context context, List<MemberResponse> memberResponseList) {
        this.context = context;
        this.memberResponseList = memberResponseList;
        this.posBillingWalaDatabase = new POSBillingWalaDatabase(context);
    }

    @NonNull
    @Override
    public MessInvoiceAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(MemberInvoiceListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessInvoiceAdapter.MyViewHolder holder, int position) {

        MemberResponse memberResponse = memberResponseList.get(position);

        holder.binding.memberName.setText(memberResponse.getMemberName());

        String mobile = memberResponse.getMemberMobileNumber();
        if (mobile != null && !mobile.trim().isEmpty()) {
            holder.binding.memberMobile.setVisibility(View.VISIBLE);
            holder.binding.memberMobile.setText(mobile);
        } else {
            holder.binding.memberMobile.setVisibility(View.GONE);
        }

        float messAmt = parseFloatSafe(memberResponse.getPaymentMessAmount());
        float paidAmt = parseFloatSafe(memberResponse.getPaymentPaidAmount());
        final float pending = Math.max(0f, messAmt - paidAmt);

        boolean hasPending = pending > 0.009f;
        if (hasPending) {
            holder.binding.tableNumberCardView.setBackgroundResource(R.drawable.bg_mess_member_card_pending);
            holder.binding.pendingAmount.setVisibility(View.VISIBLE);
            holder.binding.pendingAmountTxt.setVisibility(View.VISIBLE);
            holder.binding.pendingAmountTxt.setText(
                    context.getString(R.string.ui_mess_pending_amount_label,
                            MainActivity.currencyName != null ? MainActivity.currencyName : "₹",
                            String.format(Locale.US, "%.2f", pending)));
        } else {
            holder.binding.tableNumberCardView.setBackgroundResource(R.drawable.bg_mess_member_card);
            holder.binding.pendingAmount.setVisibility(View.GONE);
            holder.binding.pendingAmountTxt.setVisibility(View.GONE);
        }

        final int todayTokens = parseIntSafe(memberResponse.getTodayTokensGenerated());
        int monthTokens = parseIntSafe(memberResponse.getTokensGenerated());
        int allowedToday = resolveAllowedMessDays(memberResponse);
        holder.binding.tokenCountTxt.setText(
                context.getString(R.string.ui_mess_token_count_label, todayTokens, allowedToday, monthTokens));

        final boolean alreadyPrinted = todayTokens >= allowedToday;
        setPrintEnabled(holder, !alreadyPrinted);

        holder.binding.pendingAmount.setOnClickListener(v -> {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.pending_amount_dialog, null);
            mypopupWindow = PopupUi.create(context, view);

            TextView pendingAmountLayout = view.findViewById(R.id.pendingAmountLayout);
            TextView pendingAmountTxt = view.findViewById(R.id.pendingAmount);
            pendingAmountTxt.setText((MainActivity.currencyName != null ? MainActivity.currencyName : "₹")
                    + " " + String.format(Locale.US, "%.2f", pending));

            pendingAmountLayout.setOnClickListener(click -> mypopupWindow.dismiss());
            PopupUi.showBelowAnchor(mypopupWindow, holder.binding.pendingAmount);
        });

        holder.binding.billPrintLayout.setOnClickListener(v -> {
            if (alreadyPrinted) {
                Toast.makeText(context, context.getString(R.string.toast_already_coupon_created), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!hasPaidCurrentMonth(memberResponse)) {
                Toast.makeText(context, context.getString(R.string.toast_not_paid_this_month), Toast.LENGTH_LONG).show();
                return;
            }
            setBillPrintPassword(memberResponse, false);
        });

        holder.binding.qrTokenLayout.setOnClickListener(v -> {
            if (alreadyPrinted) {
                Toast.makeText(context, context.getString(R.string.toast_already_coupon_created), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!hasPaidCurrentMonth(memberResponse)) {
                Toast.makeText(context, context.getString(R.string.toast_not_paid_this_month), Toast.LENGTH_LONG).show();
                return;
            }
            setBillPrintPassword(memberResponse, true);
        });
    }

    /** True when institute pay is on, or this month has any paid amount (> 0). */
    private boolean hasPaidCurrentMonth(MemberResponse memberResponse) {
        if (MessPayerMode.isInstitutePay(context)) {
            return true;
        }
        float paid = parseFloatSafe(memberResponse.getPaymentPaidAmount());
        float mess = parseFloatSafe(memberResponse.getPaymentMessAmount());
        // Must have this-month package with at least some payment received.
        return mess > 0.009f && paid > 0.009f;
    }

    private void setPrintEnabled(MyViewHolder holder, boolean enabled) {
        holder.binding.billPrintLayout.setEnabled(enabled);
        holder.binding.qrTokenLayout.setEnabled(enabled);
        holder.binding.billPrintLayout.setAlpha(enabled ? 1f : 0.45f);
        holder.binding.qrTokenLayout.setAlpha(enabled ? 1f : 0.45f);
        holder.binding.billPrintLayout.setBackgroundResource(
                enabled ? R.drawable.bg_mess_action_btn : R.drawable.bg_mess_action_btn_disabled);
        holder.binding.qrTokenLayout.setBackgroundResource(
                enabled ? R.drawable.bg_mess_action_btn : R.drawable.bg_mess_action_btn_disabled);

        int color = ContextCompat.getColor(context, enabled ? R.color.colorPrimary : R.color.colorTextHint);
        holder.binding.billPrintTxt.setTextColor(color);
        holder.binding.qrTokenTxt.setTextColor(color);
        holder.binding.billPrintIcon.setColorFilter(color);
        holder.binding.qrTokenIcon.setColorFilter(color);

        if (!enabled) {
            holder.binding.billPrintTxt.setText(R.string.ui_printed_today);
            holder.binding.qrTokenTxt.setText(R.string.ui_printed_today);
        } else {
            holder.binding.billPrintTxt.setText(R.string.ui_print_coupon);
            holder.binding.qrTokenTxt.setText(R.string.ui_print_qr_token);
        }
    }

    private int resolveAllowedMessDays(MemberResponse memberResponse) {
        String totalDays = memberResponse.getMessTotalDays();
        if (totalDays != null && totalDays.equalsIgnoreCase("One Time")) {
            return 1;
        }
        return 2;
    }

    private float parseFloatSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (Exception e) {
            return 0f;
        }
    }

    private int parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public void setBillPrintPassword(MemberResponse memberResponse, boolean issueQrToken) {
        Activity activity = (Activity) context;
        View content = LayoutInflater.from(activity).inflate(R.layout.report_password_dialog, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(activity, content, false);

        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextInputEditText reportPin = content.findViewById(R.id.reportPin);
        TextView detailsTxt = content.findViewById(R.id.details);
        detailsTxt.setText(issueQrToken ? "QR Token Password" : "Bill Print Password");

        dismissReport.setOnClickListener(v -> sheet.dismiss());

        continueToReport.setOnClickListener(v -> {
            if (reportPin.getText().toString().equalsIgnoreCase(memberResponse.getMemberMobileNumber())) {
                sheet.dismiss();

                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String paymentDate = df.format(c);

                messInvoiceResponseList = posBillingWalaDatabase.gerMessInvoiceUserWiseList(memberResponse.getMemberName(), paymentDate);
                int messDays = resolveAllowedMessDays(memberResponse);

                if (messDays > messInvoiceResponseList.size()) {
                    if (issueQrToken) {
                        openMemberQrToken(memberResponse, messInvoiceResponseList.size());
                    } else {
                        Intent intent = new Intent(context, CouponBluetoothPrint.class);
                        intent.putExtra("invoiceRunningStatus", "printBill");
                        intent.putExtra("cartOrderStatus", "mess");
                        intent.putExtra("memberId", memberResponse.getMemberId());
                        intent.putExtra("memberName", memberResponse.getMemberName());
                        intent.putExtra("memberMobileNumber", memberResponse.getMemberMobileNumber());
                        intent.putExtra("messDays", "" + messDays);
                        intent.putExtra("messInvoiceResponseList", "" + messInvoiceResponseList.size());
                        context.startActivity(intent);
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.toast_already_coupon_created), Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                }

            } else {
                reportPin.requestFocus();
                reportPin.setError("Enter correct pin");
            }
        });
    }

    private void openMemberQrToken(MemberResponse memberResponse, int existingCouponCount) {
        String tokenCode = MessTokenQrHelper.generateTokenCode();
        String messType = MessTokenQrHelper.resolveMessType();
        if (existingCouponCount == 1) {
            messType = "Dinner";
        }
        String networkStatus = getRandomString(10);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String tokenDate = df.format(Calendar.getInstance().getTime());

        Intent intent = new Intent(context, MessTokenBluetoothPrint.class);
        intent.putExtra("tokenCode", tokenCode);
        intent.putExtra("memberId", memberResponse.getMemberId());
        intent.putExtra("memberName", memberResponse.getMemberName());
        intent.putExtra("memberMobile", memberResponse.getMemberMobileNumber());
        intent.putExtra("memberType", MessTokenQrHelper.MEMBER_TYPE_MEMBER);
        intent.putExtra("messType", messType);
        intent.putExtra("tokenAmount", "0");
        intent.putExtra("tokenDate", tokenDate);
        intent.putExtra("tokenNetworkStatus", networkStatus);
        intent.putExtra("messInvoiceResponseList", "" + existingCouponCount);
        context.startActivity(intent);
    }

    private String getRandomString(final int sizeOfRandomString) {
        String allowed = "0123456789qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; i++) {
            sb.append(allowed.charAt(random.nextInt(allowed.length())));
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return memberResponseList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public final MemberInvoiceListBinding binding;

        public MyViewHolder(MemberInvoiceListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
