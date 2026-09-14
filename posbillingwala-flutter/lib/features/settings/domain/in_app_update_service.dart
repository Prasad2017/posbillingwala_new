import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:in_app_update/in_app_update.dart';
import 'package:pos_billingwala_v2/core/constants/app_config.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:url_launcher/url_launcher.dart';

enum InAppUpdateStatus {
  notSupported,
  notAvailable,
  available,
  started,
  downloaded,
  denied,
  failed,
}

class InAppUpdateOutcome {
  const InAppUpdateOutcome(this.status);

  final InAppUpdateStatus status;
}

/* Google Play in-app updates (Android). iOS/web fall back to the store URL. */
class InAppUpdateService {
  InAppUpdateService();

  static bool get isAndroidPlay =>
      AppConfig.enableInAppUpdate && !kIsWeb && Platform.isAndroid;

  Future<AppUpdateInfo?> checkInfo() async {
    if (!isAndroidPlay) return null;
    try {
      return await InAppUpdate.checkForUpdate().timeout(
        const Duration(seconds: 8),
      );
    } catch (error) {
      debugPrint('InAppUpdate checkInfo: $error');
      return null;
    }
  }

  Future<InAppUpdateOutcome> checkAvailability() async {
    if (!isAndroidPlay) {
      return const InAppUpdateOutcome(InAppUpdateStatus.notSupported);
    }
    try {
      final info = await checkInfo();
      if (info == null) {
        return const InAppUpdateOutcome(InAppUpdateStatus.failed);
      }
      if (info.installStatus == InstallStatus.downloaded) {
        return const InAppUpdateOutcome(InAppUpdateStatus.downloaded);
      }
      if (info.updateAvailability == UpdateAvailability.updateAvailable ||
          info.updateAvailability ==
              UpdateAvailability.developerTriggeredUpdateInProgress) {
        return const InAppUpdateOutcome(InAppUpdateStatus.available);
      }
      return const InAppUpdateOutcome(InAppUpdateStatus.notAvailable);
    } catch (error) {
      debugPrint('InAppUpdate checkAvailability: $error');
      return const InAppUpdateOutcome(InAppUpdateStatus.failed);
    }
  }

  /* Immediate Play dialog when allowed; otherwise flexible download. */
  Future<InAppUpdateOutcome> startUpdate({bool preferImmediate = true}) async {
    if (!isAndroidPlay) {
      await openPlayStore();
      return const InAppUpdateOutcome(InAppUpdateStatus.notSupported);
    }
    try {
      final info = await checkInfo();
      if (info == null) {
        return const InAppUpdateOutcome(InAppUpdateStatus.failed);
      }
      if (info.installStatus == InstallStatus.downloaded) {
        await InAppUpdate.completeFlexibleUpdate();
        return const InAppUpdateOutcome(InAppUpdateStatus.downloaded);
      }
      final inProgress = info.updateAvailability ==
          UpdateAvailability.developerTriggeredUpdateInProgress;
      final available =
          info.updateAvailability == UpdateAvailability.updateAvailable ||
              inProgress;
      if (!available) {
        return const InAppUpdateOutcome(InAppUpdateStatus.notAvailable);
      }

      AppUpdateResult result;
      if (preferImmediate && (info.immediateUpdateAllowed || inProgress)) {
        result = await InAppUpdate.performImmediateUpdate();
      } else if (info.flexibleUpdateAllowed) {
        result = await InAppUpdate.startFlexibleUpdate();
      } else if (info.immediateUpdateAllowed) {
        result = await InAppUpdate.performImmediateUpdate();
      } else {
        await openPlayStore();
        return const InAppUpdateOutcome(InAppUpdateStatus.started);
      }

      switch (result) {
        case AppUpdateResult.success:
          return const InAppUpdateOutcome(InAppUpdateStatus.started);
        case AppUpdateResult.userDeniedUpdate:
          return const InAppUpdateOutcome(InAppUpdateStatus.denied);
        case AppUpdateResult.inAppUpdateFailed:
          await openPlayStore();
          return const InAppUpdateOutcome(InAppUpdateStatus.failed);
      }
    } catch (error) {
      debugPrint('InAppUpdate startUpdate: $error');
      await openPlayStore();
      return const InAppUpdateOutcome(InAppUpdateStatus.failed);
    }
  }

  Future<void> completeFlexibleUpdate() async {
    if (!isAndroidPlay) return;
    try {
      await InAppUpdate.completeFlexibleUpdate();
    } catch (error) {
      debugPrint('InAppUpdate completeFlexibleUpdate: $error');
    }
  }

  Future<bool> openPlayStore() async {
    return launchUrl(
      Uri.parse(AppConstants.playStoreUrl),
      mode: LaunchMode.externalApplication,
    );
  }
}

final inAppUpdateService = InAppUpdateService();
