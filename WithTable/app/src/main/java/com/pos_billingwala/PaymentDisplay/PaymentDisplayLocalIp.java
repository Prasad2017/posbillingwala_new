package com.pos_billingwala.PaymentDisplay;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/** Detect LAN IPv4 for hotspot / Wi-Fi pairing URLs. */
public final class PaymentDisplayLocalIp {

    private PaymentDisplayLocalIp() {
    }

    public static String detect() {
        try {
            List<String> candidates = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }
                String name = iface.getName() != null ? iface.getName().toLowerCase(Locale.US) : "";
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress()) {
                        continue;
                    }
                    String ip = addr.getHostAddress();
                    if (!isUsableLan(ip)) {
                        continue;
                    }
                    candidates.add(ip);
                    if (name.contains("wlan") || name.contains("ap") || name.contains("swlan")
                            || name.contains("wifi") || name.contains("rmnet") || name.contains("eth")) {
                        return ip;
                    }
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isUsableLan(String ip) {
        if (ip == null) {
            return false;
        }
        if (ip.startsWith("127.") || ip.startsWith("169.254.")) {
            return false;
        }
        return ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");
    }
}
