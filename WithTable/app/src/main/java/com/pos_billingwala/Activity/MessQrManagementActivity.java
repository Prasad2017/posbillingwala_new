package com.pos_billingwala.Activity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.CompanyResponse;
import com.pos_billingwala.Model.MessQrInfo;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessQrManagementActivity extends BaseActivity {

    private ImageView qrImage;
    private TextView qrStatus, qrMeta, qrUrl;
    private Button btnGenerate, btnShare, btnDownload, btnPrintQr, btnRegenerate, btnDeactivate;
    private MessQrInfo currentQr;
    private Bitmap currentBitmap;
    private String messLabel = "Mess";
    private String branchLabel = "Main Branch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_qr_management);

        qrImage = findViewById(R.id.qrImage);
        qrStatus = findViewById(R.id.qrStatus);
        qrMeta = findViewById(R.id.qrMeta);
        qrUrl = findViewById(R.id.qrUrl);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnShare = findViewById(R.id.btnShare);
        btnDownload = findViewById(R.id.btnDownload);
        btnPrintQr = findViewById(R.id.btnPrintQr);
        btnRegenerate = findViewById(R.id.btnRegenerate);
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

        btnGenerate.setOnClickListener(v -> generateOrRefresh(false));
        btnRegenerate.setOnClickListener(v -> generateOrRefresh(true));
        btnDeactivate.setOnClickListener(v -> setStatus("INACTIVE"));
        btnShare.setOnClickListener(v -> shareQr());
        btnDownload.setOnClickListener(v -> downloadQr());
        btnPrintQr.setOnClickListener(v -> printPoster());

        loadQr();
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

    private void generateOrRefresh(boolean regenerate) {
        Call<AllApiResponse> call = regenerate
                ? Api.getClient(this).regenerateMessQr(MainActivity.userId, deviceId(), messLabel, branchLabel)
                : Api.getClient(this).generateMessQr(MainActivity.userId, deviceId(), messLabel, branchLabel);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && "1".equals(response.body().status)
                        && response.body().messQr != null) {
                    bindQr(response.body().messQr);
                    Toast.makeText(MessQrManagementActivity.this,
                            regenerate ? R.string.ui_qr_regenerated : R.string.ui_qr_generated,
                            Toast.LENGTH_SHORT).show();
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
        qrMeta.setText("Mess: " + mess + "\nBranch: " + branch);
        qrUrl.setText(qr.qrUrl != null ? qr.qrUrl : "");
        if (qr.qrUrl != null) {
            currentBitmap = MessTokenQrHelper.generateQrBitmap(qr.qrUrl, 768);
            if (currentBitmap != null) {
                qrImage.setImageBitmap(currentBitmap);
            }
        }
    }

    private Bitmap buildPoster() {
        int w = 900;
        int h = 1200;
        Bitmap poster = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(poster);
        canvas.drawColor(Color.WHITE);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.BLACK);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(48);
        canvas.drawText("BILLINGWALA", w / 2f, 90, p);
        p.setTextSize(40);
        canvas.drawText("MESS TOKEN", w / 2f, 150, p);
        Bitmap qr = currentBitmap != null ? currentBitmap : MessTokenQrHelper.generateQrBitmap(
                currentQr != null ? currentQr.qrUrl : "", 640);
        if (qr != null) {
            canvas.drawBitmap(qr, (w - qr.getWidth()) / 2f, 220, null);
        }
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        p.setTextSize(28);
        canvas.drawText("Scan using your phone camera", w / 2f, 920, p);
        canvas.drawText("No App Required", w / 2f, 970, p);
        return poster;
    }

    private void shareQr() {
        if (currentQr == null || currentQr.qrUrl == null) {
            Toast.makeText(this, R.string.ui_no_active_qr, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap poster = buildPoster();
            File dir = new File(getCacheDir(), "share");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "mess_qr.png");
            FileOutputStream fos = new FileOutputStream(file);
            poster.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, currentQr.qrUrl);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.ui_share_qr)));
        } catch (Exception e) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, currentQr.qrUrl);
            startActivity(Intent.createChooser(share, getString(R.string.ui_share_qr)));
        }
    }

    private void downloadQr() {
        if (currentBitmap == null) {
            Toast.makeText(this, R.string.ui_no_active_qr, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap poster = buildPoster();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "billingwala_mess_qr.png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Billingwala");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    poster.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                }
                Toast.makeText(this, R.string.ui_qr_saved, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to save QR.", Toast.LENGTH_SHORT).show();
        }
    }

    private void printPoster() {
        if (currentQr == null) {
            Toast.makeText(this, R.string.ui_no_active_qr, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap poster = buildPoster();
            File dir = new File(getCacheDir(), "share");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "mess_qr_print.png");
            FileOutputStream fos = new FileOutputStream(file);
            poster.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent printIntent = new Intent(Intent.ACTION_SEND);
            printIntent.setType("image/png");
            printIntent.putExtra(Intent.EXTRA_STREAM, uri);
            printIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(printIntent, getString(R.string.ui_print_qr)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to print QR.", Toast.LENGTH_SHORT).show();
        }
    }
}
