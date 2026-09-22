package com.pos_billingwala.Activity;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.Extra.TabletPrintUi;
import com.pos_billingwala.Model.MessTokenResponse;
import com.pos_billingwala.R;
import com.pos_billingwala.Retrofit.Api;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.databinding.ActivityMessTokenScanBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessTokenScanActivity extends BaseActivity implements View.OnClickListener {

    private static final int CAMERA_PERMISSION_REQUEST = 501;

    private ActivityMessTokenScanBinding binding;
    private POSBillingWalaDatabase database;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessTokenScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database = new POSBillingWalaDatabase(this);
        binding.startScanCardView.setOnClickListener(this);
        TabletPrintUi.applyCenteredForm(this, binding.messScanContainer);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.startScanCardView) {
            startScanner();
        }
    }

    private void startScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        launchScanner();
    }

    private void launchScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan mess token QR code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            Toast.makeText(this, getString(R.string.toast_camera_permission_is_required_to_scan_qr), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, getString(R.string.toast_scan_cancelled), Toast.LENGTH_SHORT).show();
            } else {
                verifyScannedPayload(result.getContents());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void verifyScannedPayload(String rawPayload) {
        String[] parsed = MessTokenQrHelper.parsePayload(rawPayload);
        if (parsed == null) {
            showResult(false, "Invalid QR code format");
            return;
        }

        String tokenCode = parsed[0];
        String qrUserId = parsed[1];

        if (MainActivity.userId != null && !MainActivity.userId.equals(qrUserId)) {
            showResult(false, "Token belongs to another shop");
            return;
        }

        MessTokenResponse token = database.getMessTokenByCode(tokenCode);
        if (token == null) {
            showResult(false, "Token not found on this device");
            return;
        }

        if (MessTokenQrHelper.TOKEN_STATE_VERIFIED.equalsIgnoreCase(token.getTokenState())) {
            showResult(false, "Token already verified\n" + token.getMemberName());
            return;
        }

        if (!MessTokenQrHelper.MEMBER_TYPE_MEMBER.equalsIgnoreCase(token.getMemberType())) {
            showResult(false, "Only registered member tokens are allowed");
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        String tokenDay = token.getTokenDate() != null && token.getTokenDate().length() >= 10
                ? token.getTokenDate().substring(0, 10) : "";
        if (!today.equals(tokenDay)) {
            showResult(false, "Token expired (not valid for today)");
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String verifiedDate = df.format(Calendar.getInstance().getTime());
        String verifyNetworkStatus = getRandomString(10);

        database.markMessTokenVerified(token.getTokenId(), verifiedDate, verifyNetworkStatus);

        syncVerifyToServer(tokenCode, verifiedDate, verifyNetworkStatus);

        showResult(true, "Verified: " + token.getMemberName()
                + "\n" + token.getMessType()
                + "\nMember");
    }

    private void syncVerifyToServer(String tokenCode, String verifiedDate, String verifyNetworkStatus) {
        Call<AllApiResponse> call = Api.getClient(this).verifyMessToken(
                MainActivity.userId, tokenCode, verifiedDate, verifyNetworkStatus);
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "1".equalsIgnoreCase(response.body().getStatus())) {
                    MessTokenResponse token = database.getMessTokenByCode(tokenCode);
                    if (token != null) {
                        database.updateSyncMessTokenVerify(token.getTokenId());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                // Offline verify is kept locally; sync will retry via receiver
            }
        });
    }

    private void showResult(boolean success, String message) {
        binding.scanResultText.setVisibility(View.VISIBLE);
        binding.scanResultText.setText(message);
        binding.scanResultText.setTextColor(getColor(success ? R.color.colorPrimary : android.R.color.holo_red_dark));

        BottomSheetUi.showAction(
                this,
                success ? "Token Verified" : "Verification Failed",
                message,
                "OK",
                "Scan Again",
                0,
                true,
                null,
                this::startScanner);
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
}
