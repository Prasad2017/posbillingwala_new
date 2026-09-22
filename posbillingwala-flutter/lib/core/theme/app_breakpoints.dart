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

/* Height classes — used with width for landscape / short viewports. */
enum AppHeightClass {
  /* Phone landscape / split-screen — typically &lt; 500 */
  short,

  /* Typical phone portrait / small tablet */
  regular,

  /* Tall tablets / desktop windows */
  tall,
}

/* Single source of truth for layout breakpoints — no magic numbers in pages. */
abstract final class AppBreakpoints {
  static const double compactMax = 600;
  static const double mediumMin = 600;
  static const double expandedMin = 1000;
  static const double largeMin = 1280;

  static const double shortHeightMax = 500;
  static const double tallHeightMin = 800;

  /* Default min item widths for dynamic column math (screens may override). */
  static const double minProductCardWidth = 148;
  static const double minTableCardWidth = 140;
  static const double minModuleTileWidth = 150;
  static const double minHubCardWidth = 260;
  static const double minMessMenuWidth = 160;
  static const double minComboCardWidth = 280;
  static const double minKpiCardWidth = 160;

  static AppWidthClass ofWidth(double width) {
    if (width >= largeMin) return AppWidthClass.large;
    if (width >= expandedMin) return AppWidthClass.expanded;
    if (width >= mediumMin) return AppWidthClass.medium;
    return AppWidthClass.compact;
  }

  static AppHeightClass ofHeight(double height) {
    if (height < shortHeightMax) return AppHeightClass.short;
    if (height >= tallHeightMin) return AppHeightClass.tall;
    return AppHeightClass.regular;
  }

  static AppWidthClass of(BuildContext context) =>
      ofWidth(MediaQuery.sizeOf(context).width);

  static AppHeightClass heightOf(BuildContext context) =>
      ofHeight(MediaQuery.sizeOf(context).height);

  static AppWidthClass ofConstraints(BoxConstraints constraints) =>
      ofWidth(constraints.maxWidth);

  /* Columns from available width + per-screen min item width. */
  static int columnsForWidth(
    double availableWidth, {
    required double minItemWidth,
    int minColumns = 1,
    int maxColumns = 8,
    double spacing = 0,
  }) {
    if (availableWidth <= 0 || minItemWidth <= 0) return minColumns;
    final raw = ((availableWidth + spacing) / (minItemWidth + spacing)).floor();
    return raw.clamp(minColumns, maxColumns);
  }

  /* POS / Tables / Takeaway: persistent side cart. */
  /* Web: always-on cart (side-by-side except compact, which stacks). */
  /* Native: side cart from expanded tablet, OR medium+short (phone landscape). */
  static bool isPosSideCart(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
  }) {
    if (AppPlatform.useDesktopShell) {
      return w != AppWidthClass.compact;
    }
    if (w.index >= AppWidthClass.expanded.index) return true;
    /* Phone landscape: use width for products + cart instead of stretching portrait. */
    if (w == AppWidthClass.medium && height == AppHeightClass.short) {
      return true;
    }
    return false;
  }

  static bool isPosPersistentCart(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
  }) {
    if (isPosSideCart(w, height: height)) return true;
    return AppPlatform.useDesktopShell ||
        w.index >= AppWidthClass.expanded.index;
  }

  static double posSideCartWidth(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
    double? availableWidth,
  }) {
    if (availableWidth != null && availableWidth.isFinite) {
      /* Keep cart usable but never steal more than ~42% on narrow landscape. */
      final capped = (availableWidth * 0.42).clamp(260.0, 420.0);
      if (height == AppHeightClass.short &&
          w.index <= AppWidthClass.medium.index) {
        return capped.toDouble();
      }
    }
    if (AppPlatform.useDesktopShell) {
      return switch (w) {
        AppWidthClass.compact => 280,
        AppWidthClass.medium => 320,
        AppWidthClass.expanded => 360,
        AppWidthClass.large => 400,
      };
    }
    if (height == AppHeightClass.short && w == AppWidthClass.medium) {
      return 280;
    }
    return w == AppWidthClass.large ? 400 : 360;
  }

  /* Compact vertical rhythm when height is limited (landscape). */
  static double densePaddingFor(AppHeightClass h) => switch (h) {
    AppHeightClass.short => 6,
    AppHeightClass.regular => 10,
    AppHeightClass.tall => 12,
  };

  static double productCardExtentFor(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
  }) {
    if (height == AppHeightClass.short) return 72;
    return switch (w) {
      AppWidthClass.compact => 86,
      AppWidthClass.medium => 88,
      AppWidthClass.expanded || AppWidthClass.large => 90,
    };
  }

  /* Home / hub module grids: 2 → 3 → 4 (prefer [columnsForWidth] when possible). */
  static int moduleColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 2,
    AppWidthClass.medium => 3,
    AppWidthClass.expanded => 4,
    AppWidthClass.large => 4,
  };

  /* POS product catalog: class fallback; prefer width-based columns. */
  static int productColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 2,
    AppWidthClass.medium => 3,
    AppWidthClass.expanded => 4,
    AppWidthClass.large => 5,
  };

  static int productColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minProductCardWidth,
    minColumns: 2,
    maxColumns: 6,
    spacing: 6,
  );

  /* Tables floor grid. */
  static int tableColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 2,
    AppWidthClass.medium => 3,
    AppWidthClass.expanded => 4,
    AppWidthClass.large => 5,
  };

  static int tableColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minTableCardWidth,
    minColumns: 2,
    maxColumns: 6,
    spacing: 12,
  );

  /* Reports / masters card grids: 1 → 2 → 3. */
  static int cardColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.compact => 1,
    AppWidthClass.medium => 2,
    AppWidthClass.expanded => 2,
    AppWidthClass.large => 3,
  };

  static int cardColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minHubCardWidth,
    minColumns: 1,
    maxColumns: 4,
    spacing: 12,
  );

  /* Mess hub menu tiles. */
  static int messMenuColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minMessMenuWidth,
    minColumns: 2,
    maxColumns: 4,
    spacing: 8,
  );

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

  /* Form field columns: 1 on phone, 2 on tablet+, optional 3 on large. */
  static int formColumnsFor(AppWidthClass w, {int maxColumns = 2}) {
    final cols = switch (w) {
      AppWidthClass.compact => 1,
      AppWidthClass.medium => 2,
      AppWidthClass.expanded => 2,
      AppWidthClass.large => maxColumns.clamp(2, 3),
    };
    return cols.clamp(1, maxColumns);
  }
}

extension AppBreakpointsContext on BuildContext {
  AppWidthClass get widthClass => AppBreakpoints.of(this);

  AppHeightClass get heightClass => AppBreakpoints.heightOf(this);

  bool get isCompactWidth => widthClass == AppWidthClass.compact;

  bool get isMediumWidth => widthClass == AppWidthClass.medium;

  bool get isExpandedWidth => widthClass.index >= AppWidthClass.expanded.index;

  bool get isLargeWidth => widthClass == AppWidthClass.large;

  bool get isShortHeight => heightClass == AppHeightClass.short;

  bool get isLandscapeLayout =>
      MediaQuery.sizeOf(this).width > MediaQuery.sizeOf(this).height;

  bool get showPosSideCart =>
      AppBreakpoints.isPosSideCart(widthClass, height: heightClass);

  bool get showPosPersistentCart =>
      AppBreakpoints.isPosPersistentCart(widthClass, height: heightClass);
}
