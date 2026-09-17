/* Tracks the active UI route so API/DB logs can include a screen name. */
abstract final class ScreenContext {
  ScreenContext._();

  static String _screenName = 'App';
  static String _routePath = '/';

  static String get screenName => _screenName;
  static String get routePath => _routePath;

  static void update({String? name, String? path}) {
    final cleanedPath = (path ?? '').trim();
    if (cleanedPath.isNotEmpty) {
      _routePath = cleanedPath;
    }
    final cleanedName = (name ?? '').trim();
    if (cleanedName.isNotEmpty) {
      _screenName = _humanize(cleanedName);
      return;
    }
    if (cleanedPath.isNotEmpty) {
      _screenName = _nameFromPath(cleanedPath);
    }
  }

  static String _nameFromPath(String path) {
    final segments = path.split('/').where((s) => s.isNotEmpty).toList();
    if (segments.isEmpty) return 'Home';
    return _humanize(segments.last);
  }

  static String _humanize(String raw) {
    final cleaned = raw
        .replaceAll(RegExp(r'[^a-zA-Z0-9_\-]'), ' ')
        .replaceAll('_', ' ')
        .replaceAll('-', ' ')
        .trim();
    if (cleaned.isEmpty) return 'App';
    return cleaned
        .split(RegExp(r'\s+'))
        .map((w) {
          if (w.isEmpty) return w;
          if (w.toUpperCase() == w && w.length <= 4) return w;
          return '${w[0].toUpperCase()}${w.substring(1)}';
        })
        .join(' ');
  }
}
