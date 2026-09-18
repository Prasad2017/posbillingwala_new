import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';

/* Human-readable licence labels from session (`Demo`/`Trial`/`Testing`/`Regular`). */
class LicenceDisplay {
  LicenceDisplay._();

  static bool isTestingLicence(UserSession? session) {
    if (session == null) return false;
    if (session.isTrial) return true;
    final t = (session.licenseType ?? '').trim().toLowerCase();
    return t == 'demo' || t == 'trial' || t == 'testing';
  }

  static String planLabel(UserSession? session) {
    if (session == null) return 'Licence';
    if (isTestingLicence(session)) return 'Testing Licence';
    final t = (session.licenseType ?? '').trim();
    if (t.isEmpty) return 'Regular Licence';
    final lower = t.toLowerCase();
    if (lower == 'regular') return 'Regular Licence';
    return '${t[0].toUpperCase()}${t.substring(1)} Licence';
  }

  static String? expiryLabel(UserSession? session) {
    final raw = session?.licenceKeyExpireDate?.trim() ?? '';
    if (raw.isEmpty) return null;
    /* Keep YYYY-MM-DD when possible; otherwise show as stored. */
    if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-') {
      return 'Valid till ${raw.substring(0, 10)}';
    }
    return 'Valid till $raw';
  }

  static String subtitle(UserSession? session) {
    final expiry = expiryLabel(session);
    if (isTestingLicence(session)) {
      return expiry == null
          ? 'Free testing plan — upgrade for full access'
          : '$expiry · Upgrade for full access';
    }
    return expiry ?? 'Active paid plan';
  }
}
