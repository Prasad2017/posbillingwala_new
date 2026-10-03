import 'package:flutter/widgets.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';

/*
 * Responsive width classes (logical px / dp):
 *
 * | Device / Layout     | Width        |
 * |---------------------|-------------:|
 * | Small Mobile        |       < 360  |
 * | Mobile              |   360 – 599  |
 * | Tablet              |   600 – 899  |
 * | Large Tablet        |  900 – 1199  |
 * | Desktop             | 1200 – 1599  |
 * | Large Desktop / Web |     ≥ 1600  |
 *
 * Landscape is an additional axis on Mobile / Tablet / Large Tablet.
 */
enum AppWidthClass {
  /* < 360 — small Android phones */
  smallMobile,

  /* 360–599 — phone */
  mobile,

  /* 600–899 — 7–8" tablet */
  tablet,

  /* 900–1199 — 10–12" tablet */
  largeTablet,

  /* 1200–1599 — laptop / desktop */
  desktop,

  /* ≥ 1600 — large monitor / web */
  largeDesktop,
}

/* Height band — used with width for landscape / short viewports. */
enum AppHeightClass {
  /* Phone landscape / split-screen — typically < 500 */
  short,

  /* Typical phone portrait / small tablet */
  regular,

  /* Tall tablets / desktop windows */
  tall,
}

/* Orientation axis from the responsive tree (Mobile/Tablet/Large Tablet). */
enum AppOrientationClass {
  portrait,
  landscape,
}

/* Single source of truth for layout breakpoints. */
abstract final class AppBreakpoints {
  /* Width thresholds from the product responsive table. */
  static const double mobileMin = 360;
  static const double tabletMin = 600;
  static const double largeTabletMin = 900;
  static const double desktopMin = 1200;
  static const double largeDesktopMin = 1600;

  /* Side cart: tablet+ or mobile landscape with enough width. */
  static const double sideCartMin = 600;

  /* Two-column forms / splits start at tablet (or mobile landscape wide enough). */
  static const double wideWindowMin = 600;

  static const double shortHeightMax = 500;
  static const double tallHeightMin = 800;

  /* Wider cards so full product names wrap cleanly on POS tablets. */
  static const double minProductCardWidth = 168;
  static const double minTableCardWidth = 140;
  static const double minModuleTileWidth = 150;
  static const double minHubCardWidth = 260;
  static const double minMessMenuWidth = 160;
  static const double minComboCardWidth = 280;
  static const double minKpiCardWidth = 160;

  static const int gridMinColumns = 2;
  static const int gridMaxColumns = 6;

  static AppWidthClass ofWidth(double width) {
    if (width >= largeDesktopMin) return AppWidthClass.largeDesktop;
    if (width >= desktopMin) return AppWidthClass.desktop;
    if (width >= largeTabletMin) return AppWidthClass.largeTablet;
    if (width >= tabletMin) return AppWidthClass.tablet;
    if (width >= mobileMin) return AppWidthClass.mobile;
    return AppWidthClass.smallMobile;
  }

  static AppHeightClass ofHeight(double height) {
    if (height < shortHeightMax) return AppHeightClass.short;
    if (height >= tallHeightMin) return AppHeightClass.tall;
    return AppHeightClass.regular;
  }

  static AppOrientationClass orientationOf(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    return size.width > size.height
        ? AppOrientationClass.landscape
        : AppOrientationClass.portrait;
  }

  static AppOrientationClass orientationOfSize(Size size) =>
      size.width > size.height
      ? AppOrientationClass.landscape
      : AppOrientationClass.portrait;

  static AppWidthClass of(BuildContext context) =>
      ofWidth(MediaQuery.sizeOf(context).width);

  static AppHeightClass heightOf(BuildContext context) =>
      ofHeight(MediaQuery.sizeOf(context).height);

  /* True for smallMobile or mobile (phone family). */
  static bool isMobileClass(AppWidthClass w) =>
      w == AppWidthClass.smallMobile || w == AppWidthClass.mobile;

  static bool isWideLayoutWidth(
    double width, {
    double? height,
    AppOrientationClass? orientation,
  }) {
    final w = ofWidth(width);
    if (!isMobileClass(w)) return true;
    final land =
        orientation == AppOrientationClass.landscape ||
        (height != null && width > height);
    return land && width >= wideWindowMin;
  }

  static bool useSideCartPanel(double availableWidth) =>
      availableWidth >= sideCartMin;

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

  /*
   * POS side cart:
   * - Tablet / Large Tablet / Desktop / Web: always
   * - Mobile landscape: when available width ≥ sideCartMin
   * - Mobile portrait: stacked / footer
   */
  static bool isPosSideCart(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
    AppOrientationClass orientation = AppOrientationClass.portrait,
    double? availableWidth,
  }) {
    if (availableWidth != null && availableWidth.isFinite) {
      final classFromWidth = ofWidth(availableWidth);
      if (!isMobileClass(classFromWidth)) {
        return useSideCartPanel(availableWidth);
      }
      final land =
          orientation == AppOrientationClass.landscape ||
          height == AppHeightClass.short;
      return land && useSideCartPanel(availableWidth);
    }
    if (!isMobileClass(w)) return true;
    return orientation == AppOrientationClass.landscape ||
        height == AppHeightClass.short;
  }

  static bool isPosPersistentCart(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
    AppOrientationClass orientation = AppOrientationClass.portrait,
    double? availableWidth,
  }) {
    if (isPosSideCart(
      w,
      height: height,
      orientation: orientation,
      availableWidth: availableWidth,
    )) {
      return true;
    }
    /* Web mobile: stacked catalog + cart instead of phone footer-only. */
    return AppPlatform.useDesktopShell;
  }

  static double posSideCartWidth(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
    double? availableWidth,
  }) {
    if (availableWidth != null && availableWidth.isFinite) {
      final capped = (availableWidth * 0.40).clamp(260.0, 480.0);
      return capped.toDouble();
    }
    return switch (w) {
      AppWidthClass.smallMobile || AppWidthClass.mobile => 280,
      AppWidthClass.tablet => 320,
      AppWidthClass.largeTablet => 360,
      AppWidthClass.desktop => 400,
      AppWidthClass.largeDesktop => 440,
    };
  }

  static double densePaddingFor(AppHeightClass h) => switch (h) {
    AppHeightClass.short => 6,
    AppHeightClass.regular => 10,
    AppHeightClass.tall => 12,
  };

  static double productCardExtentFor(
    AppWidthClass w, {
    AppHeightClass height = AppHeightClass.regular,
    bool includeImage = true,
  }) {
    /* Name (up to 4 lines) + meta + price + Add — optional image above. */
    final textBlock = height == AppHeightClass.short ? 140.0 : switch (w) {
      AppWidthClass.smallMobile || AppWidthClass.mobile => 152.0,
      AppWidthClass.tablet => 158.0,
      AppWidthClass.largeTablet ||
      AppWidthClass.desktop ||
      AppWidthClass.largeDesktop =>
        164.0,
    };
    if (!includeImage) return textBlock;
    return textBlock + (height == AppHeightClass.short ? 74 : 76);
  }

  static int productColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minProductCardWidth,
    minColumns: gridMinColumns,
    maxColumns: gridMaxColumns,
    spacing: 6,
  );

  static int tableColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minTableCardWidth,
    minColumns: gridMinColumns,
    maxColumns: gridMaxColumns,
    spacing: 12,
  );

  static int cardColumnsFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile || AppWidthClass.mobile => 1,
    AppWidthClass.tablet => 2,
    AppWidthClass.largeTablet => 2,
    AppWidthClass.desktop => 3,
    AppWidthClass.largeDesktop => 3,
  };

  static int cardColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minHubCardWidth,
    minColumns: 1,
    maxColumns: 4,
    spacing: 12,
  );

  static int messMenuColumnsForWidth(double availableWidth) => columnsForWidth(
    availableWidth,
    minItemWidth: minMessMenuWidth,
    minColumns: 2,
    maxColumns: 4,
    spacing: 8,
  );

  static int settingsColumnsFor(AppWidthClass w) =>
      w.index >= AppWidthClass.tablet.index ? 2 : 1;

  static double contentMaxWidthFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile || AppWidthClass.mobile => 560,
    AppWidthClass.tablet => 720,
    AppWidthClass.largeTablet => 900,
    AppWidthClass.desktop => 1000,
    AppWidthClass.largeDesktop => 1100,
  };

  static double dashboardMaxWidthFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile || AppWidthClass.mobile => 560,
    AppWidthClass.tablet => 900,
    AppWidthClass.largeTablet => 1100,
    AppWidthClass.desktop => 1400,
    AppWidthClass.largeDesktop => 1600,
  };

  static double pagePaddingFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile => 12,
    AppWidthClass.mobile => 16,
    AppWidthClass.tablet => 20,
    AppWidthClass.largeTablet => 24,
    AppWidthClass.desktop => 28,
    AppWidthClass.largeDesktop => 32,
  };

  /* Compact dialogs (confirm / pickers) — never stretch on tablet/web. */
  static double dialogMaxWidthFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile || AppWidthClass.mobile => 400,
    AppWidthClass.tablet => 440,
    AppWidthClass.largeTablet => 480,
    AppWidthClass.desktop || AppWidthClass.largeDesktop => 520,
  };

  /* Multi-field form dialogs — wider, still capped. */
  static double dialogFormMaxWidthFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile || AppWidthClass.mobile => 420,
    AppWidthClass.tablet => 560,
    AppWidthClass.largeTablet => 640,
    AppWidthClass.desktop || AppWidthClass.largeDesktop => 720,
  };

  /* Bottom sheets: full bleed on phones; centered & capped on tablet/web. */
  static double sheetMaxWidthFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile || AppWidthClass.mobile => double.infinity,
    AppWidthClass.tablet => 480,
    AppWidthClass.largeTablet => 520,
    AppWidthClass.desktop => 560,
    AppWidthClass.largeDesktop => 600,
  };

  static double dialogHorizontalInsetFor(AppWidthClass w) => switch (w) {
    AppWidthClass.smallMobile => 16,
    AppWidthClass.mobile => 24,
    AppWidthClass.tablet => 40,
    AppWidthClass.largeTablet => 48,
    AppWidthClass.desktop || AppWidthClass.largeDesktop => 56,
  };

  static BoxConstraints dialogConstraintsOf(
    BuildContext context, {
    bool form = false,
  }) {
    final size = MediaQuery.sizeOf(context);
    final w = ofWidth(size.width);
    final preferred =
        form ? dialogFormMaxWidthFor(w) : dialogMaxWidthFor(w);
    final inset = dialogHorizontalInsetFor(w);
    final max = preferred < size.width - inset
        ? preferred
        : (size.width - inset).clamp(280.0, preferred);
    return BoxConstraints(minWidth: 280, maxWidth: max);
  }

  static BoxConstraints sheetConstraintsOf(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    final w = ofWidth(size.width);
    final preferred = sheetMaxWidthFor(w);
    if (!preferred.isFinite) {
      return const BoxConstraints();
    }
    return BoxConstraints(maxWidth: preferred.clamp(280.0, size.width));
  }

  static EdgeInsets dialogInsetPaddingOf(BuildContext context) {
    final w = of(context);
    return EdgeInsets.symmetric(
      horizontal: dialogHorizontalInsetFor(w),
      vertical: 24,
    );
  }

  static int formColumnsFor(
    AppWidthClass w, {
    int maxColumns = 2,
    double? availableWidth,
    double? availableHeight,
  }) {
    final wide = availableWidth != null
        ? isWideLayoutWidth(availableWidth, height: availableHeight)
        : !isMobileClass(w);
    if (!wide) return 1;
    final cols = w.index >= AppWidthClass.desktop.index
        ? maxColumns.clamp(2, 3)
        : 2;
    return cols.clamp(1, maxColumns);
  }

  /* Master form/list split weights. */
  static const int masterFormFlex = 38;
  static const int masterListFlex = 62;

  static const int aboutPrimaryFlex = 42;
  static const int aboutSecondaryFlex = 58;

  static const int posCatalogFlex = 60;
  static const int posCartFlex = 40;
}

extension AppBreakpointsContext on BuildContext {
  AppWidthClass get widthClass => AppBreakpoints.of(this);

  AppHeightClass get heightClass => AppBreakpoints.heightOf(this);

  AppOrientationClass get orientationClass =>
      AppBreakpoints.orientationOf(this);

  /* Phone family (smallMobile + mobile). */
  bool get isCompactWidth => AppBreakpoints.isMobileClass(widthClass);

  bool get isMobileWidth => AppBreakpoints.isMobileClass(widthClass);

  bool get isShortHeight => heightClass == AppHeightClass.short;

  bool get isLandscapeLayout =>
      orientationClass == AppOrientationClass.landscape;
}
