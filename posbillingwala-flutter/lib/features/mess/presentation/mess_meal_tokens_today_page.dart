import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
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

  Future<List<MessMealTokenDto>> localPrintedToday(String day) async {
    final db = ref.read(appDatabaseProvider);
    final now = DateTime.now();
    final qr = await db.getMessTokensForDay(now);
    final invoices = await db.getMessInvoicesForDay(now);
    final qrKeys = <String>{};
    final out = <MessMealTokenDto>[];

    for (final t in qr) {
      final name = (t.memberName ?? '').trim().toLowerCase();
      final meal = t.messType.trim().toLowerCase();
      qrKeys.add('$name|$meal');
      final code = t.tokenCode;
      out.add(
        MessMealTokenDto(
          tokenId: 'qr-${t.tokenId}',
          tokenNumber: code.length > 8
              ? code.substring(0, 8).toUpperCase()
              : code.toUpperCase(),
          mealSession: t.messType.isEmpty ? 'QR Token' : t.messType,
          date: day,
          printStatus: 'PRINTED',
          createdAt: DateFormat('yyyy-MM-dd HH:mm:ss').format(t.tokenDate),
          memberName: t.memberName ?? '',
          memberMobile: t.memberMobile ?? '',
          registrationNo: t.memberMobile ?? '',
        ),
      );
    }

    for (final inv in invoices) {
      final name = inv.memberName.trim().toLowerCase();
      final meal = inv.messType.trim().toLowerCase();
      if (qrKeys.contains('$name|$meal')) continue;
      out.add(
        MessMealTokenDto(
          tokenId: 'coupon-${inv.invoiceId}',
          tokenNumber: 'COUPON',
          mealSession: inv.messType.isEmpty ? 'Coupon' : inv.messType,
          date: day,
          printStatus: 'PRINTED',
          createdAt: DateFormat(
            'yyyy-MM-dd HH:mm:ss',
          ).format(inv.messInvoiceDate),
          memberName: inv.memberName,
        ),
      );
    }
    return out;
  }

  Future<MessMealTokenTodayResult> fromLocalQueue(String day) async {
    final db = ref.read(appDatabaseProvider);
    final rows = await db.getMessMealTokenQueueForDate(day);
    final queue = rows
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
        .toList();
    return MessMealTokenTodayResult(tokens: queue);
  }

  List<MessMealTokenDto> mergeTokens(List<MessMealTokenDto> primary,
      List<MessMealTokenDto> extras,) {
    final seen = <String>{};
    final out = <MessMealTokenDto>[];
    for (final t in [...primary, ...extras]) {
      final key = t.tokenId.trim().isNotEmpty
          ? 'id:${t.tokenId.trim().toLowerCase()}'
          : 'm:${t.memberName.trim().toLowerCase()}|'
              '${t.mealSession.trim().toLowerCase()}|'
              '${t.tokenNumber.trim().toLowerCase()}';
      if (!seen.add(key)) continue;
      out.add(t);
    }
    return out;
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
    final localPrints = await localPrintedToday(day);

    final cached = await CloudScreenCache.loadMapList(
      CloudScreenCache.mealTokensToday,
    );
    if (cached.isNotEmpty && mounted) {
      setState(
        () => state = AsyncData(
          MessMealTokenTodayResult(
            tokens: mergeTokens(
              cached.map(MessMealTokenDto.fromJson).toList(),
              localPrints,
            ),
          ),
        ),
      );
    } else if (mounted) {
      setState(() => state = const AsyncLoading());
    }

    if (!await isDeviceOnline()) {
      final local = await fromLocalQueue(day);
      if (!mounted) return;
      setState(
        () => state = AsyncData(
          MessMealTokenTodayResult(
            tokens: mergeTokens(local.tokens, localPrints),
            sessionCounts: local.sessionCounts,
          ),
        ),
      );
      return;
    }

    final next = await AsyncValue.guard(() async {
      final remote = await MessApi(
        ref.read(apiClientProvider),
      ).fetchMealTokensToday(userId, date: day);
      var base = remote.tokens;
      if (base.isEmpty) {
        final local = await fromLocalQueue(day);
        base = local.tokens;
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
      return MessMealTokenTodayResult(
        tokens: mergeTokens(base, localPrints),
        sessionCounts: remote.sessionCounts,
      );
    });
    if (!mounted) return;
    if (next.hasError && state.hasValue) return;
    if (next.hasError) {
      final local = await fromLocalQueue(day);
      if (!mounted) return;
      final merged = mergeTokens(local.tokens, localPrints);
      setState(
        () => state = merged.isNotEmpty
            ? AsyncData(
                MessMealTokenTodayResult(
                  tokens: merged,
                  sessionCounts: local.sessionCounts,
                ),
              )
            : AsyncError(next.error!, next.stackTrace!),
      );
      return;
    }
    setState(() => state = next);
  }

  bool isLocalPrinted(MessMealTokenDto token) {
    return token.tokenId.startsWith('qr-') ||
        token.tokenId.startsWith('coupon-');
  }

  Future<void> printToken(MessMealTokenDto token) async {
    if (isLocalPrinted(token)) return;
    final db = ref.read(appDatabaseProvider);
    setState(() => busy = true);
    try {
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
    if (isLocalPrinted(token)) return;
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
          final printed = data.tokens
              .where((t) => t.printStatus.toUpperCase() == 'PRINTED')
              .length;
          final countsText = data.sessionCounts.isEmpty
              ? 'Today  ·  ${data.tokens.length} tokens  ·  $printed printed'
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
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text(
                                AppStrings.of(ref).noDataFound,
                                textAlign: TextAlign.center,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w700,
                                  fontSize: 16,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                AppStrings.of(ref).emptyMessTokensToday,
                                textAlign: TextAlign.center,
                                style: const TextStyle(
                                  color: AppColors.textSecondary,
                                  fontSize: 13,
                                ),
                              ),
                            ],
                          ),
                        ),
                      )
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
                            final local = isLocalPrinted(t);
                            return AppCard(
                              padding: EdgeInsets.zero,
                              child: ListTile(
                                leading: CircleAvatar(
                                  backgroundColor: AppColors.primary.withValues(
                                    alpha: 0.12,
                                  ),
                                  child: Icon(
                                    local
                                        ? Icons.print_rounded
                                        : Icons.confirmation_number_outlined,
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
                                trailing: local
                                    ? null
                                    : Wrap(
                                        spacing: 0,
                                        children: [
                                          IconButton(
                                            tooltip: 'Retry print',
                                            onPressed: busy
                                                ? null
                                                : () => printToken(t),
                                            icon: const Icon(
                                              Icons.print_rounded,
                                            ),
                                          ),
                                          IconButton(
                                            tooltip: 'Cancel',
                                            onPressed: busy
                                                ? null
                                                : () =>
                                                      messMealTokensTodayPageCancelToken(
                                                        t,
                                                      ),
                                            icon: const Icon(
                                              Icons.cancel_outlined,
                                            ),
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
