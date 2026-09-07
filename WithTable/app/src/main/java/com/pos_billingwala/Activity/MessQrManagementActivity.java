package com.pos_billingwala.Activity;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import android.content.Intent;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.MessQrInfo;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.PrinterConnectionHelper;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessQrManagementActivity extends BaseActivity {

    private ImageView qrImage;
    private TextView qrStatus, qrMeta;
    private Button btnGenerate, btnShare, btnDownload, btnPrintQr, btnDeactivate;
    private MessQrInfo currentQr;
    private Bitmap currentBitmap;
    private String messLabel = "Mess";
    private String branchLabel = "Main Branch";
    private final List<PrinterSettingResponse> printerSettingResponseList = new ArrayList<>();
    private final ExecutorService printExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.pos_billingwala.Extra.MessModule.ensureEnabled(this)) {
            return;
        }
        setContentView(R.layout.activity_mess_qr_management);

        qrImage = findViewById(R.id.qrImage);
        qrStatus = findViewById(R.id.qrStatus);
        qrMeta = findViewById(R.id.qrMeta);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnShare = findViewById(R.id.btnShare);
        btnDownload = findViewById(R.id.btnDownload);
        btnPrintQr = findViewById(R.id.btnPrintQr);
        btnDeactivate = findViewById(R.id.btnDeactivate);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        POSBillingWalaDatabase db = new POSBillingWalaDatabase(this);
        List<CompanyResponse> companies = db.getCompanyDetails();
        if (companies != null && !companies.isEmpty()) {
            CompanyResponse c = companies.get(0);
            if (c.getCompanyName() != null && !c.getCompanyName().trim().isEmpty()) {
                messLabel = c.getCompanyName();
            }
        }
        List<PrinterSettingResponse> printers = db.getPrinterSettingDetails();
        if (printers != null) {
            printerSettingResponseList.clear();
            printerSettingResponseList.addAll(printers);
        }

        btnGenerate.setOnClickListener(v -> generateOrRefresh());
        btnDeactivate.setOnClickListener(v -> setStatus("INACTIVE"));
        btnShare.setOnClickListener(v -> shareQr());
        btnDownload.setOnClickListener(v -> downloadQr());
        btnPrintQr.setOnClickListener(v -> printQrOnPrinter());

        loadQr();
    }

    @Override
    protected void onDestroy() {
        printExecutor.shutdownNow();
        super.onDestroy();
    }

    private String deviceId() {
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return id != null ? id : "";
    }

    private void loadQr() {
        Api.getClient(this).getMessQr(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().messQr != null) {
                    bindQr(response.body().messQr);
                } else {
                    qrStatus.setText("Status: —");
                    qrMeta.setText(getString(R.string.ui_no_active_qr));
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Toast.makeText(MessQrManagementActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateOrRefresh() {
        Api.getClient(this).generateMessQr(MainActivity.userId, deviceId(), messLabel, branchLabel)
                .enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && "1".equals(response.body().status)
                        && response.body().messQr != null) {
                    bindQr(response.body().messQr);
                    Toast.makeText(MessQrManagementActivity.this, R.string.ui_qr_generated, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MessQrManagementActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Toast.makeText(MessQrManagementActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setStatus(String status) {
        Api.getClient(this).setMessQrStatus(MainActivity.userId, status, deviceId())
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        loadQr();
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                    }
                });
    }

    private void bindQr(MessQrInfo qr) {
        currentQr = qr;
        qrStatus.setText("Status: " + (qr.status != null ? qr.status : ""));
        String mess = qr.messLabel != null && !qr.messLabel.isEmpty() ? qr.messLabel : messLabel;
        String branch = qr.branchLabel != null && !qr.branchLabel.isEmpty() ? qr.branchLabel : branchLabel;
        messLabel = mess;
        qrMeta.setText("Mess: " + mess + "\nBranch: " + branch);
        if (qr.qrUrl != null) {
            currentBitmap = MessTokenQrHelper.generateBrandedMessQr(this, qr.qrUrl, 900);
            if (currentBitmap != null) {
                qrImage.setImageBitmap(currentBitmap);
            }
        }
    }

    private String resolvedMessName() {
        if (currentQr != null && currentQr.messLabel != null && !currentQr.messLabel.trim().isEmpty()) {
            return currentQr.messLabel.trim();
        }
        return messLabel != null ? messLabel : "Mess";
    }

    /** QR image with bold CAPITAL mess name — no URL text. */
    private Bitmap qrSharePrintBitmap(boolean forPrint) {
        Bitmap qrOnly;
        if (forPrint) {
            qrOnly = qrOnlyBitmapForPrint();
        } else {
            qrOnly = qrOnlyBitmap();
        }
        return MessTokenQrHelper.composeMessQrWithTitle(qrOnly, resolvedMessName());
    }

    /** QR image only (branded) — no title / caption / URL text. */
    private Bitmap qrOnlyBitmap() {
        if (currentBitmap != null) {
            return currentBitmap;
        }
        if (currentQr != null && currentQr.qrUrl != null) {
            currentBitmap = MessTokenQrHelper.generateBrandedMessQr(this, currentQr.qrUrl, 900);
        }
        return currentBitmap;
    }

    /** Black branded QR for thermal printer contrast. */
    private Bitmap qrOnlyBitmapForPrint() {
        if (currentQr == null || currentQr.qrUrl == null) {
            return null;
        }
        return MessTokenQrHelper.generateBrandedMessQrForPrint(this, currentQr.qrUrl, 900);
    }

    private void shareQr() {
        Bitmap composed = qrSharePrintBitmap(false);
        if (composed == null) {
            Toast.makeText(this, R.string.ui_no_active_qr, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(getCacheDir(), "share");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "mess_qr.png");
            FileOutputStream fos = new FileOutputStream(file);
            composed.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.ui_share_qr)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share QR.", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadQr() {
        Bitmap composed = qrSharePrintBitmap(false);
        if (composed == null) {
            Toast.makeText(this, R.string.ui_no_active_qr, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "billingwala_mess_qr.png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Billingwala");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    composed.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                }
                Toast.makeText(this, R.string.ui_qr_saved, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to save QR.", Toast.LENGTH_SHORT).show();
        }
    }

    private void printQrOnPrinter() {
        Bitmap composed = qrSharePrintBitmap(true);
        if (composed == null) {
            Toast.makeText(this, R.string.ui_no_active_qr, Toast.LENGTH_SHORT).show();
            return;
        }
        if (printerSettingResponseList.isEmpty()) {
            POSBillingWalaDatabase db = new POSBillingWalaDatabase(this);
            List<PrinterSettingResponse> printers = db.getPrinterSettingDetails();
            if (printers != null) {
                printerSettingResponseList.clear();
                printerSettingResponseList.addAll(printers);
            }
        }
        if (printerSettingResponseList.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
            return;
        }
        String addr = printerSettingResponseList.get(0).getBluetoothAddress();
        if (addr == null || addr.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_please_select_printer_from_setting), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, getString(R.string.toast_printing_in_progress), Toast.LENGTH_SHORT).show();
        PrinterConnectionHelper.ensureBillPrinterAsync(this, addr, () -> printExecutor.execute(() -> {
            boolean ok = writeQrToBillPrinter(composed);
            runOnUiThread(() -> Toast.makeText(MessQrManagementActivity.this,
                    ok ? "QR printed" : "Unable to print QR. Check printer.",
                    Toast.LENGTH_SHORT).show());
        }));
    }

    private boolean writeQrToBillPrinter(Bitmap qrOnly) {
        try {
            if (printerSettingResponseList.isEmpty()) {
                return false;
            }
            PrinterSettingResponse setting = printerSettingResponseList.get(0);
            int dots = 48;
            if (setting.getPrinterName() != null && setting.getPrinterName().equalsIgnoreCase("3-Inch")) {
                dots = 72;
            }
            Bitmap resized = getResizedBitmap(qrOnly, dots);
            PrintImage printImage = new PrintImage(resized);
            printImage.PrepareImage(PrintImage.dither.floyd_steinberg, 128);
            if (!PrinterConnectionHelper.safeWriteBill(this, printImage.getPrintImageData())) {
                return false;
            }
            feedPaper(setting.getPrinterFeedLines());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Bitmap getResizedBitmap(Bitmap bm, int effectivePrintWidth) {
        int reqWidth = Math.round(effectivePrintWidth * 8);
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width == reqWidth) {
            return bm;
        }
        int newWidth = reqWidth;
        int newHeight = Math.max(1, reqWidth * height / width);
        Matrix matrix = new Matrix();
        matrix.postScale((float) newWidth / width, (float) newHeight / height);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    private void feedPaper(String lines) {
        try {
            if (lines == null || lines.trim().isEmpty()) {
                return;
            }
            StringBuilder lineBreaks = new StringBuilder();
            for (int i = 0; i < Integer.parseInt(lines); i++) {
                lineBreaks.append("\n");
            }
            PrinterConnectionHelper.safeWriteBill(this, lineBreaks.toString().getBytes());
        } catch (Exception ignored) {
        }
    }
}
