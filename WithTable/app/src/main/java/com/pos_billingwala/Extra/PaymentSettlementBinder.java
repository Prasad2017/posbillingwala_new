package com.pos_billingwala.Extra;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.pos_billingwala.R;

import java.util.Locale;

/**
 * Wires Cash / UPI / Cash+UPI settlement UI on {@code set_payment_mode_dialog}.
 */
public final class PaymentSettlementBinder {

    public interface Callback {
        void onConfirmed(String mode, String cashAmount, String upiAmount);

        void onDismissed();
    }

    private PaymentSettlementBinder() {
    }

    public static void bind(View content, float totalAmt, String currency,
                            String presetMode, Callback callback) {
        TextView continueToReport = content.findViewById(R.id.continueToReport);
        TextView dismissReport = content.findViewById(R.id.dismissReport);
        TextView totalAmount = content.findViewById(R.id.totalAmount);
        RadioGroup paymentGroup = content.findViewById(R.id.paymentGroup);
        LinearLayout splitLayout = content.findViewById(R.id.splitPaymentLayout);
        EditText cashInput = content.findViewById(R.id.cashAmountInput);
        EditText upiInput = content.findViewById(R.id.upiAmountInput);
        TextView settlementTotal = content.findViewById(R.id.settlementTotal);
        TextView settlementStatus = content.findViewById(R.id.settlementStatus);

        String symbol = currency != null ? currency : "";
        totalAmount.setText("Total Amount: " + symbol + String.format(Locale.US, "%.2f", totalAmt));

        final boolean[] updating = {false};

        Runnable refreshSplit = () -> {
            if (splitLayout == null) {
                return;
            }
            int checked = paymentGroup.getCheckedRadioButtonId();
            boolean split = checked == R.id.splitCashUpi;
            splitLayout.setVisibility(split ? View.VISIBLE : View.GONE);
            if (!split) {
                if (settlementStatus != null) {
                    settlementStatus.setVisibility(View.GONE);
                }
                return;
            }
            float cash = PaymentSettlementHelper.parseAmount(
                    cashInput != null && cashInput.getText() != null ? cashInput.getText().toString() : "");
            float upi = PaymentSettlementHelper.parseAmount(
                    upiInput != null && upiInput.getText() != null ? upiInput.getText().toString() : "");
            float sum = cash + upi;
            if (settlementTotal != null) {
                settlementTotal.setText(content.getContext().getString(R.string.ui_total_settlement)
                        + ": " + symbol + PaymentSettlementHelper.formatAmount(sum));
            }
            if (settlementStatus != null) {
                boolean matched = PaymentSettlementHelper.amountsMatch(cash, upi, totalAmt);
                settlementStatus.setVisibility(View.VISIBLE);
                settlementStatus.setText(matched
                        ? content.getContext().getString(R.string.ui_settlement_matched)
                        : content.getContext().getString(R.string.ui_settlement_not_matched));
                settlementStatus.setTextColor(matched ? 0xFF2E7D32 : 0xFFC62828);
            }
        };

        TextWatcher remainderWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updating[0]) {
                    return;
                }
                refreshSplit.run();
            }
        };
        if (cashInput != null) {
            cashInput.addTextChangedListener(remainderWatcher);
        }
        if (upiInput != null) {
            upiInput.addTextChangedListener(remainderWatcher);
        }

        paymentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.splitCashUpi) {
                if (cashInput != null && (cashInput.getText() == null || cashInput.getText().toString().trim().isEmpty())) {
                    updating[0] = true;
                    cashInput.setText("");
                    updating[0] = false;
                }
            }
            refreshSplit.run();
        });

        String mode = PaymentSettlementHelper.canonicalMode(presetMode);
        if (PaymentSettlementHelper.isSplit(mode)) {
            paymentGroup.check(R.id.splitCashUpi);
        } else if (PaymentSettlementHelper.isUpi(mode)) {
            paymentGroup.check(R.id.online);
        } else if (PaymentSettlementHelper.isCash(mode) || !mode.isEmpty()) {
            if (mode.isEmpty()) {
                paymentGroup.clearCheck();
            } else if (PaymentSettlementHelper.MODE_BANK.equals(mode)) {
                paymentGroup.check(R.id.cash);
            } else {
                paymentGroup.check(R.id.cash);
            }
        }
        refreshSplit.run();

        dismissReport.setOnClickListener(v -> {
            if (callback != null) {
                callback.onDismissed();
            }
        });

        continueToReport.setOnClickListener(v -> {
            int selectedId = paymentGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                if (callback != null) {
                    callback.onConfirmed("", "", "");
                }
                return;
            }
            String selectedMode;
            String cash;
            String upi;
            if (selectedId == R.id.splitCashUpi) {
                selectedMode = PaymentSettlementHelper.MODE_SPLIT;
                cash = cashInput != null && cashInput.getText() != null
                        ? cashInput.getText().toString().trim() : "";
                upi = upiInput != null && upiInput.getText() != null
                        ? upiInput.getText().toString().trim() : "";
                float cashVal = PaymentSettlementHelper.parseAmount(cash);
                float upiVal = PaymentSettlementHelper.parseAmount(upi);
                if (cashVal <= 0f || upiVal <= 0f
                        || !PaymentSettlementHelper.amountsMatch(cashVal, upiVal, totalAmt)) {
                    if (settlementStatus != null) {
                        settlementStatus.setVisibility(View.VISIBLE);
                        settlementStatus.setText(content.getContext().getString(
                                R.string.toast_settlement_must_match));
                        settlementStatus.setTextColor(0xFFC62828);
                    }
                    return;
                }
                cash = PaymentSettlementHelper.formatAmount(cashVal);
                upi = PaymentSettlementHelper.formatAmount(upiVal);
            } else if (selectedId == R.id.online) {
                selectedMode = PaymentSettlementHelper.MODE_UPI;
                cash = PaymentSettlementHelper.formatAmount(0f);
                upi = PaymentSettlementHelper.formatAmount(totalAmt);
            } else {
                RadioButton radio = content.findViewById(selectedId);
                String label = radio != null ? radio.getText().toString() : PaymentSettlementHelper.MODE_CASH;
                selectedMode = PaymentSettlementHelper.canonicalMode(label);
                if (selectedMode.isEmpty()) {
                    selectedMode = PaymentSettlementHelper.MODE_CASH;
                }
                PaymentSettlementHelper.Tender tender =
                        PaymentSettlementHelper.resolve(selectedMode, totalAmt, null, null);
                cash = tender.cashAmount;
                upi = tender.upiAmount;
            }
            if (callback != null) {
                callback.onConfirmed(selectedMode, cash, upi);
            }
        });
    }
}
