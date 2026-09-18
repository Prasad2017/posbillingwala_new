import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/sync/domain/full_sync_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_progress.dart';

/* Android-parity progress screen for: */
/* - Offline Data Synchronize with Cloud (`mode=sync`) — mobile only */
/* - Fetch / Refresh Data From Cloud (`mode=fetch`) */
class SyncPage extends ConsumerStatefulWidget {
  const SyncPage({super.key, this.initialMode});

  /* `fetch` = wipe & download; `sync` / null = upload pending. */
  final String? initialMode;

  @override
  ConsumerState<SyncPage> createState() => SyncPageState();
}

class SyncPageState extends ConsumerState<SyncPage> {
  bool started = false;

  SyncScreenMode get syncPageMode {
    final raw = widget.initialMode?.trim().toLowerCase();
    if (raw == 'fetch') return SyncScreenMode.fetch;
    /* Web is online-only — never run offline upload mode. */
    if (!AppPlatform.supportsOfflineSync) return SyncScreenMode.fetch;
    return SyncScreenMode.upload;
  }

  String get syncPageTitle => syncPageMode == SyncScreenMode.fetch
      ? (AppPlatform.requiresNetwork
            ? 'Refresh Data From Cloud'
            : 'Fetch Data From Cloud')
      : 'Offline Data Synchronize with Cloud';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => syncPageStart());
  }

  Future<void> syncPageStart() async {
    if (started) return;
    started = true;

    if (!await ensureOnline()) {
      ref
          .read(syncProgressProvider.notifier)
          .setBlocked(
            headline: 'Internet required',
            subtitle: kOnlineRequiredMessage,
          );
      return;
    }

    final full = ref.read(fullSyncControllerProvider.notifier);
    if (syncPageMode == SyncScreenMode.fetch) {
      final result = await full.resetAndFetchWithProgress();
      if (!mounted) return;
      if (result.localCounts != null) {
        context.pushReplacement('/sync/fetch-result');
      }
    } else {
      await full.uploadWithProgress();
    }
  }

  @override
  Widget build(BuildContext context) {
    final progress = ref.watch(syncProgressProvider);
    final syncResult = ref.watch(fullSyncControllerProvider).asData?.value;
    final canLeave = !progress.isRunning;
    final showSavedData =
        syncPageMode == SyncScreenMode.fetch &&
        syncResult?.localCounts != null &&
        canLeave;

    return PopScope(
      canPop: canLeave,
      child: Scaffold(
        backgroundColor: const Color(0xFFF3F7FC),
        appBar: AppBar(
          title: Text(
            syncPageTitle,
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontWeight: FontWeight.w700,
              fontSize: 17,
            ),
          ),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: canLeave ? () => context.pop() : null,
          ),
        ),
        body: Column(
          children: [
            Expanded(
              child: ResponsiveScrollShell(
                dashboard: true,
                child: ListView(
                  padding: EdgeInsets.fromLTRB(
                    AppBreakpoints.pagePaddingFor(context.widthClass),
                    16,
                    AppBreakpoints.pagePaddingFor(context.widthClass),
                    12,
                  ),
                  children: [
                    StatusCard(
                      headline:
                          progress.headline ??
                          (syncPageMode == SyncScreenMode.upload
                              ? 'Preparing upload…'
                              : 'Preparing fetch…'),
                      subtitle: progress.subtitle ?? '',
                      isRunning: progress.isRunning,
                      failed: progress.failed,
                    ),
                    const SizedBox(height: 18),
                    const Text(
                      'TABLES',
                      style: TextStyle(
                        fontFamily: AppFonts.family,
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w700,
                        fontSize: 12,
                        letterSpacing: 0.8,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Container(
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(18),
                        border: Border.all(
                          color: AppColors.border.withValues(alpha: .7),
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: AppColors.navy.withValues(alpha: .05),
                            blurRadius: 14,
                            offset: const Offset(0, 6),
                          ),
                        ],
                      ),
                      child: Column(
                        children: [
                          for (var i = 0; i < progress.steps.length; i++) ...[
                            if (i > 0)
                              Divider(
                                height: 1,
                                thickness: 1,
                                color: AppColors.border.withValues(alpha: .65),
                                indent: 56,
                              ),
                            TableStatusRow(step: progress.steps[i]),
                          ],
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                child: AppButton(
                  label: showSavedData ? 'View saved data' : 'Done',
                  onPressed: canLeave
                      ? () {
                          if (showSavedData) {
                            context.pushReplacement('/sync/fetch-result');
                          } else {
                            context.pop();
                          }
                        }
                      : null,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class StatusCard extends StatelessWidget {
  const StatusCard({
    super.key,
    required this.headline,
    required this.subtitle,
    required this.isRunning,
    required this.failed,
  });

  final String headline;
  final String subtitle;
  final bool isRunning;
  final int failed;

  @override
  Widget build(BuildContext context) {
    final accent = failed > 0
        ? AppColors.red
        : isRunning
        ? AppColors.primary
        : AppColors.green;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border.withValues(alpha: .7)),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: .05),
            blurRadius: 14,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: accent.withValues(alpha: .12),
              shape: BoxShape.circle,
            ),
            child: isRunning
                ? const Padding(
                    padding: EdgeInsets.all(11),
                    child: CircularProgressIndicator(strokeWidth: 2.4),
                  )
                : Icon(
                    failed > 0
                        ? Icons.error_outline_rounded
                        : Icons.cloud_done_rounded,
                    color: accent,
                  ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  headline,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.navy,
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                    height: 1.25,
                  ),
                ),
                if (subtitle.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.navy.withValues(alpha: .55),
                      fontWeight: FontWeight.w400,
                      fontSize: 13,
                      height: 1.35,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class TableStatusRow extends StatelessWidget {
  const TableStatusRow({super.key, required this.step});

  final SyncTableStep step;

  @override
  Widget build(BuildContext context) {
    final (icon, color, statusLabel) = switch (step.status) {
      SyncTableStatus.complete => (
        Icons.check_rounded,
        AppColors.green,
        'Complete',
      ),
      SyncTableStatus.running => (
        Icons.sync_rounded,
        AppColors.primary,
        'Syncing',
      ),
      SyncTableStatus.error => (Icons.close_rounded, AppColors.red, 'Failed'),
      SyncTableStatus.skipped => (
        Icons.remove_rounded,
        AppColors.textSecondary,
        'Skipped',
      ),
      SyncTableStatus.pending => (
        Icons.radio_button_unchecked_rounded,
        AppColors.textSecondary,
        'Pending',
      ),
    };

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: [
          Container(
            width: 30,
            height: 30,
            decoration: BoxDecoration(
              color: color.withValues(alpha: .14),
              shape: BoxShape.circle,
            ),
            child: step.status == SyncTableStatus.running
                ? const Padding(
                    padding: EdgeInsets.all(7),
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Icon(icon, size: 18, color: color),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              step.label,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.navy,
                fontWeight: FontWeight.w600,
                fontSize: 14.5,
              ),
            ),
          ),
          Text(
            statusLabel,
            style: TextStyle(
              fontFamily: AppFonts.family,
              color: color,
              fontWeight: FontWeight.w700,
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }
}
