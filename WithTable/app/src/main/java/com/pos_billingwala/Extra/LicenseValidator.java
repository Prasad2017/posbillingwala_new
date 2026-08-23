package com.pos_billingwala.Extra;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Model.LoginResponse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Production licensing: offline validation of server-signed license payloads.
 * Public key only — private signing key never ships in the APK.
 */
public final class LicenseValidator {

    private static final String KEY_PAYLOAD = "licensePayload";
    private static final String KEY_SIGNATURE = "licenseSignature";
    private static final String KEY_LAST_SERVER_TIME = "licenseLastServerTimeMs";
    private static final long CLOCK_ROLLBACK_TOLERANCE_MS = TimeUnit.HOURS.toMillis(24);

    private LicenseValidator() {
    }

    public static void saveFromLogin(Context context, LoginResponse response) {
        if (response == null) {
            return;
        }
        Common.saveUserData(context, KEY_PAYLOAD, nullToEmpty(response.getLicensePayload()));
        Common.saveUserData(context, KEY_SIGNATURE, nullToEmpty(response.getLicenseSignature()));
        Common.saveUserData(context, "organizationId", nullToEmpty(response.getOrganizationId()));
        Common.saveUserData(context, "branchId", nullToEmpty(response.getBranchId()));
        Common.saveUserData(context, "branchLabel", nullToEmpty(response.getBranchLabel()));
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Common.saveUserData(context, "deviceId", deviceId != null ? deviceId : "");
        Common.saveUserData(context, "issuedAt", nullToEmpty(response.getIssuedAt()));
        Common.saveUserData(context, "offlineGraceUntil", nullToEmpty(response.getOfflineGraceUntil()));
        Common.saveUserData(context, "trialConsumed", nullToEmpty(response.getTrialConsumed()));

        long issuedAtSec = parseLong(response.getIssuedAt(), 0L);
        if (issuedAtSec > 0L) {
            long issuedAtMs = issuedAtSec * 1000L;
            long lastKnown = parseLong(Common.getSavedUserData(context, KEY_LAST_SERVER_TIME), 0L);
            if (issuedAtMs > lastKnown) {
                Common.saveUserData(context, KEY_LAST_SERVER_TIME, String.valueOf(issuedAtMs));
            }
        }
    }

    public static boolean hasStoredPayload(Context context) {
        String payload = Common.getSavedUserData(context, KEY_PAYLOAD);
        String signature = Common.getSavedUserData(context, KEY_SIGNATURE);
        return payload != null && !payload.trim().isEmpty()
                && signature != null && !signature.trim().isEmpty();
    }

    /**
     * True when locally validated license allows billing (works offline when payload is still valid).
     */
    public static boolean isValidForBilling(Context context, POSBillingWalaDatabase database) {
        ValidationResult result = validate(context, database);
        return result.valid && !result.trialBillBlocked;
    }

    public static ValidationResult validate(Context context, POSBillingWalaDatabase database) {
        ValidationResult result = new ValidationResult();

        if (!hasStoredPayload(context)) {
            result.message = "License not activated. Connect online once to refresh.";
            return result;
        }

        SignedPayload payload = verifyAndParse(context);
        if (payload == null) {
            result.message = "Invalid license signature.";
            return result;
        }

        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null || !deviceId.equals(payload.deviceId)) {
            result.message = "License is bound to another device.";
            return result;
        }

        String storedKey = Common.getSavedUserData(context, "LicenceKey");
        if (storedKey != null && !storedKey.isEmpty() && !storedKey.equals(payload.licenseKey)) {
            result.message = "License key mismatch.";
            return result;
        }

        long trustedNowMs = trustedNowMillis(context);
        if (detectClockRollback(context, trustedNowMs)) {
            result.message = "Device date appears incorrect. Connect online to refresh license.";
            return result;
        }

        if (!isExpiryValid(payload.expiryDate, trustedNowMs)) {
            result.message = "License expired.";
            return result;
        }

        long offlineGraceUntilMs = payload.offlineGraceUntil * 1000L;
        if (trustedNowMs > offlineGraceUntilMs) {
            result.message = "Offline license grace expired. Connect online briefly to refresh.";
            return result;
        }

        if (payload.trialConsumed == 1 || (payload.isTrial == 1 && isTrialConsumedByRules(context, database, payload))) {
            result.message = "Trial already used. Please upgrade your licence.";
            result.trialBillBlocked = true;
            return result;
        }

        if (payload.isTrial == 1 && isTrialBillCapReached(context, database, payload)) {
            result.message = blockedTrialMessage(payload);
            result.trialBillBlocked = true;
            return result;
        }

        result.valid = true;
        result.payload = payload;
        result.message = "OK";
        return result;
    }

    private static boolean isTrialConsumedByRules(Context context, POSBillingWalaDatabase database, SignedPayload payload) {
        if ("1".equals(Common.getSavedUserData(context, "trialConsumed"))) {
            return true;
        }
        return payload.trialConsumed == 1;
    }

    private static boolean isTrialBillCapReached(Context context, POSBillingWalaDatabase database, SignedPayload payload) {
        if (payload.trialMaxBills <= 0) {
            return false;
        }
        int localCount = database != null ? database.getTotalInvoiceCount() : 0;
        int serverCount = payload.trialBillCount;
        try {
            String serverRaw = Common.getSavedUserData(context, "trialBillCount");
            if (serverRaw != null && !serverRaw.trim().isEmpty()) {
                serverCount = Math.max(serverCount, Integer.parseInt(serverRaw.trim()));
            }
        } catch (NumberFormatException ignored) {
        }
        return Math.max(localCount, serverCount) >= payload.trialMaxBills;
    }

    private static String blockedTrialMessage(SignedPayload payload) {
        if (payload.trialMaxBills > 0) {
            return "Trial bill limit reached (max " + payload.trialMaxBills + "). Please upgrade your licence.";
        }
        return "Trial bill limit reached. Please upgrade your licence.";
    }

    private static SignedPayload verifyAndParse(Context context) {
        try {
            String payloadB64 = Common.getSavedUserData(context, KEY_PAYLOAD);
            String signatureB64 = Common.getSavedUserData(context, KEY_SIGNATURE);
            if (payloadB64 == null || signatureB64 == null) {
                return null;
            }

            byte[] payloadBytes = Base64.decode(payloadB64.trim(), Base64.DEFAULT);
            byte[] signatureBytes = Base64.decode(signatureB64.trim(), Base64.DEFAULT);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);

            PublicKey publicKey = loadPublicKey(context);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(payloadBytes);
            if (!verifier.verify(signatureBytes)) {
                return null;
            }

            return new Gson().fromJson(payloadJson, SignedPayload.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static PublicKey loadPublicKey(Context context) throws Exception {
        InputStream in = context.getAssets().open("license_signing_public.pem");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder pem = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.contains("BEGIN") && !line.contains("END")) {
                pem.append(line.trim());
            }
        }
        reader.close();

        byte[] decoded = Base64.decode(pem.toString(), Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static long trustedNowMillis(Context context) {
        long deviceNow = System.currentTimeMillis();
        long lastServer = parseLong(Common.getSavedUserData(context, KEY_LAST_SERVER_TIME), 0L);
        return Math.max(deviceNow, lastServer);
    }

    private static boolean detectClockRollback(Context context, long trustedNowMs) {
        long lastServer = parseLong(Common.getSavedUserData(context, KEY_LAST_SERVER_TIME), 0L);
        if (lastServer <= 0L) {
            return false;
        }
        long deviceNow = System.currentTimeMillis();
        return deviceNow + CLOCK_ROLLBACK_TOLERANCE_MS < lastServer
                || trustedNowMs + CLOCK_ROLLBACK_TOLERANCE_MS < lastServer;
    }

    private static boolean isExpiryValid(String expiryDateYmd, long trustedNowMs) {
        if (expiryDateYmd == null || expiryDateYmd.trim().isEmpty()) {
            return false;
        }
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date expiry = dateFormat.parse(expiryDateYmd.trim());
            if (expiry == null) {
                return false;
            }
            Calendar endOfExpiry = Calendar.getInstance(Locale.ENGLISH);
            endOfExpiry.setTime(expiry);
            endOfExpiry.set(Calendar.HOUR_OF_DAY, 23);
            endOfExpiry.set(Calendar.MINUTE, 59);
            endOfExpiry.set(Calendar.SECOND, 59);
            endOfExpiry.set(Calendar.MILLISECOND, 999);
            return trustedNowMs <= endOfExpiry.getTimeInMillis();
        } catch (ParseException e) {
            return false;
        }
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    public static final class ValidationResult {
        public boolean valid;
        public boolean trialBillBlocked;
        public String message = "";
        public SignedPayload payload;
    }

    /** Mirrors server-signed JSON payload fields. */
    public static final class SignedPayload {
        public int payloadVersion;
        public String organizationId;
        public String branchId;
        public String branchLabel;
        public String licenseId;
        public String deviceId;
        public String licenseKey;
        public String licenseType;
        public int isTrial;
        public int trialMaxBills;
        public int trialBillCount;
        public int trialConsumed;
        public String expiryDate;
        public long issuedAt;
        public long offlineGraceUntil;
        public int fastBilling;
        public int takeAway;
        public int dineIn;
        public int mess;
    }
}
