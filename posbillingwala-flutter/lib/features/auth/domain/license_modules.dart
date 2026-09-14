import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';

/* WithTable `LicenseModules` — hide POS modes the shop did not buy. */
/* If every flag is off (legacy payload), treat all modules as enabled. */
class LicenseModules {
  const LicenseModules._();

  static bool flagsMissing(UserSession? session) {
    if (session == null) return true;
    return !session.fastBilling &&
        !session.dineIn &&
        !session.takeAway &&
        !session.mess;
  }

  static bool allow(UserSession? session, bool Function(UserSession s) flag) {
    if (session == null || flagsMissing(session)) return true;
    return flag(session);
  }

  static bool fastBilling(UserSession? s) => allow(s, (x) => x.fastBilling);
  static bool dineIn(UserSession? s) => allow(s, (x) => x.dineIn);
  static bool takeAway(UserSession? s) => allow(s, (x) => x.takeAway);
  static bool mess(UserSession? s) => allow(s, (x) => x.mess);
}
