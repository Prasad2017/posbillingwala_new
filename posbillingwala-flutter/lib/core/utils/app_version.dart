import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';

/* App version + build from pubspec.yaml (`version: x.y.z+build`). */
class AppVersionInfo {
  const AppVersionInfo({
    required this.version,
    required this.buildNumber,
  });

  final String version;
  final String buildNumber;

  /* Settings / About subtitle: Version 1.0.1 (76) */
  String get label => 'Version $version ($buildNumber)';

  /* Badge: V 1.0.1+76 */
  String get badge => 'V $version+$buildNumber';

  /* Compact: 1.0.1+76 */
  String get short => '$version+$buildNumber';

  static const fallback = AppVersionInfo(version: '—', buildNumber: '—');
}

final appVersionProvider = FutureProvider<AppVersionInfo>((ref) async {
  final info = await PackageInfo.fromPlatform();
  final version = info.version.trim().isEmpty ? '—' : info.version.trim();
  final build =
      info.buildNumber.trim().isEmpty ? '—' : info.buildNumber.trim();
  return AppVersionInfo(version: version, buildNumber: build);
});

AppVersionInfo appVersionOf(WidgetRef ref) =>
    ref.watch(appVersionProvider).asData?.value ?? AppVersionInfo.fallback;
