import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:in_app_update/in_app_update.dart';
import 'package:pos_billingwala_v2/features/settings/domain/in_app_update_service.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Listens for a finished flexible Play update and offers Restart. */
class InAppUpdateHost extends ConsumerStatefulWidget {
  const InAppUpdateHost({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<InAppUpdateHost> createState() => InAppUpdateHostState();
}

class InAppUpdateHostState extends ConsumerState<InAppUpdateHost>
    with WidgetsBindingObserver {
  StreamSubscription<InstallStatus>? installSub;
  bool promptedRestart = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (InAppUpdateService.isAndroidPlay) {
      installSub = InAppUpdate.installUpdateListener.listen(onInstallStatus);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        promptIfDownloaded();
      });
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    installSub?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      promptIfDownloaded();
    }
  }

  void onInstallStatus(InstallStatus status) {
    if (status == InstallStatus.downloaded) {
      showRestartSnack();
    }
  }

  Future<void> promptIfDownloaded() async {
    final info = await inAppUpdateService.checkInfo();
    if (!mounted) return;
    if (info?.installStatus == InstallStatus.downloaded) {
      showRestartSnack();
    }
  }

  void showRestartSnack() {
    if (!mounted || promptedRestart) return;
    promptedRestart = true;
    final strings = AppStrings.of(ref);
    final messenger = ScaffoldMessenger.maybeOf(context);
    messenger?.showSnackBar(
      SnackBar(
        duration: const Duration(days: 1),
        content: Text(strings.updateDownloaded),
        action: SnackBarAction(
          label: strings.restart,
          onPressed: () {
            inAppUpdateService.completeFlexibleUpdate();
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
