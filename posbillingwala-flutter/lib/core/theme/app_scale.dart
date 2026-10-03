import 'package:flutter/widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_dimensions.dart';

/*
 * Responsive UI scale for POS / tablet screens.
 *
 * Fonts and touch targets grow with width class so 10.1" devices stay
 * readable without hard-coding phone sizes. Short (landscape phone)
 * viewports slightly densify to keep the billing screen fitting.
 */
abstract final class AppScale {
  AppScale._();

  /* MediaQuery text scaler — replaces the old fixed 0.9 lock. */
  static double textScalerForWidth(double width) {
    final w = AppBreakpoints.ofWidth(width);
    return switch (w) {
      AppWidthClass.smallMobile => 0.95,
      AppWidthClass.mobile => 1.0,
      AppWidthClass.tablet => 1.06,
      AppWidthClass.largeTablet => 1.12,
      AppWidthClass.desktop => 1.14,
      AppWidthClass.largeDesktop => 1.16,
    };
  }

  static double of(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    var scale = textScalerForWidth(size.width);
    if (AppBreakpoints.ofHeight(size.height) == AppHeightClass.short) {
      scale *= 0.92;
    }
    return scale;
  }

  /* Scaled font size (logical px). */
  static double sp(BuildContext context, double size) => size * of(context);

  /* Minimum tap target — always ≥ Material 48dp guideline. */
  static double tap(BuildContext context, {double base = AppDimensions.minTapTarget}) {
    return (base * of(context)).clamp(AppDimensions.minTapTarget, 64.0);
  }

  /* Primary action button height (PAY / SAVE / PRINT). */
  static double buttonHeight(BuildContext context) {
    return (AppDimensions.buttonHeight * of(context)).clamp(52.0, 64.0);
  }

  /* Qty +/− control size on cart / payment. */
  static double qtyButton(BuildContext context) {
    return (44.0 * of(context)).clamp(44.0, 56.0);
  }

  /* Catalog product "Add" bar height. */
  static double addButtonHeight(BuildContext context) {
    return (40.0 * of(context)).clamp(40.0, 52.0);
  }

  /* Edge padding that grows slightly on larger tablets. */
  static double pagePad(BuildContext context) {
    return AppBreakpoints.pagePaddingFor(AppBreakpoints.of(context));
  }
}

extension AppScaleContext on BuildContext {
  double get uiScale => AppScale.of(this);

  double sp(double size) => AppScale.sp(this, size);
}
