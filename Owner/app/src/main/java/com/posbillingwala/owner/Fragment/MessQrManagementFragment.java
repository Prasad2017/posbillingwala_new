package com.posbillingwala.owner.Fragment;

import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.MessQrBitmapHelper;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.Model.LicenseResponse;
import com.posbillingwala.owner.Model.MessQrInfo;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessQrManagementFragment extends Fragment {

    private Activity activity;
    private Spinner outletSpinner;
    private ImageView qrImage;
    private TextView qrStatus, qrMeta, qrUrl;
    private final List<LicenseResponse> outlets = new ArrayList<>();
    private LicenseResponse selectedOutlet;
    private MessQrInfo currentQr;
    private Bitmap currentBitmap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_qr_management, container, false);
        activity = getActivity();

        outletSpinner = view.findViewById(R.id.outletSpinner);
        qrImage = view.findViewById(R.id.qrImage);
        qrStatus = view.findViewById(R.id.qrStatus);
        qrMeta = view.findViewById(R.id.qrMeta);
        qrUrl = view.findViewById(R.id.qrUrl);
        Button btnGenerate = view.findViewById(R.id.btnGenerate);
        Button btnShare = view.findViewById(R.id.btnShare);
        Button btnDownload = view.findViewById(R.id.btnDownload);
        Button btnRegenerate = view.findViewById(R.id.btnRegenerate);
        Button btnDeactivate = view.findViewById(R.id.btnDeactivate);
        ImageView back = view.findViewById(R.id.backBtn);

        if (back != null) {
            back.setOnClickListener(v -> {
                if (activity != null) {
                    ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                }
            });
        }

        btnGenerate.setOnClickListener(v -> generateOrRefresh(false));
        btnRegenerate.setOnClickListener(v -> generateOrRefresh(true));
        btnDeactivate.setOnClickListener(v -> setStatus("INACTIVE"));
        btnShare.setOnClickListener(v -> shareQr());
        btnDownload.setOnClickListener(v -> downloadQr());

        outletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position >= 0 && position < outlets.size()) {
                    selectedOutlet = outlets.get(position);
                    loadQr();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadOutlets();
        return view;
    }

    private void loadOutlets() {
        Api.getClient().getStoreWise(MainActivity.userId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                outlets.clear();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getLicenseResponseList() != null) {
                    outlets.addAll(response.body().getLicenseResponseList());
                }
                List<String> labels = new ArrayList<>();
                for (LicenseResponse lic : outlets) {
                    String name = lic.getShopName1() != null && !lic.getShopName1().isEmpty()
                            ? lic.getShopName1()
                            : (lic.getUserName() != null ? lic.getUserName() : "Outlet");
                    String branch = lic.getBranchLabel() != null && !lic.getBranchLabel().isEmpty()
                            ? " · " + lic.getBranchLabel() : "";
                    labels.add(name + branch + " (#" + lic.getLicensesId() + ")");
                }
                if (activity == null) return;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                        android.R.layout.simple_spinner_dropdown_item, labels);
                outletSpinner.setAdapter(adapter);
                if (outlets.isEmpty()) {
                    qrMeta.setText("No outlets found for this owner.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                if (activity != null) {
                    Toast.makeText(activity, "Unable to load outlets", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String licenceId() {
        return selectedOutlet != null ? selectedOutlet.getLicensesId() : null;
    }

    private String messLabel() {
        if (selectedOutlet == null) return "Mess";
        if (selectedOutlet.getShopName1() != null && !selectedOutlet.getShopName1().isEmpty()) {
            return selectedOutlet.getShopName1();
        }
        if (selectedOutlet.getUserName() != null && !selectedOutlet.getUserName().isEmpty()) {
            return selectedOutlet.getUserName();
        }
        return "Mess";
    }

    private String branchLabel() {
        if (selectedOutlet != null && selectedOutlet.getBranchLabel() != null
                && !selectedOutlet.getBranchLabel().isEmpty()) {
            return selectedOutlet.getBranchLabel();
        }
        return "Main Branch";
    }

    private void loadQr() {
        String lid = licenceId();
        if (lid == null || lid.isEmpty()) return;
        clearQrUi();
        Api.getClient().getMessQr(MainActivity.userId, lid).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().messQr != null) {
                    bindQr(response.body().messQr);
                } else {
                    qrStatus.setText("Status: —");
                    qrMeta.setText("No active QR for this outlet. Tap Generate QR.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                if (activity != null) {
                    Toast.makeText(activity, "Network error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void clearQrUi() {
        currentQr = null;
        currentBitmap = null;
        if (qrImage != null) qrImage.setImageDrawable(null);
        if (qrUrl != null) qrUrl.setText("");
    }

    private void generateOrRefresh(boolean regenerate) {
        String lid = licenceId();
        if (lid == null || lid.isEmpty()) {
            Toast.makeText(activity, "Select an outlet first", Toast.LENGTH_SHORT).show();
            return;
        }
        Call<AllApiResponse> call = regenerate
                ? Api.getClient().regenerateMessQr(MainActivity.userId, lid, messLabel(), branchLabel())
                : Api.getClient().generateMessQr(MainActivity.userId, lid, messLabel(), branchLabel());
        call.enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().status)
                        && response.body().messQr != null) {
                    bindQr(response.body().messQr);
                    Toast.makeText(activity,
                            regenerate ? "QR regenerated" : "QR generated",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String msg = response.body() != null ? response.body().message : null;
                    Toast.makeText(activity,
                            msg != null && !msg.isEmpty() ? msg : "Unable to update QR",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(activity, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setStatus(String status) {
        String lid = licenceId();
        if (lid == null || lid.isEmpty()) {
            Toast.makeText(activity, "Select an outlet first", Toast.LENGTH_SHORT).show();
            return;
        }
        Api.getClient().setMessQrStatus(MainActivity.userId, lid, status)
                .enqueue(new Callback<AllApiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                        loadQr();
                        Toast.makeText(activity, "QR updated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                        Toast.makeText(activity, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindQr(MessQrInfo qr) {
        currentQr = qr;
        qrStatus.setText("Status: " + (qr.status != null ? qr.status : ""));
        String mess = qr.messLabel != null && !qr.messLabel.isEmpty() ? qr.messLabel : messLabel();
        String branch = qr.branchLabel != null && !qr.branchLabel.isEmpty() ? qr.branchLabel : branchLabel();
        String printNote = (qr.printDeviceId != null && !qr.printDeviceId.isEmpty())
                ? "\nPrint device: linked on POS"
                : "\nPrint device: not linked yet (open QR Management on POS once)";
        qrMeta.setText("Mess: " + mess + "\nBranch: " + branch + printNote);
        qrUrl.setText(qr.qrUrl != null ? qr.qrUrl : "");
        if (qr.qrUrl != null) {
            currentBitmap = MessQrBitmapHelper.generateQrBitmap(qr.qrUrl, 768);
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
        Bitmap qr = currentBitmap != null ? currentBitmap : MessQrBitmapHelper.generateQrBitmap(
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
        if (activity == null || currentQr == null || currentQr.qrUrl == null) {
            Toast.makeText(activity, "No active QR", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap poster = buildPoster();
            File dir = new File(activity.getCacheDir(), "share");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "mess_qr.png");
            FileOutputStream fos = new FileOutputStream(file);
            poster.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, currentQr.qrUrl);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share QR"));
        } catch (Exception e) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, currentQr.qrUrl);
            startActivity(Intent.createChooser(share, "Share QR"));
        }
    }

    private void downloadQr() {
        if (activity == null || currentBitmap == null) {
            Toast.makeText(activity, "No active QR", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap poster = buildPoster();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "billingwala_mess_qr.png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Billingwala");
            Uri uri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = activity.getContentResolver().openOutputStream(uri);
                if (os != null) {
                    poster.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                }
                Toast.makeText(activity, "QR saved to Pictures/Billingwala", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Unable to save QR", Toast.LENGTH_SHORT).show();
        }
    }
}
