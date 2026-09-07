package com.pos_billingwala.Extra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Weight / fresh (fish, meat, veg, fruit) billing.
 * Phase 08: FeatureEngine {@link FeatureFlags#WEIGHT_SCALE} + productUnit (KG/GRAM).
 * Optional Bluetooth SPP scale (saved MAC) — common ASCII weight lines.
 */
public final class WeightFreshModule {

    public static final String PREF_SCALE_ADDRESS = "bluetoothScaleAddress";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(?:(?:n\\.?w\\.?|net|g\\.?w\\.?|gross|w(?:t|eight)?)\\s*[:=]?\\s*)?"
                    + "([+-]?\\d+(?:[.,]\\d+)?)\\s*(kg|g|gm|gram|kgs)?",
            Pattern.CASE_INSENSITIVE);

    public interface ScaleWeightCallback {
        void onWeight(float kg);

        void onError(String message);
    }

    private WeightFreshModule() {
    }

    public static boolean isEnabled(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.WEIGHT_SCALE);
    }

    public static boolean isWeightUnit(String productUnit) {
        if (productUnit == null) {
            return false;
        }
        String u = productUnit.trim().toLowerCase();
        return u.equals("kg") || u.equals("gram") || u.equals("g") || u.equals("gm")
                || u.contains("kg") || u.contains("gram");
    }

    /**
     * Prompt for weight (stored as cart qty; rate stays unit price) when flag on and unit is weight-like.
     * Open-price products already use the amount/qty sheet — skip duplicate prompt.
     */
    public static boolean shouldPromptWeight(Context context, String productUnit, boolean openPrice) {
        return !openPrice && isEnabled(context) && isWeightUnit(productUnit);
    }

    /** Line amount = rate × weight (caller supplies weight from scale or keypad). */
    public static float lineAmount(float ratePerUnit, float weight) {
        if (weight < 0f) {
            weight = 0f;
        }
        return ratePerUnit * weight;
    }

    public static String getScaleAddress(Context context) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return "";
        }
        String v = Common.getSavedUserData(ctx, PREF_SCALE_ADDRESS);
        return v != null ? v.trim() : "";
    }

    public static void setScaleAddress(Context context, String address) {
        Context ctx = AppContexts.or(context);
        if (ctx == null) {
            return;
        }
        Common.saveUserData(ctx, PREF_SCALE_ADDRESS, address != null ? address.trim() : "");
    }

    public static boolean hasScaleAddress(Context context) {
        return !getScaleAddress(context).isEmpty();
    }

    /**
     * Parse common scale ASCII / wedge strings into kilograms.
     * Supports {@code 1.250}, {@code 1,250 kg}, {@code ST,GS,+  1.250kg},
     * {@code N.W.:1.234kg}, {@code Net 1234 g}, grams → kg.
     */
    @Nullable
    public static Float parseScaleReading(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String s = raw.trim().replace('\u0000', ' ');
        Matcher m = WEIGHT_PATTERN.matcher(s);
        Float last = null;
        while (m.find()) {
            String num = m.group(1);
            if (num == null) {
                continue;
            }
            num = num.replace(',', '.');
            try {
                float v = Float.parseFloat(num);
                String unit = m.group(2);
                if (unit != null) {
                    String u = unit.toLowerCase();
                    if (u.equals("g") || u.equals("gm") || u.equals("gram")) {
                        v = v / 1000f;
                    }
                } else if (v > 100f && !s.toLowerCase().contains("kg")) {
                    // Large bare number often grams on retail scales
                    v = v / 1000f;
                }
                if (v > 0f && v < 500f) {
                    last = v;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return last;
    }

    /**
     * One-shot Bluetooth SPP read (~1.5s), parse latest weight as kg.
     * Runs on background thread; callback on main.
     */
    public static void readScaleWeightAsync(Context context, ScaleWeightCallback callback) {
        if (callback == null) {
            return;
        }
        Context ctx = AppContexts.or(context);
        String address = getScaleAddress(ctx);
        if (address.isEmpty()) {
            callback.onError("scale_not_configured");
            return;
        }
        Handler main = new Handler(Looper.getMainLooper());
        AppExecutors.get().db().execute(() -> {
            BluetoothSocket socket = null;
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    main.post(() -> callback.onError("bluetooth_off"));
                    return;
                }
                BluetoothDevice device = adapter.getRemoteDevice(address);
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                adapter.cancelDiscovery();
                socket.connect();
                InputStream in = socket.getInputStream();
                StringBuilder buf = new StringBuilder();
                long end = System.currentTimeMillis() + 2200L;
                byte[] chunk = new byte[128];
                while (System.currentTimeMillis() < end) {
                    if (in.available() > 0) {
                        int n = in.read(chunk);
                        if (n > 0) {
                            buf.append(new String(chunk, 0, n));
                        }
                    } else {
                        try {
                            Thread.sleep(40);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                Float kg = parseScaleReading(buf.toString());
                if (kg == null) {
                    main.post(() -> callback.onError("no_weight"));
                } else {
                    final float value = kg;
                    main.post(() -> callback.onWeight(value));
                }
            } catch (Exception e) {
                main.post(() -> callback.onError("connect_failed"));
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    public static String moduleSummary(Context context) {
        boolean scale = hasScaleAddress(context);
        return "Weight/Fresh: " + (isEnabled(context) ? "on (keypad + SPP BT scale)" : "off")
                + "\nScale MAC: " + (scale ? getScaleAddress(context) : "not set")
                + "\nUnits: KG / GRAM via productUnit → cart qty = weight"
                + "\nSettings: Bluetooth scale (when weight template on)";
    }
}
