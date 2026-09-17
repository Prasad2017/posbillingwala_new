import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/logging/screen_context.dart';

/* Keeps [ScreenContext] in sync with Navigator / GoRouter pushes. */
class ScreenRouteObserver extends NavigatorObserver {
  @override
  void didPush(Route<dynamic> route, Route<dynamic>? previousRoute) {
    _capture(route);
  }

  @override
  void didReplace({Route<dynamic>? newRoute, Route<dynamic>? oldRoute}) {
    if (newRoute != null) _capture(newRoute);
  }

  @override
  void didPop(Route<dynamic> route, Route<dynamic>? previousRoute) {
    if (previousRoute != null) _capture(previousRoute);
  }

  void _capture(Route<dynamic> route) {
    final settings = route.settings;
    final name = settings.name;
    final args = settings.arguments;
    String? path;
    if (args is Map && args['path'] is String) {
      path = args['path'] as String;
    }
    ScreenContext.update(name: name, path: path ?? name);
  }
}
