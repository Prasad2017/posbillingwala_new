package com.posbillingwala.owner.Fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.posbillingwala.owner.Activity.MainActivity;
import com.posbillingwala.owner.Extra.DetectConnection;
import com.posbillingwala.owner.Model.StoreOpsResponse;
import com.posbillingwala.owner.Retrofit.Api;
import com.posbillingwala.owner.databinding.FragmentStoreOpsBinding;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoreOpsFragment extends Fragment {

    private Activity activity;
    private FragmentStoreOpsBinding binding;
    private String licenseId = "";
    private String shopName = "";

    public static StoreOpsFragment newInstance(String licenseId, String shopName) {
        StoreOpsFragment fragment = new StoreOpsFragment();
        Bundle args = new Bundle();
        args.putString("licenseId", licenseId);
        args.putString("shopName", shopName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStoreOpsBinding.inflate(inflater, container, false);
        activity = getActivity();
        if (getArguments() != null) {
            licenseId = getArguments().getString("licenseId", "");
            shopName = getArguments().getString("shopName", "");
        }
        binding.toolbarTitle.setText(shopName.isEmpty() ? "Store operations" : shopName);
        binding.backButton.setOnClickListener(v -> ((MainActivity) activity).removeCurrentFragmentAndMoveBack());
        View root = binding.getRoot();
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                ((MainActivity) activity).removeCurrentFragmentAndMoveBack();
                return true;
            }
            return false;
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (activity != null && DetectConnection.checkInternetConnection(activity)) {
            loadOps();
        } else if (binding != null) {
            binding.opsBody.setText("No internet connection.");
        }
    }

    private void loadOps() {
        SweetAlertDialog pDialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#2D7FED"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();
        Api.getClient().getStoreOpsSummary(MainActivity.userId, licenseId).enqueue(new Callback<StoreOpsResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoreOpsResponse> call, @NonNull Response<StoreOpsResponse> response) {
                pDialog.dismiss();
                if (!isAdded() || binding == null) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    binding.opsBody.setText(format(response.body()));
                } else {
                    binding.opsBody.setText("Unable to load store operations.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<StoreOpsResponse> call, @NonNull Throwable t) {
                pDialog.dismiss();
                if (isAdded() && binding != null) {
                    binding.opsBody.setText(t.getMessage() == null ? "Unable to load store operations." : t.getMessage());
                }
            }
        });
    }

    private String format(StoreOpsResponse ops) {
        StringBuilder out = new StringBuilder();
        boolean um = "1".equals(ops.userManagementEnabled);
        out.append("User Management: ").append(um ? "ON" : "OFF").append("\n");
        out.append("Limits: ").append(n(ops.maxUsers)).append(" users, ")
                .append(n(ops.maxDevices)).append(" devices, ")
                .append(n(ops.maxPrinters)).append(" printers (0 = no cap)\n");
        out.append("Active: ").append(n(ops.staffCount)).append(" users, ")
                .append(n(ops.deviceCount)).append(" devices, ")
                .append(n(ops.printerCount)).append(" printers\n");

        out.append("\nUSERS / ROLES / PERMISSIONS\n");
        if (ops.staffResponse == null || ops.staffResponse.isEmpty()) {
            out.append(um ? "No staff yet. Create users in POS.\n" : "Single-user licence (UM off).\n");
        } else {
            for (StoreOpsResponse.Staff staff : ops.staffResponse) {
                out.append("• ").append(n(staff.name)).append(" (").append(n(staff.roleLabel)).append(")\n");
                out.append("  ").append(n(staff.mobileNumber)).append("  ").append(n(staff.status)).append("\n");
                if (staff.lastLoginAt != null && !staff.lastLoginAt.isEmpty()) {
                    out.append("  Last login: ").append(staff.lastLoginAt).append("\n");
                }
                List<String> perms = staff.allowedPermissions;
                if (perms != null && !perms.isEmpty()) {
                    out.append("  Permissions: ").append(String.join(", ", perms)).append("\n");
                }
            }
        }

        out.append("\nDEVICES\n");
        if (ops.deviceResponse == null || ops.deviceResponse.isEmpty()) {
            out.append("No extra POS devices registered.\n");
        } else {
            for (StoreOpsResponse.Device device : ops.deviceResponse) {
                out.append("• ").append(n(device.deviceName).isEmpty() ? n(device.deviceId) : device.deviceName)
                        .append("  ").append(n(device.platform)).append("  ").append(n(device.status));
                if ("1".equals(device.isPrintHost)) {
                    out.append("  PRINT HOST");
                }
                out.append("\n");
            }
        }

        out.append("\nPRINTERS\n");
        if (ops.printerResponse == null || ops.printerResponse.isEmpty()) {
            out.append("Using existing bill/KOT printer settings.\n");
        } else {
            for (StoreOpsResponse.Printer printer : ops.printerResponse) {
                out.append("• ").append(n(printer.printerName)).append("  ")
                        .append(n(printer.connectionType)).append("  ")
                        .append(n(printer.paperSize)).append("  ")
                        .append(n(printer.purpose)).append("  ")
                        .append("1".equals(printer.enabled) ? "enabled" : "disabled")
                        .append("\n");
            }
        }

        out.append("\nPRINTER ROUTING\n");
        if (ops.routeResponse == null || ops.routeResponse.isEmpty()) {
            out.append("No category routes yet.\n");
        } else {
            for (StoreOpsResponse.Route route : ops.routeResponse) {
                out.append("• ").append(n(route.documentType))
                        .append(" → printer #").append(n(route.printerId));
                if (route.foodTypeCode != null && !route.foodTypeCode.isEmpty()) {
                    out.append("  ").append(route.foodTypeCode);
                }
                if (route.categoryId != null && !route.categoryId.isEmpty()) {
                    out.append("  cat ").append(route.categoryId);
                }
                out.append("\n");
            }
        }
        return out.toString();
    }

    private String n(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value.trim();
    }
}
