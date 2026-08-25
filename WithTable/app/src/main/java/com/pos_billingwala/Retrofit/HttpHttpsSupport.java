package com.pos_billingwala.Retrofit;

import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

/**
 * Shared HTTP + HTTPS support for the POS Billingwala host.
 * Server currently uses a self-signed cert with a mismatched CN, so we
 * trust that host via network_security_config and relax hostname checks
 * only for *.posbillingwala.com.
 */
public final class HttpHttpsSupport {

    private HttpHttpsSupport() {
    }

    public static boolean isOurHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        String h = host.toLowerCase();
        return h.equals("posbillingwala.com")
                || h.endsWith(".posbillingwala.com");
    }

    public static HostnameVerifier hostnameVerifier() {
        final HostnameVerifier platform = HttpsURLConnection.getDefaultHostnameVerifier();
        return (hostname, session) -> {
            if (isOurHost(hostname)) {
                return true;
            }
            return platform.verify(hostname, session);
        };
    }

    /** Call once from Application so Picasso / HttpURLConnection HTTPS also works. */
    public static void installPlatformHostnameVerifier() {
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier());
    }

    public static void applyTo(OkHttpClient.Builder builder) {
        builder.hostnameVerifier(hostnameVerifier())
                .connectionSpecs(Arrays.asList(
                        ConnectionSpec.CLEARTEXT,
                        ConnectionSpec.MODERN_TLS,
                        ConnectionSpec.COMPATIBLE_TLS));
    }
}
