import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_token_qr_page.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Dedicated Member List screen (opened from Mess hub card). */
class MessMembersPage extends ConsumerStatefulWidget {
  const MessMembersPage({super.key});

  @override
  ConsumerState<MessMembersPage> createState() => MessMembersPageState();
}

class MessMembersPageState extends ConsumerState<MessMembersPage> {
  Future<void> editMember(MessMember member) async {
    final result = await showMemberFormDialog(
      context,
      title: 'Edit mess member',
      initial: member,
    );
    if (result == null || !mounted) return;
    if (result.name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).nameRequired)));
      return;
    }
    if (messTokenDigits(result.mobile).length != 10) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).mobileRequired)),
      );
      return;
    }
    await ref
        .read(messControllerProvider.notifier)
        .updateLocalMember(
          memberId: member.memberId,
          name: result.name,
          mobile: result.mobile,
          altMobile: result.altMobile,
          address: result.address,
          registrationNo: result.registrationNo,
          memberType: result.memberType,
          rollNo: result.rollNo,
          college: result.college,
          studentYear: result.studentYear,
          company: result.company,
        );
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).memberUpdated)));
  }

  Future<void> addMember() async {
    final result = await showMemberFormDialog(
      context,
      title: 'Add mess member',
    );
    if (result == null || !mounted) return;
    if (result.name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).nameRequired)));
      return;
    }
    if (messTokenDigits(result.mobile).length != 10) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).mobileRequired)),
      );
      return;
    }

    final memberId = await ref
        .read(messControllerProvider.notifier)
        .addLocalMember(
          name: result.name,
          mobile: result.mobile,
          altMobile: result.altMobile,
          address: result.address,
          registrationNo: result.registrationNo,
          memberType: result.memberType,
          rollNo: result.rollNo,
          college: result.college,
          studentYear: result.studentYear,
          company: result.company,
        );
    if (result.messAmount != null || result.messPaidAmount != null) {
      final month = DateFormat('yyyy-MM').format(DateTime.now());
      final network = 'pay_${DateTime.now().millisecondsSinceEpoch}';
      final messAmount = result.messAmount ?? 0;
      final paidAmount = result.messPaidAmount ?? 0;
      final messDays = result.messDays ?? 'Two Time';
      final db = ref.read(appDatabaseProvider);
      await db.upsertLocalMessPayment(
        memberId: '$memberId',
        memberName: result.name,
        messAmount: messAmount,
        paidAmount: paidAmount,
        messTotalDays: messDays,
        paymentDate: month,
        paymentNetworkStatus: network,
      );
      final userId = ref.read(authControllerProvider).session?.userId;
      if (userId != null && userId.isNotEmpty) {
        try {
          final ok = await MessApi(ref.read(apiClientProvider)).insertMemberPayment(
            userId: userId,
            memberId: '$memberId',
            memberName: result.name,
            paymentMessAmount: messAmount.toStringAsFixed(2),
            paymentPaidAmount: paidAmount.toStringAsFixed(2),
            messTotalDays: messDays,
            paymentDate: month,
            paymentNetworkStatus: network,
          );
          if (ok) {
            final pending = await db.getPendingMessPayments();
            for (final row in pending) {
              if (row.paymentNetworkStatus == network) {
                await db.markMessPaymentSynced(row.localPaymentId);
              }
            }
          }
        } catch (_) {
          /* Local row remains pending for sync. */
        }
      }
    }
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).memberSaved)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Member List'),
        actions: [
          TextButton.icon(
            onPressed: addMember,
            icon: const Icon(Icons.person_add_alt_1_rounded, color: Colors.white),
            label: Text(
              AppStrings.of(ref).addMember,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: MembersTab(onEditMember: editMember),
      ),
    );
  }
}
