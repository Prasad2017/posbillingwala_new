import 'package:flutter/widgets.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';

/* Width classes for phone → tablet → desktop/web (see POS_UPGRADE_PLAN §4.3). */
enum AppWidthClass {
  /* Phone portrait — typically &lt; 600 */
  compact,

  /* Phone landscape / small tablet — ≥ 600 */
  medium,

  /* Tablet / web POS (side cart) — ≥ 1000 */
  expanded,

  /* Wide web dashboard — ≥ 1280 */
  large,
}

/* Single source of truth for layout breakpoints — no magic numbers in pages. */
abstract final class AppBreakpoints {
  static const double compactMax = 600;
  static const double mediumMin = 600;
  static const double expandedMin = 1000;
  static const double largeMin = 1280;

  static AppWidthClass ofWidth(double width) {
    if (width >= largeMin) return AppWidthClass.large;
    if (width >= expandedMin) return AppWidthClass.expanded;
    if (width >= mediumMin) return AppWidthClass.medium;
    return AppWidthClass.compact;
  }

  static AppWidthClass of(BuildContext context) =>
      ofWidth(MediaQuery.sizeOf(context).width);

  static AppWidthClass ofConstraints(BoxConstraints constraints) =>
      ofWidth(constraints.maxWidth);

  /* POS / Tables / Takeaway: persistent side cart. */
  /* Web: always-on cart (side-by-side except compact, which stacks). */
  /* Android/iOS: side cart from expanded tablet width up. */
  static bool isPosSideCart(AppWidthClass w) {
    if (AppPlatform.useDesktopShell) {
      return w != AppWidthClass.compact;
    }
    return w.index >= AppWidthClass.expanded.index;
  }

  static bool isPosPersistentCart(AppWidthClass w) =>
      AppPlatform.useDesktopShell || w.index >= AppWidthClass.expanded.index;

  static double posSideCartWidth(AppWidthClass w) {
    if (AppPlatform.useDesktopShell) {
      return switch (w) {
        AppWidthClass.compact => 280,
        AppWidthClass.medium => 320,
        AppWidthClass.expanded => 360,
        AppWidthClass.large => 400,
      };
    }
    return w == AppWidthClass.large ? 400 : 360;
  }

  /* Home / hub module grids: 2 → 3 → 4. */
  static int moduleColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 2,
    AppWidthClass.medium => 3,
    AppWidthClass.expanded => 4,
    AppWidthClass.large => 4,
  };

  /* POS product catalog: 2 → 3 → 4 → 5. */
  static int productColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 2,
    AppWidthClass.medium => 3,
    AppWidthClass.expanded => 4,
    AppWidthClass.large => 5,
  };

  /* Tables floor grid: 2 → 3 → 4 → 5. */
  static int tableColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 2,
    AppWidthClass.medium => 3,
    AppWidthClass.expanded => 4,
    AppWidthClass.large => 5,
  };

  /* Reports / masters card grids: 1 → 2 → 3. */
  static int cardColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 1,
    AppWidthClass.medium => 2,
    AppWidthClass.expanded => 2,
    AppWidthClass.large => 3,
  };

  /* Settings groups: single column until large, then 2. */
  static int settingsColumnsFor(AppWidthClass w) =>
      w == AppWidthClass.large ? 2 : 1;

  /* Readable content max width by class (auth / settings forms). */
  static double contentMaxWidthFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 560,
    AppWidthClass.medium => 720,
    AppWidthClass.expanded => 960,
    AppWidthClass.large => 1200,
  };

  /* Hub / dashboard pages can stretch wider than forms. */
  static double dashboardMaxWidthFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 560,
    AppWidthClass.medium => 900,
    AppWidthClass.expanded => 1100,
    AppWidthClass.large => 1400,
  };

  /* Horizontal page padding scales up slightly on wide screens. */
  static double pagePaddingFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 16,
    AppWidthClass.medium => 20,
    AppWidthClass.expanded => 24,
    AppWidthClass.large => 28,
  };
}

extension AppBreakpointsContext on BuildContext {
  AppWidthClass get widthClass => AppBreakpoints.of(this);

  bool get isCompactWidth => widthClass == AppWidthClass.compact;

  bool get isMediumWidth => widthClass == AppWidthClass.medium;

  bool get isExpandedWidth => widthClass.index >= AppWidthClass.expanded.index;

  bool get isLargeWidth => widthClass == AppWidthClass.large;

  bool get showPosSideCart => AppBreakpoints.isPosSideCart(widthClass);

  bool get showPosPersistentCart =>
      AppBreakpoints.isPosPersistentCart(widthClass);
}
