package com.posbillingwala.admin.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.admin.Activity.MainActivity;
import com.posbillingwala.admin.Extra.DetectConnection;
import com.posbillingwala.admin.Model.AllApiResponse;
import com.posbillingwala.admin.Model.ErrorLogDetail;
import com.posbillingwala.admin.Retrofit.Api;
import com.posbillingwala.admin.databinding.FragmentCrashErrorLogDetailBinding;
import com.posbillingwala.admin.databinding.IncludeErrorLogSectionBinding;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrashErrorLogDetail extends Fragment {

    private static final String ARG_ID = "error_log_id";

    Activity activity;
    FragmentCrashErrorLogDetailBinding binding;
    String logId;
    ErrorLogDetail detail;

    public static CrashErrorLogDetail newInstance(String id) {
        CrashErrorLogDetail f = new CrashErrorLogDetail();
        Bundle b = new Bundle();
        b.putString(ARG_ID, id);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCrashErrorLogDetailBinding.inflate(inflater, container, false);
        activity = getActivity();
        logId = getArguments() != null ? getArguments().getString(ARG_ID) : null;
        MainActivity.title.setText("Error Detail");
        MainActivity.back.setOnClickListener(v ->
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack());

        binding.btnSaveResolution.setOnClickListener(v -> saveResolution());
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.title.setVisibility(View.VISIBLE);
        ((MainActivity) activity).lockUnlockDrawer(0);
        if (DetectConnection.checkInternetConnection(activity)) {
            loadDetail();
        } else {
            DetectConnection.noInternetConnection(activity);
        }
    }

    private void loadDetail() {
        if (logId == null || logId.isEmpty()) {
            binding.loadingOrEmpty.setText("Missing log id");
            return;
        }
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().getErrorLogDetails(logId).enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                detail = response.body() != null ? response.body().getErrorLogDetail() : null;
                if (detail == null) {
                    binding.loadingOrEmpty.setText("Log not found");
                    binding.detailContent.setVisibility(View.GONE);
                    return;
                }
                binding.loadingOrEmpty.setVisibility(View.GONE);
                binding.detailContent.setVisibility(View.VISIBLE);
                binding.resolutionLayout.setVisibility(View.VISIBLE);
                binding.btnSaveResolution.setVisibility(View.VISIBLE);
                if (detail.getResolutionNotes() != null) {
                    binding.resolutionNotes.setText(detail.getResolutionNotes());
                }
                renderSections(detail);
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                binding.loadingOrEmpty.setText("Failed to load detail");
            }
        });
    }

    private void renderSections(ErrorLogDetail d) {
        LinearLayout container = binding.detailContent;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);

        addSection(container, inflater, "Summary", buildSummary(d), true);
        addSection(container, inflater, "Original Device/SDK Error", buildOriginal(d), false);
        addSection(container, inflater, "User Context", buildUserContext(d), true);
        addSection(container, inflater, "Screen & Action", buildScreenAction(d), true);
        addSection(container, inflater, "User Journey", nz(d.getUserFlow()), true);

        boolean isApi = "API".equalsIgnoreCase(nz(d.getErrorType()))
                || (d.getApiUrl() != null && !d.getApiUrl().isEmpty());
        if (isApi) {
            addSection(container, inflater, "API Information", buildApiInfo(d), true);
            addSection(container, inflater, "Request", formatBody(d.getRequestBody(), d.getRequestSize()), false);
            addSection(container, inflater, "Response", formatBody(
                    firstNonEmpty(d.getOriginalApiResponse(), d.getResponseBody()), d.getResponseSize()), false);
        }

        boolean isPrinter = "PRINTER".equalsIgnoreCase(nz(d.getErrorType()))
                || (d.getPrinterType() != null && !d.getPrinterType().isEmpty());
        if (isPrinter) {
            addSection(container, inflater, "Printer", buildPrinter(d), true);
        }

        addSection(container, inflater, "Device / Application", buildDeviceApp(d), false);
        addSection(container, inflater, "Stack Trace", nz(d.getOriginalStackTrace()), false);
        addSection(container, inflater, "Occurrences", buildOccurrences(d), true);
    }

    private void addSection(LinearLayout parent, LayoutInflater inflater,
                            String title, String body, boolean expanded) {
        IncludeErrorLogSectionBinding section =
                IncludeErrorLogSectionBinding.inflate(inflater, parent, false);
        section.sectionTitle.setText(title);
        section.sectionBody.setText(body != null && !body.isEmpty() ? body : "—");
        setExpanded(section, expanded);
        section.sectionHeader.setOnClickListener(v ->
                setExpanded(section, section.sectionBody.getVisibility() != View.VISIBLE));
        parent.addView(section.getRoot());
    }

    private void setExpanded(IncludeErrorLogSectionBinding section, boolean expanded) {
        section.sectionBody.setVisibility(expanded ? View.VISIBLE : View.GONE);
        section.sectionToggle.setText(expanded ? "▼" : "▶");
    }

    private String buildSummary(ErrorLogDetail d) {
        return "Severity: " + nz(d.getSeverity())
                + "\nType: " + nz(d.getErrorType())
                + "\nCategory: " + nz(d.getErrorCategory())
                + "\n\n" + nz(d.getSummary())
                + (isEmpty(d.getWhatHappened()) ? "" : "\n\nWhat happened:\n" + d.getWhatHappened());
    }

    private String buildOriginal(ErrorLogDetail d) {
        return "Exception: " + nz(d.getOriginalExceptionClass())
                + "\nError code: " + nz(d.getOriginalErrorCode())
                + "\n\nOriginal message:\n" + nz(d.getOriginalErrorMessage())
                + (isEmpty(d.getOriginalApiResponse()) ? ""
                : "\n\nOriginal API response:\n" + d.getOriginalApiResponse());
    }

    private String buildUserContext(ErrorLogDetail d) {
        return "Customer: " + nz(d.getShopName())
                + "\nBranch: " + nz(d.getBranchLabel())
                + "\nDevice: " + nz(d.getDeviceName())
                + "\nUser: " + nz(d.getUserLabel())
                + "\nCustomer ID: " + nz(d.getCustomerId());
    }

    private String buildScreenAction(ErrorLogDetail d) {
        return "Screen: " + nz(d.getScreenName())
                + "\nActivity: " + nz(d.getActivityName())
                + "\nFragment: " + nz(d.getFragmentName())
                + "\nAction: " + nz(d.getUserAction());
    }

    private String buildApiInfo(ErrorLogDetail d) {
        String duration = nz(d.getRequestDurationMs());
        return "Method: " + nz(d.getApiMethod())
                + "\nURL: " + nz(d.getApiUrl())
                + "\nHTTP Status: " + nz(d.getHttpStatus())
                + "\nDuration: " + (duration.equals("-") ? "-" : duration + " ms");
    }

    private String buildPrinter(ErrorLogDetail d) {
        return "Type: " + nz(d.getPrinterType())
                + "\nModel: " + nz(d.getPrinterModel())
                + "\nConnection: " + nz(d.getPrinterConnection())
                + "\nOperation: " + nz(d.getPrintOperation())
                + "\n\nOriginal SDK error:\n" + nz(d.getOriginalErrorMessage())
                + "\nCode: " + nz(d.getOriginalErrorCode());
    }

    private String buildDeviceApp(ErrorLogDetail d) {
        return "App: " + nz(d.getAppType()) + " v" + nz(d.getAppVersion())
                + "\nDevice: " + nz(d.getDeviceName())
                + "\nDevice ID: " + nz(d.getDeviceId());
    }

    private String buildOccurrences(ErrorLogDetail d) {
        return "Count: " + nz(d.getOccurrenceCount())
                + "\nFirst seen: " + nz(d.getFirstSeenAt())
                + "\nLast seen: " + nz(d.getLastSeenAt())
                + "\nFingerprint: " + nz(d.getFingerprint());
    }

    private String formatBody(String body, String size) {
        String s = body == null || body.isEmpty() ? "—" : body;
        if (size != null && !size.isEmpty()) {
            return "Size: " + size + " bytes\n\n" + s;
        }
        return s;
    }

    private void saveResolution() {
        if (logId == null) {
            return;
        }
        String notes = binding.resolutionNotes.getText() != null
                ? binding.resolutionNotes.getText().toString().trim() : "";
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Saving");
        pDialog.setCancelable(false);
        pDialog.show();

        Api.getClient().updateErrorLogResolution(logId, notes, "admin").enqueue(new Callback<AllApiResponse>() {
            @Override
            public void onResponse(Call<AllApiResponse> call, Response<AllApiResponse> response) {
                pDialog.dismiss();
                boolean ok = response.body() != null && "1".equals(response.body().getStatus());
                Toast.makeText(activity, ok ? "Resolution saved" : "Save failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AllApiResponse> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(activity, "Save failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String nz(String v) {
        return v == null || v.isEmpty() ? "-" : v;
    }

    private static boolean isEmpty(String v) {
        return v == null || v.isEmpty();
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.isEmpty()) {
            return a;
        }
        return b;
    }
}
