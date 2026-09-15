import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/core/widgets/app_module_icon.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/* Android InvoiceMess / CouponBluetoothPrint — paper meal coupon. */
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
  int messCouponPageLimit = 30;

  @override
  void initState() {
    super.initState();
    Future.microtask(loadCounts);
  }

  Future<void> loadCounts() async {
    final db = ref.read(appDatabaseProvider);
    final used = await db.countMessCouponsForMember(widget.member.memberName);
    final payments = await db.getLocalMessPayments(
      memberId: '${widget.member.memberId}',
    );
    var limit = 30;
    if (payments.isNotEmpty) {
      limit = int.tryParse(payments.first.messTotalDays) ?? 30;
    }
    if (!mounted) return;
    setState(() {
      messCouponPageUsed = used;
      messCouponPageLimit = limit;
    });
  }

  Future<void> issueAndPrint() async {
    if (messCouponPageUsed >= messCouponPageLimit) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Coupon limit $messCouponPageLimit reached for this member')),
      );
      return;
    }
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(kOnlineRequiredMessage)),
      );
      return;
    }
    setState(() => busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      final couponNo = messCouponPageUsed + 1;
      final id = await db.issueMessCoupon(
        memberId: '${widget.member.memberId}',
        memberName: widget.member.memberName,
        messType: messCouponPageMessType,
      );
      final now = DateFormat('dd MMM yyyy, hh:mm a').format(DateTime.now());
      final shop = ref.read(authControllerProvider).session?.shopName ??
          'POS Billingwala';
      final text = StringBuffer()
        ..writeln(shop)
        ..writeln('MESS COUPON')
        ..writeln('-' * 32)
        ..writeln(widget.member.memberName)
        ..writeln(messCouponPageMessType)
        ..writeln(now)
        ..writeln('MESS COUPON No: $couponNo')
        ..writeln('-' * 32)
        ..writeln();
      final printResult = await ref.read(printServiceProvider).printRawText(
            text.toString(),
            label: 'Mess coupon',
          );

      final userId = ref.read(authControllerProvider).session?.userId;
      var uploaded = false;
      if (userId != null && userId.isNotEmpty) {
        final row = await db.getMessInvoiceById(id);
        if (row != null) {
          final ok =
              await MessApi(ref.read(apiClientProvider)).insertMessInvoice(
            userId: userId,
            memberName: row.memberName,
            messType: row.messType,
            messInvoiceDate:
                DateFormat('yyyy-MM-dd HH:mm:ss').format(row.messInvoiceDate),
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
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text(kWebApiSaveFailedMessage)),
        );
        return;
      }

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            printResult.outcome == PrintOutcome.failed
                ? (printResult.message ?? 'Coupon saved (print failed)')
                : 'Coupon #$couponNo issued',
          ),
        ),
      );
      await loadCounts();
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
      appBar: AppBar(title: Text(AppStrings.of(ref).paperMessCoupon)),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
        children: [
            const AppModuleIcon(icon: Icons.confirmation_number_rounded, color: AppColors.orange, size: 62),
            const SizedBox(height: 10),
          AppCard(
            accentColor: AppColors.teal,
            padding: EdgeInsets.zero,
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              leading: const AppModuleIcon(icon: Icons.person_rounded, color: AppColors.teal, size: 52),
              title: Text(
                widget.member.memberName,
                style: const TextStyle(fontWeight: FontWeight.w800),
              ),
              subtitle: Text(
                'Used $messCouponPageUsed / $messCouponPageLimit coupons',
              ),
            ),
          ),
          const SizedBox(height: 16),
          const SizedBox(height: 4),
          const AppModuleIcon(icon: Icons.restaurant_menu_rounded, color: AppColors.orange, size: 54),
          const SizedBox(height: 8),
          Text(
            'Choose meal type',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: ['Lunch', 'Dinner', 'Breakfast', 'Snacks']
                .map(
                  (t) => ChoiceChip(
                    label: Text(t),
                    selected: messCouponPageMessType == t,
                    onSelected: busy
                        ? null
                        : (_) => setState(() => messCouponPageMessType = t),
                  ),
                )
                .toList(),
          ),
          const SizedBox(height: 24),
          AppButton(
            label: 'Print coupon',
            isLoading: busy,
            expanded: false,
            onPressed: issueAndPrint,
          ),
        ],
      ),
      ),
    );
  }
}
