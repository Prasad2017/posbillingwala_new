package com.pos_billingwala.PaymentDisplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.pos_billingwala.Activity.PaymentDisplaySettingsActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.PaymentUpiQrHelper;
import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.InvoiceResponse;
import com.pos_billingwala.R;

import java.util.List;
import java.util.Locale;

/**
 * Orchestrates Show QR for a specific invoice (auto or manual). Never blocks billing.
 */
public final class PaymentDisplayService {

    private static final String TAG = "PaymentDisplay";

    private PaymentDisplayService() {
    }

    /** Fire-and-forget after successful new bill save. */
    public static void tryAutoShowAfterBill(Context context, String invoiceNumber, double amount) {
        try {
            DisplayConnectionManager mgr = DisplayConnectionManager.get(context);
            if (!mgr.isAutoDisplayEnabled()) {
                return;
            }
            if (!mgr.isConnected()) {
                return;
            }
            showBill(context, invoiceNumber, invoiceNumber, amount, "completed");
        } catch (Exception e) {
            Log.e(TAG, "PAYMENT_DISPLAY_ERROR auto_show", e);
        }
    }

    public static DisplayConnectionManager.ShowResult showInvoice(
            Context context, InvoiceResponse invoice) {
        if (invoice == null) {
            return DisplayConnectionManager.ShowResult.fail(
                    DisplayConnectionManager.ShowResult.Code.INVOICE_INVALID,
                    "Cannot display QR for this invoice.");
        }
        String status = invoice.getInvoiceOrderStatus() != null
                ? invoice.getInvoiceOrderStatus().trim().toLowerCase(Locale.US) : "";
        if ("cancelled".equals(status) || invoice.isRefunded()) {
            return DisplayConnectionManager.ShowResult.fail(
                    DisplayConnectionManager.ShowResult.Code.INVOICE_INVALID,
                    "Cannot display QR for voided or refunded bills.");
        }
        double amount = parseAmount(invoice.getTotalAmount());
        return showBill(context, invoice.getInvoiceId(), invoice.getInvoiceNumber(), amount, status);
    }

    public static DisplayConnectionManager.ShowResult showBill(
            Context context,
            String billId,
            String billNumber,
            double amount,
            String invoiceOrderStatus
    ) {
        try {
            String status = invoiceOrderStatus != null
                    ? invoiceOrderStatus.trim().toLowerCase(Locale.US) : "";
            if ("cancelled".equals(status) || "refunded".equals(status)) {
                return DisplayConnectionManager.ShowResult.fail(
                        DisplayConnectionManager.ShowResult.Code.INVOICE_INVALID,
                        "Cannot display QR for voided or refunded bills.");
            }
            if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
                return DisplayConnectionManager.ShowResult.fail(
                        DisplayConnectionManager.ShowResult.Code.INVALID_AMOUNT,
                        "Invalid payment amount.");
            }

            POSBillingWalaDatabase db = new POSBillingWalaDatabase(context);
            List<CompanyResponse> companies = db.getCompanyDetails();
            if (companies == null || companies.isEmpty()) {
                return DisplayConnectionManager.ShowResult.fail(
                        DisplayConnectionManager.ShowResult.Code.UPI_NOT_CONFIGURED,
                        "UPI payment is not configured.");
            }
            CompanyResponse company = companies.get(0);
            String upiId = company.getPaymentLogo();
            if (!PaymentUpiQrHelper.isUpiId(upiId)) {
                return DisplayConnectionManager.ShowResult.fail(
                        DisplayConnectionManager.ShowResult.Code.UPI_NOT_CONFIGURED,
                        "UPI payment is not configured.");
            }
            String payee = ShopHeaderBuilder.resolveShopName1(company);
            if (payee == null || payee.trim().isEmpty()) {
                payee = "Merchant";
            }
            String uri = PaymentUpiQrHelper.buildUpiPayUri(upiId, payee, amount, billNumber);
            String svg = PaymentDisplayQrSvg.fromPayload(uri);
            if (svg == null || svg.isEmpty()) {
                return DisplayConnectionManager.ShowResult.fail(
                        DisplayConnectionManager.ShowResult.Code.QR_FAILED,
                        "QR generation failed.");
            }

            DisplayConnectionManager mgr = DisplayConnectionManager.get(context);
            long now = System.currentTimeMillis();
            long expires = now + mgr.getQrDurationSeconds() * 1000L;
            PaymentDisplayBillPayload payload = new PaymentDisplayBillPayload(
                    billId != null ? billId : billNumber,
                    billNumber,
                    Double.parseDouble(String.format(Locale.US, "%.2f", amount)),
                    "INR",
                    payee,
                    upiId.trim(),
                    payee,
                    uri,
                    svg,
                    now,
                    expires
            );
            return mgr.publishBill(payload);
        } catch (Exception e) {
            Log.e(TAG, "PAYMENT_DISPLAY_ERROR show_bill", e);
            return DisplayConnectionManager.ShowResult.fail(
                    DisplayConnectionManager.ShowResult.Code.ERROR,
                    "Could not show payment QR.");
        }
    }

    /** UI helper for Invoice list/detail Show QR. */
    public static void requestShowInvoiceQr(Activity activity, InvoiceResponse invoice) {
        if (activity == null || invoice == null) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.payment_display_show_qr)
                .setMessage(R.string.payment_display_already_paid_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.payment_display_show_qr, (d, w) -> {
                    DisplayConnectionManager.ShowResult result = showInvoice(activity, invoice);
                    handleShowResult(activity, result);
                })
                .show();
    }

    public static void handleShowResult(Activity activity, DisplayConnectionManager.ShowResult result) {
        if (activity == null || result == null) {
            return;
        }
        switch (result.code) {
            case SUCCESS:
                Toast.makeText(activity, R.string.payment_display_qr_sent, Toast.LENGTH_SHORT).show();
                break;
            case NOT_CONNECTED:
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.payment_display_title)
                        .setMessage(R.string.payment_display_not_connected)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.payment_display_connect, (d, w) ->
                                activity.startActivity(new Intent(activity, PaymentDisplaySettingsActivity.class)))
                        .show();
                break;
            case UPI_NOT_CONFIGURED:
                Toast.makeText(activity, R.string.payment_display_upi_not_configured, Toast.LENGTH_LONG).show();
                break;
            case INVALID_AMOUNT:
                Toast.makeText(activity, R.string.payment_display_invalid_amount, Toast.LENGTH_SHORT).show();
                break;
            case INVOICE_INVALID:
                Toast.makeText(activity, R.string.payment_display_invoice_invalid, Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(activity,
                        result.message != null ? result.message
                                : activity.getString(R.string.payment_display_error),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private static double parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(raw.trim().replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
