import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
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
      await ref
          .read(appDatabaseProvider)
          .upsertLocalMessPayment(
            memberId: '$memberId',
            memberName: result.name,
            messAmount: result.messAmount ?? 0,
            paidAmount: result.messPaidAmount ?? 0,
            messTotalDays: result.messDays ?? '30',
            paymentDate: month,
            paymentNetworkStatus: network,
          );
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
      body: MembersTab(onEditMember: editMember),
    );
  }
}
