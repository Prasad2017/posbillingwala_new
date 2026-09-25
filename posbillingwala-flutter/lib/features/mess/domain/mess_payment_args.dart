import 'package:pos_billingwala_v2/core/database/app_database.dart';

/* How Member List opens payments — Android History / New Payment / Pay Pending. */
enum MessPaymentOpenMode { list, history, newPayment, payPending }

class MessPaymentsArgs {
  const MessPaymentsArgs({
    required this.member,
    this.mode = MessPaymentOpenMode.list,
  });

  final MessMember member;
  final MessPaymentOpenMode mode;
}
