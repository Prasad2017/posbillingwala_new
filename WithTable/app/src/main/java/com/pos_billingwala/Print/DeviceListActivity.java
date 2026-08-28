package com.pos_billingwala.Print;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pos_billingwala.R;
import com.zj.btsdk.BluetoothService;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Paired / discovered Bluetooth device picker. Never crashes on null device names.
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
                    discoveredAdapter.add(name + "\n" + device.getAddress());
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    setProgressBarIndeterminateVisibility(false);
                    setTitle(R.string.select_device);
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
            TextView row = (TextView) view;
            String info = row.getText().toString();
            if (info == null || !MAC_PATTERN.matcher(info).find()) {
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
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            setContentView(R.layout.device_list);
            setResult(RESULT_CANCELED);

            Button scanButton = findViewById(R.id.button_scan);
            scanButton.setOnClickListener(v -> {
                doDiscovery();
                v.setVisibility(View.GONE);
            });

            pairedAdapter = new ArrayAdapter<>(this, R.layout.device_name);
            discoveredAdapter = new ArrayAdapter<>(this, R.layout.device_name);

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

    private void doDiscovery() {
        try {
            if (btService == null) {
                return;
            }
            setProgressBarIndeterminateVisibility(true);
            setTitle(R.string.scanning);
            findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
            if (btService.isDiscovering()) {
                btService.cancelDiscovery();
            }
            btService.startDiscovery();
        } catch (Exception e) {
            Log.e(TAG, "doDiscovery failed", e);
            Toast.makeText(this, R.string.connect_fail, Toast.LENGTH_SHORT).show();
        }
    }
}
