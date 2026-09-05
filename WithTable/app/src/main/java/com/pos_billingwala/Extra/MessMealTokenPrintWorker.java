package com.pos_billingwala.Extra;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.MessMealTokenItem;
import com.pos_billingwala.Model.PrinterSettingResponse;
import com.pos_billingwala.Print.PrintImage;
import com.pos_billingwala.Print.PrinterConnectionHelper;
import com.pos_billingwala.Retrofit.Api;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Single-flight Mess meal-token print worker.
 * Never print directly from FCM callbacks — enqueue then drain here.
 */
public final class MessMealTokenPrintWorker {

    private static final String TAG = "MessMealPrintWorker";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);

    private MessMealTokenPrintWorker() {
    }

    public static void bindActivity(Activity activity) {
        activityRef = new WeakReference<>(activity);
    }

    public static void enqueueFromFcm(Context context, String tokenId, String tokenNumber,
                                      String registrationNo, String mealSession, String date,
                                      String createdAt, String printStatus) {
        Context app = context.getApplicationContext();
        POSBillingWalaDatabase db = new POSBillingWalaDatabase(app);
        String status = "PRINT_PENDING";
        if ("PRINT_FAILED".equalsIgnoreCase(printStatus)) {
            status = "PRINT_FAILED";
        }
        db.upsertMessMealTokenQueue(tokenId, tokenNumber, registrationNo, mealSession, date, "", createdAt, status);
        kick(app);
    }

    public static void kick(Context context) {
        Context app = context.getApplicationContext();
        if (context instanceof Activity) {
            bindActivity((Activity) context);
        }
        EXEC.execute(() -> drain(app));
    }

    public static void recoverPendingFromServer(Context context) {
        Context app = context.getApplicationContext();
        if (context instanceof Activity) {
            bindActivity((Activity) context);
        }
        if (MainActivity.userId == null || MainActivity.userId.trim().isEmpty()) {
            return;
        }
        String androidId = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
        Api.getClient(app).getMessMealTokenPending(MainActivity.userId, androidId != null ? androidId : "")
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().messMealTokens == null) {
                            return;
                        }
                        POSBillingWalaDatabase db = new POSBillingWalaDatabase(app);
                        for (MessMealTokenItem t : response.body().messMealTokens) {
                            if (t == null || t.tokenId == null) continue;
                            db.upsertMessMealTokenQueue(
                                    t.tokenId,
                                    t.tokenNumber,
                                    t.registrationNo,
                                    t.mealSession,
                                    t.date,
                                    t.memberName,
                                    t.createdAt,
                                    "PRINT_PENDING"
                            );
                        }
                        kick(app);
                    }

                    @Override
                    public void onFailure(Call<AllApiResponse> call, Throwable t) {
                        Log.w(TAG, "pending recover failed", t);
                    }
                });
    }

    private static void drain(Context app) {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }
        try {
            POSBillingWalaDatabase db = new POSBillingWalaDatabase(app);
            while (true) {
                Cursor c = db.getNextMessMealPrintJob();
                if (c == null || !c.moveToFirst()) {
                    if (c != null) c.close();
                    break;
                }
                String publicId = c.getString(c.getColumnIndex("serverPublicId"));
                String tokenNumber = c.getString(c.getColumnIndex("tokenNumber"));
                String registrationNo = c.getString(c.getColumnIndex("registrationNo"));
                String mealSession = c.getString(c.getColumnIndex("mealSession"));
                String tokenDate = c.getString(c.getColumnIndex("tokenDate"));
                String createdAt = c.getString(c.getColumnIndex("createdAt"));
                c.close();

                db.updateMessMealTokenQueueStatus(publicId, "PRINTING");
                boolean printed = printSlip(app, tokenNumber, registrationNo, mealSession, tokenDate, createdAt);
                if (printed) {
                    db.updateMessMealTokenQueueStatus(publicId, "PRINTED");
                    ackServer(app, publicId, "SUCCESS");
                } else {
                    db.updateMessMealTokenQueueStatus(publicId, "PRINT_FAILED");
                    ackServer(app, publicId, "FAILED");
                    MAIN.post(() -> Toast.makeText(app, "Token received — printer unavailable.", Toast.LENGTH_LONG).show());
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "drain error", e);
        } finally {
            RUNNING.set(false);
        }
    }

    private static boolean printSlip(Context app, String tokenNumber, String registrationNo,
                                     String mealSession, String tokenDate, String createdAt) {
        try {
            POSBillingWalaDatabase db = new POSBillingWalaDatabase(app);
            List<PrinterSettingResponse> printers = db.getPrinterSettingDetails();
            if (printers == null || printers.isEmpty()) {
                return false;
            }
            String addr = printers.get(0).getBluetoothAddress();
            if (addr == null || addr.trim().isEmpty()) {
                return false;
            }

            Bitmap slip = buildSlipBitmap(tokenNumber, registrationNo, mealSession, tokenDate, createdAt);
            PrintImage printImage = new PrintImage(slip);
            printImage.PrepareImage(PrintImage.dither.floyd_steinberg, 128);
            byte[] bytes = printImage.getPrintImageData();

            // Fast path: already connected
            if (PrinterConnectionHelper.safeWriteBill(app, bytes)) {
                return true;
            }

            Activity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) {
                return false;
            }

            final boolean[] ok = {false};
            final Object lock = new Object();
            MAIN.post(() -> PrinterConnectionHelper.ensureBillPrinterAsync(activity, addr, () -> {
                boolean wrote = PrinterConnectionHelper.safeWriteBill(app, bytes);
                synchronized (lock) {
                    ok[0] = wrote;
                    lock.notifyAll();
                }
            }, () -> {
                synchronized (lock) {
                    ok[0] = false;
                    lock.notifyAll();
                }
            }));
            synchronized (lock) {
                lock.wait(25000);
            }
            return ok[0];
        } catch (Exception e) {
            Log.e(TAG, "printSlip failed", e);
            return false;
        }
    }

    private static Bitmap buildSlipBitmap(String tokenNumber, String registrationNo,
                                          String mealSession, String tokenDate, String createdAt) {
        int width = 384;
        int height = 420;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.BLACK);
        title.setTextAlign(Paint.Align.CENTER);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextSize(28);

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.BLACK);
        body.setTextAlign(Paint.Align.CENTER);
        body.setTextSize(22);

        Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
        big.setColor(Color.BLACK);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        big.setTextSize(40);

        float cx = width / 2f;
        float y = 36;
        canvas.drawText("BILLINGWALA", cx, y, title);
        y += 34;
        canvas.drawText("MESS TOKEN", cx, y, title);
        y += 28;
        canvas.drawLine(24, y, width - 24, y, body);
        y += 50;
        canvas.drawText("Token: " + nullSafe(tokenNumber), cx, y, big);
        y += 42;
        canvas.drawText("Reg. No: " + nullSafe(registrationNo), cx, y, body);
        y += 32;
        canvas.drawText("Meal: " + nullSafe(mealSession), cx, y, body);
        y += 32;
        String dateLine = nullSafe(tokenDate);
        if (dateLine.isEmpty()) {
            dateLine = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(new Date());
        }
        canvas.drawText("Date: " + dateLine, cx, y, body);
        y += 32;
        String timeLine = nullSafe(createdAt);
        if (timeLine.length() >= 16) {
            timeLine = timeLine.substring(11, 16);
        }
        if (!timeLine.isEmpty()) {
            canvas.drawText("Time: " + timeLine, cx, y, body);
            y += 28;
        }
        canvas.drawLine(24, y, width - 24, y, body);
        return bmp;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static void ackServer(Context app, String publicId, String result) {
        if (MainActivity.userId == null || MainActivity.userId.trim().isEmpty()) {
            return;
        }
        String androidId = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
        Api.getClient(app).ackMessMealTokenPrint(
                MainActivity.userId,
                publicId,
                result,
                androidId != null ? androidId : ""
        ).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                Log.w(TAG, "ack failed", t);
            }
        });
    }
}
