/* Build-time feature flags. Flip here, then rebuild. */
abstract final class AppConfig {
  AppConfig._();

  /* Google Play in-app updates (Android). */
  static const bool enableInAppUpdate = true;

  /* true = screenshots allowed; false = block capture (FLAG_SECURE). */
  static const bool allowScreenshot = false;
}
