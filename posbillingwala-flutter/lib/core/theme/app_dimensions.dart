/// Spacing, radii, and control sizes for consistent layout.
///
/// Prefer [AppBreakpoints] for width-dependent padding / max width.
/// [maxContentWidth] remains for narrow forms (auth); dashboards use
/// [maxDashboardWidth] / breakpoint helpers instead.
abstract final class AppDimensions {
  // Spacing
  static const double xs = 4;
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 24;
  static const double xxl = 32;

  // Radii
  static const double radiusSm = 10;
  static const double radiusMd = 14;
  static const double radiusLg = 18;
  static const double radiusXl = 24;
  static const double radiusPill = 999;

  // Icons
  static const double iconSm = 18;
  static const double iconMd = 22;
  static const double iconLg = 28;
  static const double iconXl = 36;

  // Controls
  static const double inputHeight = 52;
  static const double buttonHeight = 52;
  static const double appBarAction = 44;
  static const double cardPadding = 16;
  static const double pagePadding = 16;
  static const double minTapTarget = 48;

  /// Narrow forms (login / MPIN) — prefer [AppBreakpoints.contentMaxWidthFor].
  static const double maxContentWidth = 560;

  /// Home / Reports / Masters on tablet & web.
  static const double maxDashboardWidth = 1400;

  /// POS catalog pane comfort width before side cart.
  static const double maxPosCatalogWidth = 1200;
}
