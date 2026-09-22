package com.pos_billingwala.Activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.pos_billingwala.Extra.MessTokenQrHelper;
import com.pos_billingwala.PaymentDisplay.DisplayConnectionManager;
import com.pos_billingwala.PaymentDisplay.PaymentDisplayPrefs;
import com.pos_billingwala.R;

/**
 * Settings → Payment Display: start local server, show pairing QR, auto-display toggle.
 */
public class PaymentDisplaySettingsActivity extends AppCompatActivity
        implements DisplayConnectionManager.Listener {

    private DisplayConnectionManager manager;
    private TextView statusText;
    private TextView deviceText;
    private TextView localUrlText;
    private TextView pairingUrlText;
    private ImageView pairingQrImage;
    private SwitchCompat autoSwitch;
    private View pairingCard;
    private MaterialButton disconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_display_settings);

        manager = DisplayConnectionManager.get(this);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.toolbarTitle)).setText(R.string.payment_display_title);

        statusText = findViewById(R.id.statusText);
        deviceText = findViewById(R.id.deviceText);
        localUrlText = findViewById(R.id.localUrlText);
        pairingUrlText = findViewById(R.id.pairingUrlText);
        pairingQrImage = findViewById(R.id.pairingQrImage);
        autoSwitch = findViewById(R.id.autoDisplaySwitch);
        pairingCard = findViewById(R.id.pairingCard);
        TextView durationText = findViewById(R.id.durationText);

        durationText.setText(getString(R.string.payment_display_duration_value,
                PaymentDisplayPrefs.DEFAULT_QR_DURATION_SEC / 60));

        autoSwitch.setChecked(manager.isAutoDisplayEnabled());
        autoSwitch.setOnCheckedChangeListener((btn, checked) ->
                manager.setAutoDisplayEnabled(checked));

        MaterialButton showPairing = findViewById(R.id.showPairingButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        MaterialButton copyUrl = findViewById(R.id.copyUrlButton);

        showPairing.setOnClickListener(v -> manager.showPairingQr());
        disconnectButton.setOnClickListener(v -> manager.disconnectDisplays());
        disconnectButton.setVisibility(View.GONE);
        copyUrl.setOnClickListener(v -> {
            String url = manager.getPairingUrl();
            if (url == null || url.isEmpty()) {
                return;
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("pairing", url));
                Toast.makeText(this, R.string.payment_display_url_copied, Toast.LENGTH_SHORT).show();
            }
        });

        refreshUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        manager.addListener(this);
        refreshUi();
    }

    @Override
    protected void onStop() {
        manager.removeListener(this);
        super.onStop();
    }

    @Override
    public void onDisplayStateChanged() {
        runOnUiThread(this::refreshUi);
    }

    private void refreshUi() {
        DisplayConnectionManager.Status status = manager.getStatus();
        String statusLabel;
        switch (status) {
            case CONNECTED:
                statusLabel = getString(R.string.payment_display_connected)
                        + " (" + manager.getConnectedClients() + ")";
                break;
            case WAITING_FOR_PAIR:
                statusLabel = getString(R.string.payment_display_waiting_pair);
                break;
            case STARTING:
                statusLabel = getString(R.string.payment_display_starting);
                break;
            case ERROR:
                statusLabel = getString(R.string.payment_display_error);
                break;
            default:
                statusLabel = getString(R.string.payment_display_stopped);
                break;
        }
        statusText.setText(getString(R.string.payment_display_status_fmt, statusLabel));
        deviceText.setText(getString(R.string.payment_display_device_fmt,
                manager.getConnectedClients() > 0 ? "Chrome / Android" : "—"));
        String url = manager.getLocalUrl();
        localUrlText.setText(getString(R.string.payment_display_local_url_fmt,
                url != null && !url.isEmpty() ? url : "—"));

        String pairing = manager.getPairingUrl();
        if (pairing != null && !pairing.isEmpty()) {
            pairingCard.setVisibility(View.VISIBLE);
            pairingUrlText.setText(pairing);
            Bitmap qr = MessTokenQrHelper.generateQrBitmap(pairing, 512);
            if (qr != null) {
                pairingQrImage.setImageBitmap(qr);
            }
        } else {
            pairingCard.setVisibility(View.GONE);
        }

        disconnectButton.setVisibility(manager.isConnected() ? View.VISIBLE : View.GONE);

        if (manager.getErrorMessage() != null) {
            Toast.makeText(this, manager.getErrorMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
