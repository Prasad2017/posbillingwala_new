package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Model.AllApiResponse;
import com.posbillingwala.owner.R;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.Utils.CatalogFileHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessMemberExcelFragment extends Fragment {

    private static final int PICK_XLSX = 5201;

    private Activity activity;
    private String licenceId;
    private String outletLabel;
    private TextView statusTxt;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_member_excel, container, false);
        activity = getActivity();

        Bundle args = getArguments();
        if (args != null) {
            licenceId = args.getString("licenceId", "");
            outletLabel = args.getString("outletLabel", "");
        }

        ImageView back = view.findViewById(R.id.backBtn);
        TextView outlet = view.findViewById(R.id.outletLabel);
        Button btnTemplate = view.findViewById(R.id.btnTemplate);
        Button btnExport = view.findViewById(R.id.btnExport);
        Button btnImport = view.findViewById(R.id.btnImport);
        statusTxt = view.findViewById(R.id.statusTxt);

        outlet.setText(outletLabel != null ? outletLabel : ("Licence #" + licenceId));
        back.setOnClickListener(v -> ((MainActivity) activity).removeCurrentFragmentAndMoveBack());
        btnTemplate.setOnClickListener(v -> download("messMemberTemplate.php?userId=" + MainActivity.userId,
                "mess_member_template.xlsx"));
        btnExport.setOnClickListener(v -> download(
                "messMemberExport.php?userId=" + MainActivity.userId + "&licenceId=" + licenceId,
                "mess_members_" + licenceId + ".xlsx"));
        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Excel"), PICK_XLSX);
        });
        return view;
    }

    private void download(String endpointQuery, String fileName) {
        statusTxt.setText("Downloading…");
        CatalogFileHelper.downloadCatalogFile(activity, endpointQuery, fileName,
                new CatalogFileHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File savedFile) {
                        if (activity == null) return;
                        activity.runOnUiThread(() -> {
                            statusTxt.setText("Saved: " + savedFile.getAbsolutePath());
                            Toast.makeText(activity, "Downloaded", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (activity == null) return;
                        activity.runOnUiThread(() -> {
                            statusTxt.setText(message);
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_XLSX || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            File temp = copyUriToCache(uri);
            if (temp == null) {
                Toast.makeText(activity, "Unable to read file", Toast.LENGTH_SHORT).show();
                return;
            }
            statusTxt.setText("Importing…");
            RequestBody userBody = RequestBody.create(MediaType.parse("text/plain"), MainActivity.userId);
            RequestBody licenceBody = RequestBody.create(MediaType.parse("text/plain"), licenceId);
            RequestBody fileBody = RequestBody.create(MediaType.parse(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), temp);
            MultipartBody.Part part = MultipartBody.Part.createFormData("importFile", temp.getName(), fileBody);
            Api.getClient().messMemberImport(userBody, licenceBody, part)
                    .enqueue(new Callback<AllApiResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<AllApiResponse> call, @NonNull Response<AllApiResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                statusTxt.setText(response.body().getMessage());
                                Toast.makeText(activity, response.body().getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                statusTxt.setText("Import failed");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<AllApiResponse> call, @NonNull Throwable t) {
                            statusTxt.setText(t.getMessage());
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToCache(Uri uri) throws Exception {
        String name = "mess_import.xlsx";
        Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx);
            }
            cursor.close();
        }
        File out = new File(activity.getCacheDir(), name);
        InputStream in = activity.getContentResolver().openInputStream(uri);
        if (in == null) return null;
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            fos.write(buf, 0, n);
        }
        fos.close();
        in.close();
        return out;
    }
}
