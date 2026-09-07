package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.ProductCartResponse;
import com.pos_billingwala.Model.ProductPortionResponse;
import com.pos_billingwala.Model.ProductResponse;
import com.pos_billingwala.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Bakery / cake custom orders — note + delivery date + optional local photo
 * captured into cart portionName label (no new cart column).
 */
public final class CakeBakeryModule {

    public static final String NOTE_PREFIX = "Order: ";
    public static final String PHOTO_MARKER = " | Photo:";
    public static final int REQUEST_CUSTOM_ORDER_PHOTO = 904;

    public interface CustomOrderCallback {
        void onReady(ProductPortionResponse noteAsPortion);
    }

    private static class PendingForm {
        Fragment host;
        Activity activity;
        ProductResponse product;
        ProductPortionResponse existingPortion;
        CustomOrderCallback callback;
        String note;
        String due;
        String dep;
        String photoPath;
    }

    private static PendingForm pendingForm;

    private CakeBakeryModule() {
    }

    public static boolean isEnabled(Context context) {
        return FeatureEngine.isEnabled(context, FeatureFlags.CUSTOM_ORDERS);
    }

    public static boolean isBakeryTemplate(Context context) {
        return BusinessTypes.BAKERY.equals(
                BusinessTypes.normalize(BusinessSession.getBusinessType(context)));
    }

    public static boolean shouldPromptCustomOrder(Context context) {
        return isEnabled(context);
    }

    public static BillingMode preferredBillingMode() {
        return BillingMode.FAST;
    }

    public static void showCustomOrderDialog(Activity activity, ProductResponse product,
                                             ProductPortionResponse existingPortion,
                                             CustomOrderCallback callback) {
        showCustomOrderDialog(null, activity, product, existingPortion, callback, "", "", "", null);
    }

    public static void showCustomOrderDialog(Fragment host, Activity activity, ProductResponse product,
                                             ProductPortionResponse existingPortion,
                                             CustomOrderCallback callback) {
        showCustomOrderDialog(host, activity, product, existingPortion, callback, "", "", "", null);
    }

    private static void showCustomOrderDialog(Fragment host, Activity activity, ProductResponse product,
                                              ProductPortionResponse existingPortion,
                                              CustomOrderCallback callback,
                                              String noteVal, String dueVal, String depVal,
                                              String existingPhotoPath) {
        if (activity == null || product == null || callback == null) {
            return;
        }
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, pad / 2);
        EditText note = new EditText(activity);
        note.setHint(activity.getString(R.string.custom_order_note_hint));
        if (noteVal != null && !noteVal.isEmpty()) {
            note.setText(noteVal);
        }
        EditText delivery = new EditText(activity);
        delivery.setHint(activity.getString(R.string.custom_order_delivery_hint));
        delivery.setSingleLine(true);
        if (dueVal != null && !dueVal.isEmpty()) {
            delivery.setText(dueVal);
        } else {
            SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            delivery.setText(fmt.format(cal.getTime()));
        }
        EditText deposit = new EditText(activity);
        deposit.setHint(activity.getString(R.string.custom_order_deposit_hint));
        deposit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        deposit.setSingleLine(true);
        if (depVal != null && !depVal.isEmpty()) {
            deposit.setText(depVal);
        }
        TextView photoStatus = new TextView(activity);
        final String[] photoPath = {existingPhotoPath};
        if (photoPath[0] != null && !photoPath[0].isEmpty()) {
            photoStatus.setText(activity.getString(R.string.custom_order_photo_attached));
        } else {
            photoStatus.setText(activity.getString(R.string.custom_order_photo_none));
        }
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        Button attach = new Button(activity);
        attach.setText(R.string.custom_order_attach_photo);
        attach.setOnClickListener(v -> {
            if (host == null || !host.isAdded()) {
                Toast.makeText(activity, R.string.custom_order_photo_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }
            PendingForm p = new PendingForm();
            p.host = host;
            p.activity = activity;
            p.product = product;
            p.existingPortion = existingPortion;
            p.callback = callback;
            p.note = note.getText() != null ? note.getText().toString() : "";
            p.due = delivery.getText() != null ? delivery.getText().toString() : "";
            p.dep = deposit.getText() != null ? deposit.getText().toString() : "";
            p.photoPath = photoPath[0];
            pendingForm = p;
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                host.startActivityForResult(
                        Intent.createChooser(intent, activity.getString(R.string.custom_order_attach_photo)),
                        REQUEST_CUSTOM_ORDER_PHOTO);
            } catch (Exception e) {
                pendingForm = null;
                Toast.makeText(activity, R.string.custom_order_photo_unavailable, Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(note);
        box.addView(delivery);
        box.addView(deposit);
        box.addView(photoStatus);
        box.addView(attach);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.custom_order_title)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String n = note.getText() != null ? note.getText().toString().trim() : "";
                    String due = delivery.getText() != null ? delivery.getText().toString().trim() : "";
                    String dep = deposit.getText() != null ? deposit.getText().toString().trim() : "";
                    finishCustomOrder(activity, product, existingPortion, callback, n, due, dep, photoPath[0]);
                    pendingForm = null;
                })
                .setNeutralButton(R.string.custom_order_skip, (d, w) -> {
                    pendingForm = null;
                    callback.onReady(existingPortion);
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> pendingForm = null)
                .create();
        dialogHolder[0] = dialog;
        dialog.show();
    }

    /** Call from CreatePos {@code onActivityResult}. */
    public static boolean handlePhotoActivityResult(Fragment host, int requestCode, int resultCode,
                                                    Intent data) {
        if (requestCode != REQUEST_CUSTOM_ORDER_PHOTO) {
            return false;
        }
        PendingForm p = pendingForm;
        if (p == null || p.activity == null || p.product == null || p.callback == null) {
            return true;
        }
        Fragment useHost = p.host != null ? p.host : host;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            showCustomOrderDialog(useHost, p.activity, p.product, p.existingPortion, p.callback,
                    p.note, p.due, p.dep, p.photoPath);
            return true;
        }
        String saved = copyPhotoToAppFiles(p.activity, data.getData());
        if (saved == null) {
            Toast.makeText(p.activity, R.string.custom_order_photo_failed, Toast.LENGTH_SHORT).show();
            showCustomOrderDialog(useHost, p.activity, p.product, p.existingPortion, p.callback,
                    p.note, p.due, p.dep, p.photoPath);
            return true;
        }
        Toast.makeText(p.activity, R.string.custom_order_photo_attached, Toast.LENGTH_SHORT).show();
        showCustomOrderDialog(useHost, p.activity, p.product, p.existingPortion, p.callback,
                p.note, p.due, p.dep, saved);
        return true;
    }

    private static void finishCustomOrder(Activity activity, ProductResponse product,
                                          ProductPortionResponse existingPortion,
                                          CustomOrderCallback callback, String n, String due, String dep,
                                          String photoPath) {
        StringBuilder label = new StringBuilder(NOTE_PREFIX);
        if (n != null && !n.isEmpty()) {
            label.append(n);
        } else {
            label.append(product.getProductName());
        }
        if (due != null && !due.isEmpty()) {
            label.append(" | Due ").append(due);
        }
        if (dep != null && !dep.isEmpty()) {
            label.append(" | Dep ").append(dep);
        }
        String photoFile = "";
        if (photoPath != null && !photoPath.trim().isEmpty()) {
            photoFile = new File(photoPath).getName();
            label.append(PHOTO_MARKER).append(photoFile);
        }
        ProductPortionResponse p = existingPortion != null ? existingPortion : new ProductPortionResponse();
        if (existingPortion == null) {
            p.setPortionId("order:" + System.currentTimeMillis());
            p.setPortionPrice(product.getProductPrice());
        }
        String base = existingPortion != null && existingPortion.getPortionName() != null
                ? existingPortion.getPortionName() + " · " : "";
        p.setPortionName(base + label);
        p.setProductId(product.getProductId());

        if (activity != null && dep != null && !dep.trim().isEmpty()) {
            try {
                new POSBillingWalaDatabase(activity).addCustomOrderDeposit(
                        product.getProductName(),
                        n != null ? n : "",
                        dep.trim(),
                        due != null ? due : "",
                        photoFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        callback.onReady(p);
    }

    public static void showDepositLedger(Activity activity) {
        if (activity == null) {
            return;
        }
        POSBillingWalaDatabase db = new POSBillingWalaDatabase(activity);
        List<android.util.Pair<String, String>> open = db.getOpenCustomOrderDeposits(50);
        if (open.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.deposit_ledger_title)
                    .setMessage(R.string.deposit_ledger_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        String[] labels = new String[open.size()];
        for (int i = 0; i < open.size(); i++) {
            labels[i] = open.get(i).second;
        }
        BottomSheetUi.showSingleChoice(activity, activity.getString(R.string.deposit_ledger_title),
                labels, -1, true, index -> {
                    if (index < 0 || index >= open.size()) {
                        return;
                    }
                    String depositId = open.get(index).first;
                    String[] actions = new String[]{
                            activity.getString(R.string.deposit_mark_applied),
                            activity.getString(R.string.deposit_mark_refunded)
                    };
                    BottomSheetUi.showSingleChoice(activity, open.get(index).second, actions, -1, true,
                            action -> {
                                if (action == 0) {
                                    db.updateCustomOrderDepositStatus(depositId, "applied");
                                    Toast.makeText(activity, R.string.deposit_updated, Toast.LENGTH_SHORT).show();
                                } else if (action == 1) {
                                    db.updateCustomOrderDepositStatus(depositId, "refunded");
                                    Toast.makeText(activity, R.string.deposit_updated, Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private static String copyPhotoToAppFiles(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        File dir = new File(context.getFilesDir(), "custom_orders");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        String name = "order_" + System.currentTimeMillis() + ".jpg";
        File out = new File(dir, name);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            if (in == null) {
                return null;
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                fos.write(buf, 0, n);
            }
            fos.flush();
            return out.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Strip {@code | Photo:filename} from cart/print labels (image prints separately). */
    public static String stripPhotoMarker(String label) {
        if (label == null || label.isEmpty()) {
            return label;
        }
        int idx = label.indexOf(PHOTO_MARKER);
        if (idx < 0) {
            return label;
        }
        return label.substring(0, idx).trim();
    }

    public static String extractPhotoFileName(String label) {
        if (label == null || label.isEmpty()) {
            return "";
        }
        int idx = label.indexOf(PHOTO_MARKER);
        if (idx < 0) {
            return "";
        }
        String name = label.substring(idx + PHOTO_MARKER.length()).trim();
        int cut = name.indexOf(" | ");
        if (cut >= 0) {
            name = name.substring(0, cut).trim();
        }
        // Safety: basename only
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        return name;
    }

    public static File resolvePhotoFile(Context context, String fileName) {
        if (context == null || fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        String safe = fileName.trim();
        if (safe.contains("..") || safe.contains("/") || safe.contains("\\")) {
            return null;
        }
        File f = new File(new File(context.getFilesDir(), "custom_orders"), safe);
        return f.isFile() ? f : null;
    }

    public static Bitmap loadPrintablePhoto(Context context, String fileName, int maxWidthPx) {
        File f = resolvePhotoFile(context, fileName);
        if (f == null) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), bounds);
            int sample = 1;
            int target = Math.max(48, maxWidthPx);
            while (bounds.outWidth / sample > target * 2 || bounds.outHeight / sample > target * 2) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap raw = BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            if (raw == null) {
                return null;
            }
            if (raw.getWidth() <= target) {
                return raw;
            }
            float scale = target / (float) raw.getWidth();
            int h = Math.max(1, Math.round(raw.getHeight() * scale));
            Bitmap scaled = Bitmap.createScaledBitmap(raw, target, h, true);
            if (scaled != raw) {
                raw.recycle();
            }
            return scaled;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fill a bill layout container with local custom-order reference photos from cart lines.
     * No-op when container null or no photos found (restaurant bills unchanged).
     */
    public static void bindCustomOrderPhotos(Context context, LinearLayout container,
                                             List<ProductCartResponse> cart, int maxHeightDp) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        container.setVisibility(View.GONE);
        if (context == null || cart == null || cart.isEmpty()) {
            return;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int maxH = Math.max(48, Math.round(maxHeightDp * density));
        int maxW = Math.max(120, Math.round(container.getResources().getDisplayMetrics().widthPixels * 0.35f));
        // Prefer bill strip width ~48mm/72mm; cap decode width for thermal printers
        int decodeW = Math.min(384, Math.max(160, maxW));

        Set<String> seen = new LinkedHashSet<>();
        for (ProductCartResponse line : cart) {
            if (line == null) {
                continue;
            }
            String fromPortion = extractPhotoFileName(line.getPortionName());
            String fromDisplay = extractPhotoFileName(line.getDisplayLineName());
            String fileName = !fromPortion.isEmpty() ? fromPortion : fromDisplay;
            if (fileName.isEmpty() || !seen.add(fileName)) {
                continue;
            }
            Bitmap bmp = loadPrintablePhoto(context, fileName, decodeW);
            if (bmp == null) {
                continue;
            }
            ImageView iv = new ImageView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, maxH);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            int m = Math.round(4 * density);
            lp.setMargins(m, m, m, m);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setAdjustViewBounds(true);
            iv.setImageBitmap(bmp);
            container.addView(iv);
        }
        if (container.getChildCount() > 0) {
            TextView caption = new TextView(context);
            caption.setText(context.getString(R.string.custom_order_photo_on_bill));
            caption.setGravity(Gravity.CENTER);
            caption.setTextSize(10);
            caption.setPadding(0, 0, 0, Math.round(2 * density));
            container.addView(caption, 0);
            container.setVisibility(View.VISIBLE);
        }
    }

    public static String moduleSummary(Context context) {
        int open = 0;
        try {
            if (context != null) {
                open = new POSBillingWalaDatabase(context).getOpenCustomOrderDeposits(200).size();
            }
        } catch (Exception ignored) {
        }
        return "Custom orders: " + (isEnabled(context) ? "on (note+due+photo+deposit+bill photo)" : "off")
                + "\nBakery template: " + (isBakeryTemplate(context) ? "yes" : "no")
                + "\nOpen deposits: " + open
                + "\nBilling: " + preferredBillingMode().getWireValue();
    }
}
