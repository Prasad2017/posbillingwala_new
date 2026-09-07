package com.pos_billingwala.NetworkToOffline;

import android.content.Context;

import com.pos_billingwala.Activity.MainActivity;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BusinessConfigStore;
import com.pos_billingwala.Extra.BusinessSession;
import com.pos_billingwala.Extra.Observability;
import com.pos_billingwala.Model.AllApiResponse;
import com.pos_billingwala.Model.CustomOrderDepositResponse;
import com.pos_billingwala.Model.ProductPriceTierResponse;
import com.pos_billingwala.Model.ProductVariantResponse;
import com.pos_billingwala.Model.ServiceAppointmentResponse;
import com.pos_billingwala.Model.StaffUserResponse;
import com.pos_billingwala.Retrofit.Api;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Upload pending Universal POS entities (appointments, deposits, tiers, variants,
 * business template, staff). Shared by Sync, connectivity receiver, and Fetch upload.
 */
public final class UniversalPendingUpload {

    private UniversalPendingUpload() {
    }

    public static void uploadAll(Context context, POSBillingWalaDatabase db) {
        if (context == null || db == null) {
            return;
        }
        Context app = context.getApplicationContext();
        uploadAppointments(app, db);
        uploadDeposits(app, db);
        uploadPriceTiers(app, db);
        uploadVariants(app, db);
        uploadBusinessTemplate(app);
        uploadStaff(app, db);
    }

    private static void uploadAppointments(Context context, POSBillingWalaDatabase db) {
        List<ServiceAppointmentResponse> pending = db.getPendingAppointmentsForSync(200);
        if (pending == null) {
            return;
        }
        for (ServiceAppointmentResponse a : pending) {
            if (a == null || a.getAppointmentId() == null) {
                continue;
            }
            if (execute(context, Api.getClient(context).saveServiceAppointment(
                    MainActivity.userId,
                    a.getAppointmentId(),
                    nz(a.getProductId()),
                    nz(a.getProductName()),
                    nz(a.getCustomerName()),
                    nz(a.getCustomerMobile()),
                    nz(a.getAppointmentAt()),
                    nz(a.getNotes()),
                    a.getAppointmentStatus() != null ? a.getAppointmentStatus() : "booked",
                    nz(a.getStaffId()),
                    nz(a.getStaffName()),
                    a.getAppointmentNetworkStatus() != null ? a.getAppointmentNetworkStatus() : "pending"))) {
                db.markAppointmentSynced(a.getAppointmentId());
            }
        }
    }

    private static void uploadDeposits(Context context, POSBillingWalaDatabase db) {
        List<CustomOrderDepositResponse> pending = db.getPendingDepositsForSync(200);
        if (pending == null) {
            return;
        }
        for (CustomOrderDepositResponse d : pending) {
            if (d == null || d.getDepositId() == null) {
                continue;
            }
            if (execute(context, Api.getClient(context).saveCustomOrderDeposit(
                    MainActivity.userId,
                    d.getDepositId(),
                    nz(d.getProductName()),
                    nz(d.getOrderNote()),
                    nz(d.getDepositAmount()),
                    nz(d.getDueDate()),
                    nz(d.getPhotoFile()),
                    d.getDepositStatus() != null ? d.getDepositStatus() : "open",
                    d.getDepositNetworkStatus() != null ? d.getDepositNetworkStatus() : "pending",
                    nz(d.getCreatedAt())))) {
                db.markDepositSynced(d.getDepositId());
            }
        }
    }

    private static void uploadPriceTiers(Context context, POSBillingWalaDatabase db) {
        List<ProductPriceTierResponse> pending = db.getPendingPriceTiersForSync(500);
        if (pending == null) {
            return;
        }
        for (ProductPriceTierResponse t : pending) {
            if (t == null || t.getTierId() == null) {
                continue;
            }
            if (execute(context, Api.getClient(context).saveProductPriceTier(
                    MainActivity.userId,
                    t.getTierId(),
                    nz(t.getProductId()),
                    nz(t.getProductNetworkStatus()),
                    t.getMinQty() != null ? t.getMinQty() : "1",
                    nz(t.getTierPrice()),
                    nz(t.getTierLabel()),
                    t.getTierDeletedStatus() != null ? t.getTierDeletedStatus() : "0",
                    t.getTierNetworkStatus() != null ? t.getTierNetworkStatus() : "pending"))) {
                db.markPriceTierSynced(t.getTierId());
            }
        }
    }

    private static void uploadVariants(Context context, POSBillingWalaDatabase db) {
        List<ProductVariantResponse> pending = db.getPendingVariantsForSync(500);
        if (pending == null) {
            return;
        }
        for (ProductVariantResponse v : pending) {
            if (v == null || v.getVariantId() == null) {
                continue;
            }
            if (execute(context, Api.getClient(context).saveProductVariant(
                    MainActivity.userId,
                    v.getVariantId(),
                    nz(v.getProductId()),
                    nz(v.getProductNetworkStatus()),
                    nz(v.getVariantSize()),
                    nz(v.getVariantColor()),
                    nz(v.getVariantSku()),
                    nz(v.getVariantPrice()),
                    v.getVariantDeletedStatus() != null ? v.getVariantDeletedStatus() : "0",
                    v.getVariantSortOrder() != null ? v.getVariantSortOrder() : "0",
                    v.getVariantNetworkStatus() != null ? v.getVariantNetworkStatus() : "pending"))) {
                db.markVariantSynced(v.getVariantId());
            }
        }
    }

    private static void uploadBusinessTemplate(Context context) {
        if (!BusinessSession.isTemplatePendingSync(context)) {
            return;
        }
        String type = BusinessSession.getBusinessType(context);
        String templateId = BusinessSession.getTemplateId(context);
        String json = BusinessConfigStore.getTemplateJson(context);
        if (execute(context, Api.getClient(context).saveBusinessTemplate(
                MainActivity.userId,
                type != null ? type : "restaurant",
                templateId != null ? templateId : "restaurant_default",
                json != null ? json : "",
                "pending"))) {
            BusinessSession.markTemplateSynced(context);
        }
    }

    private static void uploadStaff(Context context, POSBillingWalaDatabase db) {
        List<StaffUserResponse> pending = db.getPendingStaffUsersForSync(200);
        if (pending == null) {
            return;
        }
        for (StaffUserResponse s : pending) {
            if (s == null || s.getStaffId() == null) {
                continue;
            }
            if (execute(context, Api.getClient(context).saveStaffUser(
                    MainActivity.userId,
                    s.getStaffId(),
                    nz(s.getStaffName()),
                    s.getStaffRole() != null ? s.getStaffRole() : "cashier",
                    nz(s.getStaffPin()),
                    s.getStaffActive() != null ? s.getStaffActive() : "1",
                    s.getStaffDeletedStatus() != null ? s.getStaffDeletedStatus() : "0",
                    s.getStaffNetworkStatus() != null ? s.getStaffNetworkStatus() : "pending",
                    nz(s.getCreatedAt())))) {
                db.markStaffUserSynced(s.getStaffId());
            }
        }
    }

    private static boolean execute(Context context, Call<AllApiResponse> call) {
        try {
            Response<AllApiResponse> response = call.execute();
            boolean ok = response.isSuccessful() && response.body() != null
                    && "1".equalsIgnoreCase(response.body().getStatus());
            if (!ok) {
                String status = response.body() != null ? response.body().getStatus() : "null_body";
                String msg = response.body() != null ? response.body().getMessage() : "";
                Observability.log("universal_pending_upload failed | HTTP " + response.code()
                        + " | status=" + status + " | msg=" + msg);
            }
            return ok;
        } catch (Exception e) {
            Observability.logNonFatal(e, "universal_pending_upload");
            return false;
        }
    }

    private static String nz(String value) {
        return value != null ? value : "";
    }
}
