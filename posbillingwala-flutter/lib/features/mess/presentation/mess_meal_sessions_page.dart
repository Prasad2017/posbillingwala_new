import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_offline_queue.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class MessMealSessionsPage extends ConsumerStatefulWidget {
  const MessMealSessionsPage({super.key});

  @override
  ConsumerState<MessMealSessionsPage> createState() =>
      MessMealSessionsPageState();
}

Color sessionColor(int index) => [
  AppColors.orange,
  AppColors.purple,
  AppColors.teal,
  AppColors.primary,
][index % 4];

class MessMealSessionsPageState extends ConsumerState<MessMealSessionsPage> {
  AsyncValue<List<MessMealSessionDto>> messMealSessionsPageSessions =
      const AsyncLoading();
  bool saving = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      setState(
        () => messMealSessionsPageSessions = AsyncError(
          'Login required',
          StackTrace.current,
        ),
      );
      return;
    }
    final cached = await CloudScreenCache.loadMapList(
      CloudScreenCache.mealSessions,
    );
    if (cached.isNotEmpty && mounted) {
      setState(
        () => messMealSessionsPageSessions = AsyncData(
          cached.map(MessMealSessionDto.fromJson).toList(),
        ),
      );
    } else if (mounted) {
      setState(() => messMealSessionsPageSessions = const AsyncLoading());
    }
    if (!await isDeviceOnline()) return;
    if (await StaffOfflineQueue.isMealSessionsPending()) return;
    final result = await AsyncValue.guard(() async {
      return MessApi(ref.read(apiClientProvider)).fetchMealSessions(userId);
    });
    if (!mounted) return;
    /* Keep cache if cloud fetch fails. */
    if (result.hasError && messMealSessionsPageSessions.hasValue) return;
    if (result.hasValue) {
      await CloudScreenCache.saveJson(
        CloudScreenCache.mealSessions,
        result.value!.map((e) => e.toJson()).toList(),
      );
    }
    setState(() => messMealSessionsPageSessions = result);
  }

  Future<void> edit(MessMealSessionDto session) async {
    final nameCtrl = TextEditingController(text: session.sessionName);
    final prefixCtrl = TextEditingController(text: session.tokenPrefix);
    var start = session.startTime;
    var end = session.endTime;
    var active = session.active;

    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setLocal) => AlertDialog(
          title: Text(
            session.sessionId.isEmpty ? 'Add session' : 'Edit session',
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AppTextField(
                  required: true,
                  controller: nameCtrl,
                  label: 'Session name',
                ),
                const SizedBox(height: 12),
                AppTextField(controller: prefixCtrl, label: 'Token prefix'),
                const SizedBox(height: 12),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text('Start: $start'),
                  trailing: const Icon(Icons.schedule),
                  onTap: () async {
                    final parts = start.split(':');
                    final picked = await showTimePicker(
                      context: context,
                      initialTime: TimeOfDay(
                        hour: int.tryParse(parts.first) ?? 8,
                        minute: parts.length > 1
                            ? int.tryParse(parts[1]) ?? 0
                            : 0,
                      ),
                    );
                    if (picked != null) {
                      setLocal(() {
                        start =
                            '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
                      });
                    }
                  },
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text('End: $end'),
                  trailing: const Icon(Icons.schedule),
                  onTap: () async {
                    final parts = end.split(':');
                    final picked = await showTimePicker(
                      context: context,
                      initialTime: TimeOfDay(
                        hour: int.tryParse(parts.first) ?? 10,
                        minute: parts.length > 1
                            ? int.tryParse(parts[1]) ?? 0
                            : 0,
                      ),
                    );
                    if (picked != null) {
                      setLocal(() {
                        end =
                            '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
                      });
                    }
                  },
                ),
                AppSwitchTile(
                  title: AppStrings.of(ref).active,
                  value: active,
                  showDivider: false,
                  onChanged: (v) => setLocal(() => active = v),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(AppStrings.of(ref).cancel),
            ),
            AppButton(
              label: 'Save',
              onPressed: () => Navigator.pop(context, true),
            ),
          ],
        ),
      ),
    );

    if (ok != true || !mounted) return;
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null) return;
    setState(() => saving = true);
    try {
      final sessionName = nameCtrl.text.trim();
      final tokenPrefix = prefixCtrl.text.trim();
      final isActive = active ? '1' : '0';
      var sessionId = session.sessionId;
      if (sessionId.isEmpty || int.tryParse(sessionId) == null) {
        if (sessionId.isEmpty) {
          sessionId = 'sess_${DateTime.now().millisecondsSinceEpoch}';
        }
      }

      final updated = session.copyWith(
        sessionId: sessionId,
        sessionName: sessionName,
        startTime: start,
        endTime: end,
        tokenPrefix: tokenPrefix,
        isActive: isActive,
      );

      final current =
          messMealSessionsPageSessions.asData?.value.toList() ??
          <MessMealSessionDto>[];
      final idx = current.indexWhere((e) => e.sessionId == session.sessionId);
      if (idx >= 0) {
        current[idx] = updated;
      } else if (session.sessionId.isEmpty) {
        current.add(updated);
      } else {
        final byId = current.indexWhere((e) => e.sessionId == sessionId);
        if (byId >= 0) {
          current[byId] = updated;
        } else {
          current.add(updated);
        }
      }

      await CloudScreenCache.saveJson(
        CloudScreenCache.mealSessions,
        current.map((e) => e.toJson()).toList(),
      );
      if (mounted) {
        setState(() => messMealSessionsPageSessions = AsyncData(current));
      }

      final online = await isDeviceOnline();
      var synced = false;
      if (online) {
        synced = await MessApi(ref.read(apiClientProvider)).saveMealSession(
          userId: userId,
          sessionId: int.tryParse(sessionId) != null ? sessionId : '',
          sessionName: sessionName,
          startTime: start,
          endTime: end,
          tokenPrefix: tokenPrefix,
          isActive: isActive,
          menuNotes: session.menuNotes,
          sortOrder: session.sortOrder,
        );
      }

      if (!mounted) return;
      if (synced) {
        await StaffOfflineQueue.setMealSessionsPending(false);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Session saved')),
        );
        await load();
      } else if (AppPlatform.requiresNetwork) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text(kWebApiSaveFailedMessage)),
        );
      } else {
        await StaffOfflineQueue.setMealSessionsPending(true);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Session saved'),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => saving = false);
    }
    nameCtrl.dispose();
    prefixCtrl.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppStrings.of(ref).mealSessions)),
      body: Column(
        children: [
          Expanded(
            child: messMealSessionsPageSessions.when(
              data: (rows) {
                if (rows.isEmpty) {
                  return AppEmptyState(
                    title: 'No meal sessions yet',
                    message: 'Add breakfast, lunch or dinner windows.',
                    iconAsset: AppAssets.svgClock,
                    actionLabel: 'Add session',
                    onAction: saving
                        ? null
                        : () => edit(
                            const MessMealSessionDto(
                              sessionId: '',
                              sessionName: '',
                            ),
                          ),
                  );
                }
                return ResponsiveScrollShell(
                  dashboard: true,
                  child: ListView.separated(
                      padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      12,
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      24,
                    ),
                    itemCount: rows.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final s = rows[index];
                      final color = sessionColor(index);
                      return AppCard(
                        accentColor: color,
                        padding: EdgeInsets.zero,
                        child: ListTile(
                          leading: AppModuleIcon(
                            svgPath: AppAssets.svgClock,
                            color: color,
                            size: 48,
                          ),
                          title: Text(
                            s.sessionName,
                            style: AppTypography.cardTitle(),
                          ),
                          subtitle: Text(
                            '${s.startTime} – ${s.endTime}'
                            '${s.tokenPrefix.isNotEmpty ? ' • ${s.tokenPrefix}' : ''}',
                          ),
                          trailing: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              AppStatusBadge(
                                label: s.active ? 'Active' : 'Inactive',
                                color: s.active
                                    ? AppColors.success
                                    : AppColors.textSecondary,
                              ),
                              const SizedBox(width: 8),
                              const AppSvg(
                                AppAssets.svgEdit,
                                width: 18,
                                height: 18,
                                color: AppColors.textSecondary,
                              ),
                            ],
                          ),
                          onTap: () => edit(s),
                        ),
                      );
                    },
                  ),
                );
              },
              loading: () =>
                  const AppLoadingState(message: 'Loading sessions…'),
              error: (e, _) => AppErrorState(message: '$e', onRetry: load),
            ),
          ),
        ],
      ),
    );
  }
}
