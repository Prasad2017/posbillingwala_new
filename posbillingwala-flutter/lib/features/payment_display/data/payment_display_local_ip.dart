import 'dart:io';

/* Resolve a LAN IPv4 suitable for hotspot / Wi-Fi pairing URLs. */
class PaymentDisplayLocalIp {
  PaymentDisplayLocalIp._();

  static Future<String?> detect() async {
    try {
      final interfaces = await NetworkInterface.list(
        includeLinkLocal: false,
        type: InternetAddressType.IPv4,
      );
      final candidates = <String>[];
      for (final iface in interfaces) {
        final name = iface.name.toLowerCase();
        for (final addr in iface.addresses) {
          if (addr.isLoopback) continue;
          final ip = addr.address;
          if (_isUsableLan(ip)) {
            candidates.add(ip);
            /* Prefer typical hotspot / private Wi-Fi adapters. */
            if (name.contains('wlan') ||
                name.contains('wifi') ||
                name.contains('ap') ||
                name.contains('swlan') ||
                name.contains('rmnet') ||
                name.contains('eth')) {
              return ip;
            }
          }
        }
      }
      if (candidates.isNotEmpty) return candidates.first;
    } catch (_) {
      /* Ignore — caller shows error state. */
    }
    return null;
  }

  static bool _isUsableLan(String ip) {
    if (ip.startsWith('127.')) return false;
    if (ip.startsWith('169.254.')) return false;
    return ip.startsWith('192.168.') ||
        ip.startsWith('10.') ||
        RegExp(r'^172\.(1[6-9]|2\d|3[0-1])\.').hasMatch(ip);
  }
}
