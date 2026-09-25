import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_meal_token_print_worker.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class MessMealTokensTodayPage extends ConsumerStatefulWidget {
  const MessMealTokensTodayPage({super.key});

  @override
  ConsumerState<MessMealTokensTodayPage> createState() =>
      MessMealTokensTodayPageState();
}

class MessMealTokensTodayPageState
    extends ConsumerState<MessMealTokensTodayPage> {
  AsyncValue<MessMealTokenTodayResult> state = const AsyncLoading();
  bool busy = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      setState(() {
        state = AsyncError('Login required', StackTrace.current);
      });
      return;
    }
    final day = DateFormat('yyyy-MM-dd').format(DateTime.now());
    final db = ref.read(appDatabaseProvider);

    final cached = await CloudScreenCache.loadMapList(
      CloudScreenCache.mealTokensToday,
    );
    if (cached.isNotEmpty && mounted) {
      setState(
        () => state = AsyncData(
          MessMealTokenTodayResult(
            tokens: cached.map(MessMealTokenDto.fromJson).toList(),
          ),
        ),
      );
    } else if (mounted) {
      setState(() => state = const AsyncLoading());
    }

    /* Android MessMealTokenTodayActivity — fall back to local print queue. */
    Future<MessMealTokenTodayResult> fromLocalQueue() async {
      final rows = await db.getMessMealTokenQueueForDate(day);
      return MessMealTokenTodayResult(
        tokens: rows
            .map(
              (e) => MessMealTokenDto(
                tokenId: e.serverPublicId,
                tokenNumber: e.tokenNumber ?? '',
                registrationNo: e.registrationNo ?? '',
                mealSession: e.mealSession ?? '',
                date: e.tokenDate ?? day,
                printStatus: e.printStatus,
                createdAt: e.createdAt ?? '',
                memberName: e.memberName ?? '',
                memberMobile: e.registrationNo ?? '',
              ),
            )
            .toList(),
      );
    }

    if (!await isDeviceOnline()) {
      final local = await fromLocalQueue();
      if (!mounted) return;
      setState(() => state = AsyncData(local));
      return;
    }

    final next = await AsyncValue.guard(() async {
      final remote = await MessApi(
        ref.read(apiClientProvider),
      ).fetchMealTokensToday(userId, date: day);
      /* Keep local queue rows that API omitted (just printed / offline). */
      if (remote.tokens.isEmpty) {
        final local = await fromLocalQueue();
        if (local.tokens.isNotEmpty) return local;
      } else {
        for (final t in remote.tokens) {
          if (t.tokenId.isEmpty) continue;
          await db.enqueueMessMealToken(
            serverPublicId: t.tokenId,
            tokenNumber: t.tokenNumber,
            registrationNo: t.registrationNo,
            mealSession: t.mealSession,
            tokenDate: t.date.isNotEmpty ? t.date : day,
            memberName: t.memberName,
            createdAt: t.createdAt,
            printStatus: t.printStatus.isNotEmpty
                ? t.printStatus
                : 'PRINT_PENDING',
          );
        }
        await CloudScreenCache.saveJson(
          CloudScreenCache.mealTokensToday,
          remote.tokens
              .map(
                (e) => {
                  'tokenId': e.tokenId,
                  'tokenNumber': e.tokenNumber,
                  'registrationNo': e.registrationNo,
                  'mealSession': e.mealSession,
                  'date': e.date,
                  'printStatus': e.printStatus,
                  'createdAt': e.createdAt,
                  'printedAt': e.printedAt,
                  'memberName': e.memberName,
                  'memberMobile': e.memberMobile,
                },
              )
              .toList(),
        );
      }
      return remote;
    });
    if (!mounted) return;
    if (next.hasError && state.hasValue) return;
    if (next.hasError) {
      final local = await fromLocalQueue();
      if (!mounted) return;
      setState(
        () => state = local.tokens.isNotEmpty
            ? AsyncData(local)
            : AsyncError(next.error!, next.stackTrace!),
      );
      return;
    }
    setState(() => state = next);
  }

  Future<void> printToken(MessMealTokenDto token) async {
    final db = ref.read(appDatabaseProvider);
    setState(() => busy = true);
    try {
      /* Android MessMealTokenTodayActivity — requeue then drain worker. */
      await db.enqueueMessMealToken(
        serverPublicId: token.tokenId,
        tokenNumber: token.tokenNumber,
        registrationNo: token.memberMobile.trim().isNotEmpty
            ? token.memberMobile
            : token.registrationNo,
        mealSession: token.mealSession,
        tokenDate: token.date,
        memberName: token.memberName,
        createdAt: token.createdAt,
        printStatus: 'PRINT_PENDING',
      );
      await ref.read(messMealTokenPrintWorkerProvider).kick();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Print queued')),
      );
      await load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> messMealTokensTodayPageCancelToken(
    MessMealTokenDto token,
  ) async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null) return;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) {
        final screenW = MediaQuery.sizeOf(context).width;
        return AlertDialog(
        insetPadding: EdgeInsets.symmetric(
          horizontal: screenW < 360 ? 12 : 24,
          vertical: 24,
        ),
        title: Text(AppStrings.of(ref).cancelToken),
        content: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: screenW - 48),
          child: Text('Cancel ${token.tokenNumber}?'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('No'),
          ),
          AppButton(
            label: 'Cancel token',
            onPressed: () => Navigator.pop(context, true),
          ),
        ],
      );
      },
    );
    if (confirm != true) return;
    setState(() => busy = true);
    try {
      await MessApi(
        ref.read(apiClientProvider),
      ).cancelMealToken(userId: userId, tokenId: token.tokenId);
      await load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Today's Mess Tokens"),
        actions: [
          IconButton(
            onPressed: busy ? null : load,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: state.when(
        data: (data) {
          final countsText = data.sessionCounts.isEmpty
              ? ''
              : data.sessionCounts
                    .map(
                      (c) =>
                          '${c.sessionName}: Gen ${c.generated} · '
                          'Printed ${c.printed} · Pending ${c.pending} · '
                          'Failed ${c.failed}',
                    )
                    .join('\n');
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (countsText.isNotEmpty)
                ResponsiveContent(
                  dashboard: true,
                  padding: EdgeInsets.all(
                    AppBreakpoints.pagePaddingFor(context.widthClass),
                  ),
                  child: Text(
                    countsText,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ),
              Expanded(
                child: data.tokens.isEmpty
                    ? Center(child: Text(AppStrings.of(ref).noQrTokensToday))
                    : ResponsiveScrollShell(
                        dashboard: true,
                        child: ListView.separated(
                        padding: EdgeInsets.all(
                          AppBreakpoints.pagePaddingFor(context.widthClass),
                        ),
                        itemCount: data.tokens.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 8),
                        itemBuilder: (context, i) {
                          final t = data.tokens[i];
                          return AppCard(
                            padding: EdgeInsets.zero,
                            child: ListTile(
                              leading: CircleAvatar(
                                backgroundColor: AppColors.primary.withValues(
                                  alpha: 0.12,
                                ),
                                child: const Icon(
                                  Icons.confirmation_number_outlined,
                                  color: AppColors.primary,
                                ),
                              ),
                              title: Text(
                                t.tokenNumber.isEmpty
                                    ? t.tokenId
                                    : t.tokenNumber,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              subtitle: Text(
                                [
                                  if (t.memberName.isNotEmpty) t.memberName,
                                  if (t.memberMobile.isNotEmpty)
                                    t.memberMobile
                                  else if (t.registrationNo.isNotEmpty)
                                    t.registrationNo,
                                  if (t.mealSession.isNotEmpty) t.mealSession,
                                  if (t.printStatus.isNotEmpty) t.printStatus,
                                ].join(' · '),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              trailing: Wrap(
                                spacing: 0,
                                children: [
                                  IconButton(
                                    tooltip: 'Retry print',
                                    onPressed: busy
                                        ? null
                                        : () => printToken(t),
                                    icon: const Icon(Icons.print_rounded),
                                  ),
                                  IconButton(
                                    tooltip: 'Cancel',
                                    onPressed: busy
                                        ? null
                                        : () =>
                                              messMealTokensTodayPageCancelToken(
                                                t,
                                              ),
                                    icon: const Icon(Icons.cancel_outlined),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                      ),
              ),
            ],
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('$e', textAlign: TextAlign.center),
                const SizedBox(height: 12),
                AppButton(label: 'Retry', expanded: false, onPressed: load),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
