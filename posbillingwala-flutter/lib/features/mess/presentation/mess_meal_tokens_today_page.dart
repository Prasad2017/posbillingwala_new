import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_token_qr_page.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
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
    if (!await isDeviceOnline()) return;
    final next = await AsyncValue.guard(
      () => MessApi(ref.read(apiClientProvider)).fetchMealTokensToday(userId),
    );
    if (!mounted) return;
    if (next.hasError && state.hasValue) return;
    setState(() => state = next);
  }

  Future<void> printToken(MessMealTokenDto token) async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null) return;
    final memberName = token.memberName.trim();
    final memberMobile = messTokenDigits(token.memberMobile).isNotEmpty
        ? messTokenDigits(token.memberMobile)
        : messTokenDigits(token.registrationNo);
    if (!messTokenHasRequiredIdentity(memberName, memberMobile)) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(AppStrings.of(ref).tokenPrintNameMobileRequired),
        ),
      );
      return;
    }
    setState(() => busy = true);
    try {
      final text = StringBuffer()
        ..writeln('MESS MEAL TOKEN')
        ..writeln(token.tokenNumber)
        ..writeln(token.mealSession)
        ..writeln(memberName)
        ..writeln(memberMobile)
        ..writeln(token.date)
        ..writeln();
      final printResult = await ref
          .read(printServiceProvider)
          .printRawText(text.toString(), label: 'Mess meal token');
      final device = await DeviceIdentityService().resolve();
      final ok =
          printResult.outcome != PrintOutcome.failed &&
          printResult.outcome != PrintOutcome.previewOnly;
      await MessApi(ref.read(apiClientProvider)).ackMealTokenPrint(
        userId: userId,
        tokenId: token.tokenId,
        result: ok ? 'PRINTED' : 'PRINT_FAILED',
        deviceId: device.deviceId,
        deviceName: device.deviceName,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(printResult.message ?? printResult.outcome.name),
        ),
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
                                    tooltip: 'Print',
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
