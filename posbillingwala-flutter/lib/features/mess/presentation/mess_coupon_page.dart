import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_slip_preview.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Android CouponBluetoothPrint — paper meal coupon (save after print). */
class MessCouponPage extends ConsumerStatefulWidget {
  const MessCouponPage({super.key, required this.member});

  final MessMember member;

  @override
  ConsumerState<MessCouponPage> createState() => MessCouponPageState();
}

class MessCouponPageState extends ConsumerState<MessCouponPage> {
  String messCouponPageMessType = 'Lunch';
  bool busy = false;
  int messCouponPageUsed = 0;
  int messCouponPageLimit = 2;

  @override
  void initState() {
    super.initState();
    Future.microtask(loadCounts);
  }

  Future<void> loadCounts() async {
    final db = ref.read(appDatabaseProvider);
    final used = await db.countMessCouponsForMemberOnDay(
      widget.member.memberName,
      DateTime.now(),
    );
    final payments = await db.getLocalMessPayments(
      memberId: '${widget.member.memberId}',
    );
    var limit = 2;
    if (payments.isNotEmpty) {
      final days = payments.first.messTotalDays;
      final t = days.trim().toLowerCase();
      if (t == 'one time' || t == '1') {
        limit = 1;
      } else {
        limit = int.tryParse(days) ?? 2;
        if (limit > 2) limit = 2;
      }
    }
    final type = MessSlipBuilder.resolveMessType(existingPrintsToday: used);
    if (!mounted) return;
    setState(() {
      messCouponPageUsed = used;
      messCouponPageLimit = limit;
      messCouponPageMessType = type;
    });
  }

  Future<void> issueAndPrint() async {
    if (messCouponPageUsed >= messCouponPageLimit) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Already coupon created')),
      );
      return;
    }
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text(kOnlineRequiredMessage)));
      return;
    }
    setState(() => busy = true);
    try {
      final couponNo = messCouponPageUsed + 1;
      final messType = MessSlipBuilder.resolveMessType(
        existingPrintsToday: messCouponPageUsed,
      );
      final profile = ref.read(shopReceiptProfileProvider);
      final layout = MessSlipBuilder.couponLayout(
        profile: profile,
        memberName: widget.member.memberName,
        messType: messType,
        couponNo: couponNo,
      );
      final printResult = await ref.read(printServiceProvider).printMessSlip(
        layout.toPlainText(
          width: ref.read(printerSettingsProvider).charsPerLine,
        ),
        layout: layout,
        label: 'Mess coupon',
      );
      final printed =
          printResult.outcome != PrintOutcome.failed &&
          printResult.outcome != PrintOutcome.previewOnly;
      if (!printed) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(printResult.message ?? 'Print failed — not saved'),
          ),
        );
        return;
      }

      /* Android: saveMessInvoice only after successful print. */
      final db = ref.read(appDatabaseProvider);
      final id = await db.issueMessCoupon(
        memberId: '${widget.member.memberId}',
        memberName: widget.member.memberName,
        messType: messType.isEmpty ? 'Lunch' : messType,
      );

      final userId = ref.read(authControllerProvider).session?.userId;
      var uploaded = false;
      if (userId != null && userId.isNotEmpty) {
        final row = await db.getMessInvoiceById(id);
        if (row != null) {
          final ok = await MessApi(ref.read(apiClientProvider))
              .insertMessInvoice(
                userId: userId,
                memberName: row.memberName,
                messType: row.messType,
                messInvoiceDate: DateFormat(
                  'yyyy-MM-dd HH:mm:ss',
                ).format(row.messInvoiceDate),
                messInvoiceNetworkStatus: row.messInvoiceNetworkStatus,
                messInvoiceStatus: '0',
              );
          if (ok) {
            await db.markMessInvoiceSynced(id);
            uploaded = true;
          }
        }
      }
      if (AppPlatform.requiresNetwork && !uploaded) {
        if (!mounted) return;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text(kWebApiSaveFailedMessage)));
        return;
      }

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Coupon saved')),
      );
      await loadCounts();
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profile = ref.watch(shopReceiptProfileProvider);
    final couponNo = messCouponPageUsed + 1;
    final atLimit = messCouponPageUsed >= messCouponPageLimit;
    final layout = messCouponPreviewLayout(
      profile: profile,
      memberName: widget.member.memberName,
      messType: messCouponPageMessType,
      couponNo: couponNo.clamp(1, 99),
    );

    return Scaffold(
      appBar: AppBar(title: Text(AppStrings.of(ref).paperMessCoupon)),
      body: Column(
        children: [
          Expanded(
            child: ResponsiveScrollShell(
              dashboard: true,
              child: ListView(
                padding: EdgeInsets.all(
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                ),
                children: [
                  Text(
                    'Today $messCouponPageUsed / $messCouponPageLimit · '
                    'Meal: ${messCouponPageMessType.isEmpty ? '—' : messCouponPageMessType}',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.black54,
                    ),
                  ),
                  const SizedBox(height: 8),
                  MessSlipPreview(layout: layout),
                  if (atLimit) ...[
                    const SizedBox(height: 12),
                    const Text(
                      'Already coupon created for today',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: AppColors.danger),
                    ),
                  ],
                ],
              ),
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                8,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                12,
              ),
              child: AppButton(
                label: 'Print coupon',
                icon: Icons.print_rounded,
                isLoading: busy,
                onPressed: busy || atLimit ? null : issueAndPrint,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
