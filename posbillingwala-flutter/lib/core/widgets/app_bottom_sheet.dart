import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/app_button.dart';
import 'package:pos_billingwala_v2/core/widgets/widget_strings.dart';
import 'package:pos_billingwala_v2/core/widgets/widget_theme.dart';

/* Modal bottom sheet capped by screen width (full on phone, centered on tablet/web). */
Future<T?> showAppModalBottomSheet<T>({
  required BuildContext context,
  required WidgetBuilder builder,
  bool isScrollControlled = false,
  bool isDismissible = true,
  bool enableDrag = true,
  bool? showDragHandle,
  Color? backgroundColor,
  ShapeBorder? shape,
  Clip? clipBehavior,
  bool useSafeArea = false,
  bool useRootNavigator = false,
  RouteSettings? routeSettings,
  AnimationController? transitionAnimationController,
  Offset? anchorPoint,
  double? elevation,
  Color? barrierColor,
}) {
  return showModalBottomSheet<T>(
    context: context,
    isScrollControlled: isScrollControlled,
    isDismissible: isDismissible,
    enableDrag: enableDrag,
    showDragHandle: showDragHandle,
    backgroundColor: backgroundColor,
    shape: shape,
    clipBehavior: clipBehavior,
    useSafeArea: useSafeArea,
    useRootNavigator: useRootNavigator,
    routeSettings: routeSettings,
    transitionAnimationController: transitionAnimationController,
    anchorPoint: anchorPoint,
    elevation: elevation,
    barrierColor: barrierColor,
    constraints: AppBreakpoints.sheetConstraintsOf(context),
    builder: builder,
  );
}

Future<T?> showAppBottomSheet<T>({
  required BuildContext context,
  required String title,
  required Widget child,
  IconData? icon,
  bool isDismissible = true,
  bool enableDrag = true,
}) {
  return showAppModalBottomSheet<T>(
    context: context,
    isScrollControlled: true,
    isDismissible: isDismissible,
    enableDrag: enableDrag,
    backgroundColor: AppColors.glassSolid,
    showDragHandle: true,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
    ),
    builder: (ctx) => Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        16,
        24,
        MediaQuery.viewInsetsOf(ctx).bottom + 24,
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                if (icon != null) ...[
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: ctx.primary.withValues(alpha: .10),
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Icon(icon, color: ctx.primary),
                  ),
                  const SizedBox(width: 10),
                ],
                Expanded(
                  child: Text(
                    title,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: ctx.textPrimary,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            child,
          ],
        ),
      ),
    ),
  );
}

/* Confirmation sheet built on [showAppBottomSheet] with message + AppButton row. */
Future<bool> showAppConfirmBottomSheet({
  required BuildContext context,
  required String title,
  required String message,
  required String confirmLabel,
  String? cancelLabel,
  bool showCancel = true,
  IconData? icon,
  AppButtonVariant confirmVariant = AppButtonVariant.primary,
  bool barrierDismissible = true,
}) async {
  final result = await showAppBottomSheet<bool>(
    context: context,
    title: title,
    icon: icon,
    isDismissible: barrierDismissible,
    enableDrag: barrierDismissible,
    child: Builder(
      builder: (sheetContext) => Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            message,
            style: TextStyle(
              fontSize: 15,
              height: 1.45,
              color: sheetContext.textSecondary,
            ),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              if (showCancel) ...[
                Expanded(
                  child: AppButton(
                    label: cancelLabel ?? WidgetStrings.cancel,
                    variant: AppButtonVariant.outlined,
                    onPressed: () => Navigator.of(sheetContext).pop(false),
                  ),
                ),
                const SizedBox(width: 12),
              ],
              Expanded(
                child: AppButton(
                  label: confirmLabel,
                  variant: confirmVariant,
                  onPressed: () => Navigator.of(sheetContext).pop(true),
                ),
              ),
            ],
          ),
        ],
      ),
    ),
  );
  return result == true;
}

class AppSheetAction {
  const AppSheetAction({
    required this.value,
    required this.label,
    this.icon,
    this.destructive = false,
  });

  final String value;
  final String label;
  final IconData? icon;
  final bool destructive;
}

/* Toolbar overflow / filter menus as a bottom sheet (replaces PopupMenu). */
Future<String?> showAppActionSheet({
  required BuildContext context,
  required String title,
  required List<AppSheetAction> actions,
  IconData? icon,
}) {
  if (actions.isEmpty) return Future.value(null);
  return showAppBottomSheet<String>(
    context: context,
    title: title,
    icon: icon ?? Icons.more_horiz_rounded,
    child: Builder(
      builder: (sheetContext) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final action in actions)
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: action.icon == null
                  ? null
                  : Icon(
                      action.icon,
                      color: action.destructive
                          ? AppColors.danger
                          : AppColors.primary,
                    ),
              title: Text(
                action.label,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: action.destructive
                      ? AppColors.danger
                      : AppColors.navy,
                ),
              ),
              onTap: () => Navigator.of(sheetContext).pop(action.value),
            ),
        ],
      ),
    ),
  );
}
