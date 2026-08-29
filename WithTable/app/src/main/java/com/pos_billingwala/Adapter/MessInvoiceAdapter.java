package com.pos_billingwala.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.pos_billingwala.Activity.CouponBluetoothPrint;
import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Activity.MessTokenBluetoothPrint;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
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
    int messDays = 0;


    public MessInvoiceAdapter(Context context, List<MemberResponse> memberResponseList) {
        this.context = context;
        this.memberResponseList = memberResponseList;
    }

    @NonNull
    @Override
    public MessInvoiceAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(MemberInvoiceListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessInvoiceAdapter.MyViewHolder holder, int position) {

        MemberResponse memberResponse = memberResponseList.get(position);
        posBillingWalaDatabase = new POSBillingWalaDatabase(context);

        holder.binding.memberName.setText(memberResponse.getMemberName());

        try {

            pendingAmount = Float.parseFloat(memberResponse.getPaymentMessAmount()) - Float.parseFloat(memberResponse.getPaymentPaidAmount());

            if (pendingAmount > 0) {
                holder.binding.pendingAmount.setVisibility(View.VISIBLE);
            } else {
                holder.binding.pendingAmount.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.binding.pendingAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.pending_amount_dialog, null);
                mypopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);

                try {
                    pendingAmount = Float.parseFloat(memberResponse.getPaymentMessAmount()) - Float.parseFloat(memberResponse.getPaymentPaidAmount());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TextView pendingAmountLayout = view.findViewById(R.id.pendingAmountLayout);
                TextView pendingAmountTxt = view.findViewById(R.id.pendingAmount);
                pendingAmountTxt.setText(MainActivity.currencyName + " " + pendingAmount);

                pendingAmountLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mypopupWindow.dismiss();
                    }
                });

                mypopupWindow.showAsDropDown(holder.binding.pendingAmount, 0, -75);

            }
        });

        holder.binding.billPrintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBillPrintPassword(memberResponse, false);

            }
        });

        holder.binding.qrTokenLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBillPrintPassword(memberResponse, true);
            }
        });

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
                System.out.println("Current time => " + c);
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String paymentDate = df.format(c);

                messInvoiceResponseList = posBillingWalaDatabase.gerMessInvoiceUserWiseList(memberResponse.getMemberName(), paymentDate);

                if (memberResponse.getMessTotalDays().equalsIgnoreCase("One Time")) {
                    messDays = 1;
                } else {
                    messDays = 2;
                }

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
