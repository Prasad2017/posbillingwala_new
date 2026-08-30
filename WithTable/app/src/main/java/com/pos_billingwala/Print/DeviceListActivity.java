package com.pos_billingwala.Print;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pos_billingwala.Extra.TabletUi;
import com.pos_billingwala.R;
import com.zj.btsdk.BluetoothService;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Paired / discovered Bluetooth device picker as a bottom sheet.
 */
@SuppressLint("MissingPermission")
public class DeviceListActivity extends Activity {

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final String TAG = "DeviceListActivity";
    private static final Pattern MAC_PATTERN =
            Pattern.compile("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}");

    private ArrayAdapter<String> pairedAdapter;
    private ArrayAdapter<String> discoveredAdapter;
    private BluetoothService btService;
    private boolean receiverRegistered;
    private ProgressBar scanProgress;
    private TextView sheetTitle;
    private TextView scanButton;
    private View titleNewDevices;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null || device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        return;
                    }
                    String name = device.getName();
                    if (name == null || name.trim().isEmpty()) {
                        name = "Unknown device";
                    }
                    String row = name + "\n" + device.getAddress();
                    if (discoveredAdapter.getPosition(row) < 0) {
                        discoveredAdapter.add(row);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    setScanningUi(false);
                    if (discoveredAdapter.getCount() == 0) {
                        discoveredAdapter.add(getString(R.string.none_found));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "discoveryReceiver failed", e);
            }
        }
    };

    private final AdapterView.OnItemClickListener deviceClickListener = (parent, view, position, id) -> {
        try {
            if (btService != null) {
                btService.cancelDiscovery();
            }
            Object item = parent.getItemAtPosition(position);
            String info = item != null ? item.toString() : "";
            if (info.isEmpty() || !MAC_PATTERN.matcher(info).find()) {
                Toast.makeText(this, R.string.connect_fail, Toast.LENGTH_SHORT).show();
                return;
            }
            String address = info.substring(info.length() - 17);
            Intent result = new Intent();
            result.putExtra(EXTRA_DEVICE_ADDRESS, address);
            setResult(RESULT_OK, result);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "device click failed", e);
            Toast.makeText(this, R.string.connect_fail, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.device_list);
            applyBottomSheetWindow();
            setResult(RESULT_CANCELED);

            sheetTitle = findViewById(R.id.sheetTitle);
            scanProgress = findViewById(R.id.scanProgress);
            scanButton = findViewById(R.id.button_scan);
            titleNewDevices = findViewById(R.id.title_new_devices);

            findViewById(R.id.closeDeviceSheet).setOnClickListener(v -> finish());
            scanButton.setOnClickListener(v -> doDiscovery());

            pairedAdapter = new ArrayAdapter<>(this, R.layout.device_name, android.R.id.text1);
            discoveredAdapter = new ArrayAdapter<>(this, R.layout.device_name, android.R.id.text1);

            ListView pairedList = findViewById(R.id.paired_devices);
            pairedList.setAdapter(pairedAdapter);
            pairedList.setOnItemClickListener(deviceClickListener);

            ListView newList = findViewById(R.id.new_devices);
            newList.setAdapter(discoveredAdapter);
            newList.setOnItemClickListener(deviceClickListener);

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(discoveryReceiver, filter);
            receiverRegistered = true;

            btService = new BluetoothService(this, null);
            Set<BluetoothDevice> paired = btService.getPairedDev();
            if (paired != null && !paired.isEmpty()) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                for (BluetoothDevice device : paired) {
                    if (device == null) {
                        continue;
                    }
                    String name = device.getName();
                    if (name == null || name.trim().isEmpty()) {
                        name = "Paired device";
                    }
                    pairedAdapter.add(name + "\n" + device.getAddress());
                }
            } else {
                pairedAdapter.add(getString(R.string.none_paired));
            }
            sizeListView(pairedList, pairedAdapter.getCount());
        } catch (Exception e) {
            Log.e(TAG, "onCreate failed", e);
            Toast.makeText(this, R.string.connect_fail, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (btService != null) {
                btService.cancelDiscovery();
            }
        } catch (Exception e) {
            Log.e(TAG, "cancelDiscovery on destroy", e);
        }
        if (receiverRegistered) {
            try {
                unregisterReceiver(discoveryReceiver);
            } catch (Exception e) {
                Log.e(TAG, "unregisterReceiver failed", e);
            }
        }
        btService = null;
        super.onDestroy();
    }

    private void applyBottomSheetWindow() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.BOTTOM);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        WindowManager.LayoutParams params = window.getAttributes();
        if (TabletUi.isTablet(this)) {
            int horizontalInset = TabletUi.dpToPx(this, TabletUi.horizontalInsetDp(this));
            int maxWidth = TabletUi.dpToPx(this, TabletUi.bottomSheetMaxWidthDp(this));
            params.width = Math.min(maxWidth, metrics.widthPixels - horizontalInset * 2);
            params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        } else {
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.BOTTOM;
        }
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewGroup.LayoutParams lp = root.getLayoutParams();
            if (lp != null) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                root.setLayoutParams(lp);
            }
        }
    }

    private void doDiscovery() {
        try {
            if (btService == null) {
                return;
            }
            setScanningUi(true);
            titleNewDevices.setVisibility(View.VISIBLE);
            discoveredAdapter.clear();
            if (btService.isDiscovering()) {
                btService.cancelDiscovery();
            }
            btService.startDiscovery();
        } catch (Exception e) {
            Log.e(TAG, "doDiscovery failed", e);
            setScanningUi(false);
            Toast.makeText(this, R.string.connect_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void setScanningUi(boolean scanning) {
        if (scanProgress != null) {
            scanProgress.setVisibility(scanning ? View.VISIBLE : View.GONE);
        }
        if (sheetTitle != null) {
            sheetTitle.setText(scanning ? R.string.scanning : R.string.select_device);
        }
        if (scanButton != null) {
            scanButton.setEnabled(!scanning);
            scanButton.setAlpha(scanning ? 0.5f : 1f);
        }
    }

    private void sizeListView(ListView listView, int count) {
        if (listView == null || count <= 0) {
            return;
        }
        // Cap visible paired rows so the sheet stays bottom-sized
        int maxRows = Math.min(count, 5);
        float density = getResources().getDisplayMetrics().density;
        int rowHeight = (int) (56 * density);
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = rowHeight * maxRows;
        listView.setLayoutParams(params);
    }
}
