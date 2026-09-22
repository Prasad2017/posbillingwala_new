package com.pos_billingwala.Extra;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;


public class DetectConnection {

    /**
     * True when the device has a usable internet path on Wi‑Fi, mobile data,
     * Ethernet, or VPN. Uses NetworkCapabilities (reliable on modern OEMs /
     * dual‑SIM) instead of deprecated getActiveNetworkInfo().
     */
    public static boolean checkInternetConnection(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }

        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) {
            return false;
        }

        boolean hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean hasTransport =
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);

        // Do not require NET_CAPABILITY_VALIDATED: on some mobile/OEM builds it
        // stays unset even when cellular data is working.
        return hasInternet && hasTransport;
    }

    public static void noInternetConnection(Context context) {
        BottomSheetUi.showNoInternet(context);
    }


}
