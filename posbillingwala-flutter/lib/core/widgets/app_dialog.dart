import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';

/* Applies responsive dialog width / inset from [AppBreakpoints]. */
Widget appDialogShell(
  BuildContext context,
  Widget child, {
  bool form = false,
}) {
  final base = Theme.of(context);
  return Theme(
    data: base.copyWith(
      dialogTheme: base.dialogTheme.copyWith(
        constraints: AppBreakpoints.dialogConstraintsOf(context, form: form),
        insetPadding: AppBreakpoints.dialogInsetPaddingOf(context),
      ),
    ),
    child: child,
  );
}

/* showDialog with screen-width–aware max width (tablet/web capped). */
Future<T?> showAppDialog<T>({
  required BuildContext context,
  required WidgetBuilder builder,
  bool form = false,
  bool barrierDismissible = true,
  Color? barrierColor,
  bool useRootNavigator = true,
  RouteSettings? routeSettings,
  Offset? anchorPoint,
  TraversalEdgeBehavior? traversalEdgeBehavior,
}) {
  return showDialog<T>(
    context: context,
    barrierDismissible: barrierDismissible,
    barrierColor: barrierColor,
    useRootNavigator: useRootNavigator,
    routeSettings: routeSettings,
    anchorPoint: anchorPoint,
    traversalEdgeBehavior: traversalEdgeBehavior,
    builder: (ctx) => appDialogShell(ctx, builder(ctx), form: form),
  );
}
