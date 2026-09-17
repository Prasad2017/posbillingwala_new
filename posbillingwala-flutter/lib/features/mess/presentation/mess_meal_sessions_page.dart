import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class MessMealSessionsPage extends ConsumerStatefulWidget {
  const MessMealSessionsPage({super.key});

  @override
  ConsumerState<MessMealSessionsPage> createState() =>
      MessMealSessionsPageState();
}

Color sessionColor(int index) => [AppColors.orange, AppColors.purple, AppColors.teal, AppColors.primary][index % 4];

class MessMealSessionsPageState extends ConsumerState<MessMealSessionsPage> {
  AsyncValue<List<MessMealSessionDto>> messMealSessionsPageSessions = const AsyncLoading();
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
        () => messMealSessionsPageSessions =
            AsyncError('Login required', StackTrace.current),
      );
      return;
    }
    setState(() => messMealSessionsPageSessions = const AsyncLoading());
    final cached =
        await CloudScreenCache.loadMapList(CloudScreenCache.mealSessions);
    if (cached.isNotEmpty && mounted) {
      setState(
        () => messMealSessionsPageSessions = AsyncData(
          cached.map(MessMealSessionDto.fromJson).toList(),
        ),
      );
    }
    final result = await AsyncValue.guard(() async {
      return MessApi(ref.read(apiClientProvider)).fetchMealSessions(userId);
    });
    if (!mounted) return;
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
          title: Text(session.sessionId.isEmpty ? 'Add session' : 'Edit session'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AppTextField(
                      controller: nameCtrl,
                      label: 'Session name',
                    ),
                const SizedBox(height: 12),
                AppTextField(
                      controller: prefixCtrl,
                      label: 'Token prefix',
                    ),
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
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(AppStrings.of(ref).active),
                  value: active,
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
      final success = await MessApi(ref.read(apiClientProvider)).saveMealSession(
        userId: userId,
        sessionId: session.sessionId.isEmpty
            ? 'sess_${DateTime.now().millisecondsSinceEpoch}'
            : session.sessionId,
        sessionName: nameCtrl.text.trim(),
        startTime: start,
        endTime: end,
        tokenPrefix: prefixCtrl.text.trim(),
        isActive: active ? '1' : '0',
        menuNotes: session.menuNotes,
        sortOrder: session.sortOrder,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(success ? 'Session saved' : 'Save failed'),
        ),
      );
      if (success) await load();
    } finally {
      if (mounted) setState(() => saving = false);
    }
    nameCtrl.dispose();
    prefixCtrl.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppStrings.of(ref).mealSessions),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: saving
            ? null
            : () => edit(
                  const MessMealSessionDto(
                    sessionId: '',
                    sessionName: '',
                  ),
                ),
        child: const Icon(Icons.add),
      ),
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
            88),
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
                        title: Text(s.sessionName, style: AppTypography.cardTitle()),
                        subtitle: Text(
                          '${s.startTime} – ${s.endTime}'
                          '${s.tokenPrefix.isNotEmpty ? ' • ${s.tokenPrefix}' : ''}',
                        ),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            AppStatusBadge(
                              label: s.active ? 'Active' : 'Inactive',
                              color: s.active ? AppColors.success : AppColors.textSecondary,
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
              loading: () => const AppLoadingState(message: 'Loading sessions…'),
              error: (e, _) => AppErrorState(
                message: '$e',
                onRetry: load,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
