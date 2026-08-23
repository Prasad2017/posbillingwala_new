package com.pos_billingwala.Print;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.pos_billingwala.R;
import com.woosim.printer.WoosimService;


@SuppressLint("StaticFieldLeak")
public class WoosimPrnMng {

    public static final int REQUEST_ENABLE_BT = 4;
    public static final int REQUEST_CONNECT_DEVICE = 6;

    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;

    // Key names received from the BluetoothPrintService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private static final String TAG = "Bluetooth";
    static Context context = null;
    static Activity act = null;
    static boolean printerConnected = false;
    private static BluetoothPrintService mPrintService = null;
    private static WoosimService mWoosim = null;
    protected Context contx;
    private String mDeviceAddr = "";
    private BluetoothDevice device;

    @SuppressLint("HandlerLeak")
    public WoosimPrnMng(Context c, String deviceAddr, Context context) {
        mDeviceAddr = deviceAddr;
        contx = c;
        act = (Activity) context;

        if (deviceAddr.isEmpty()) {
            pairPrinter(contx, act);
        } else {

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            device = mBluetoothAdapter.getRemoteDevice(mDeviceAddr);

            Handler mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_DEVICE_NAME:
                            try {
                                String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                                Toast.makeText(contx, contx.getString(R.string.connected) + " " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case MESSAGE_TOAST:
                            if (mWoosim != null) {
                                Log.e("handleMessage: ", "" + msg.getData().getInt(TOAST));
                            }
                            break;
                        case MESSAGE_READ:
                            if (mWoosim != null) {
                                mWoosim.processRcvData((byte[]) msg.obj, msg.arg1);
                            }
                            break;
                        case WoosimService.MESSAGE_PRINTER:
                            Toast.makeText(contx, contx.getString(R.string.toast_msr_message), Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Log.e(TAG, "Unknown message: " + msg.what);
                            break;
                    }
                }
            };

            if (mWoosim == null) mWoosim = new WoosimService(mHandler);
            if (mPrintService == null) {
                mPrintService = new BluetoothPrintService(contx, mHandler);
            }

            if (mPrintService.getState() == BluetoothPrintService.STATE_NONE) {
                mPrintService.start();
            }

            if (mPrintService.getState() == BluetoothPrintService.STATE_LISTEN) {
                mPrintService.connect(device, false);
            } else {
                if (mPrintService.getState() == BluetoothPrintService.STATE_CONNECTED)
                    printInfo(deviceAddr);
            }
        }

    }

    public static void releaseAllocations(Context context) {
        try {
            printerConnected = false;
            if (mPrintService != null) {
                final BluetoothPrintService service = mPrintService;
                mPrintService = null;
                new Thread(() -> {
                    try {
                        service.stop();
                    } catch (Exception e) {
                        Log.e(TAG, "releaseAllocations stop failed", e);
                    }
                }, "printer-release").start();
            }
            mWoosim = null;
            if (context != null) {
                Toast.makeText(context, context.getString(R.string.toast_printer_disconnect), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "releaseAllocations failed", e);
        }
    }

    public static boolean isBTopen(Context con, Activity activity) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(con, con.getString(R.string.toast_bluetooth_is_not_supported_on_this_devic), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }

    public static void pairPrinter(Context con, Activity act1) {
        // Check if Bluetooth is enabled before pairing
        context = con;
        act = act1;
        Intent serverIntent = new Intent(context, DeviceListActivity.class);
        act.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public static BluetoothPrintService getServiceInstance() {
        return mPrintService;
    }

    public static boolean isPrinterConnected(Context con, Activity act1) {
        context = con;
        act = act1;
        return printerConnected;
    }

    public static void sendAutoCutter() {
        try {
            if (mPrintService == null) {
                return;
            }
            byte[] cutCommand = new byte[]{0x1B, 0x69};
            mPrintService.write(cutCommand);
            Log.d(TAG, "Auto cut command sent");
        } catch (Exception e) {
            Log.e(TAG, "sendAutoCutter failed", e);
        }
    }

    public boolean printSucc() {
        return mPrintService.getState() == BluetoothPrintService.STATE_CONNECTED;
    }

    public String getDeviceAddr() {
        return mDeviceAddr;
    }

    private void printInfo(String deviceAddr) {
        printerConnected = true;
        Log.d(TAG, "Connecting to device: " + deviceAddr);
    }

}