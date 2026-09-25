import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

/* Selected calendar day for Print Fast Bill (date-only; time applied at save). */
class BillingDateController extends Notifier<DateTime> {
  @override
  DateTime build() {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day);
  }

  void setDate(DateTime date) {
    final today = DateTime.now();
    final day = DateTime(date.year, date.month, date.day);
    final max = DateTime(today.year, today.month, today.day);
    state = day.isAfter(max) ? max : day;
  }

  void resetToToday() {
    final now = DateTime.now();
    state = DateTime(now.year, now.month, now.day);
  }
}

final billingDateProvider =
    NotifierProvider<BillingDateController, DateTime>(BillingDateController.new);

/* Effective invoice timestamp: selected day when Print Fast Bill is ON. */
DateTime resolveBillingDateTime({
  required bool printFastBill,
  required DateTime selectedDate,
  DateTime? clock,
}) {
  final now = clock ?? DateTime.now();
  if (!printFastBill) return now;
  return DateTime(
    selectedDate.year,
    selectedDate.month,
    selectedDate.day,
    now.hour,
    now.minute,
    now.second,
    now.millisecond,
  );
}

DateTime resolveBillingDateTimeFromRef(Ref ref, {DateTime? clock}) {
  final settings = ref.read(printerSettingsProvider);
  final selected = ref.read(billingDateProvider);
  return resolveBillingDateTime(
    printFastBill: settings.printFastBill,
    selectedDate: selected,
    clock: clock,
  );
}

final billingDateDisplayFormat = DateFormat('dd-MMM-yyyy');

/* Invoice-preview header only. Hidden when Print Fast Bill is OFF. Max date = today. */
class BillingDateBar extends ConsumerWidget {
  const BillingDateBar({
    super.key,
    this.inAppBar = false,
  });

  final bool inAppBar;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final printFastBill = ref.watch(
      printerSettingsProvider.select((s) => s.printFastBill),
    );
    if (!printFastBill) {
      return const SizedBox.shrink();
    }

    final selected = ref.watch(billingDateProvider);
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final display = selected.isAfter(today) ? today : selected;
    final label = billingDateDisplayFormat.format(display);

    Future<void> pickDate() async {
      final picked = await showDatePicker(
        context: context,
        initialDate: display,
        firstDate: DateTime(2020),
        lastDate: today,
      );
      if (picked == null) return;
      ref.read(billingDateProvider.notifier).setDate(picked);
    }

    if (inAppBar) {
      return InkWell(
        onTap: pickDate,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.only(top: 2, right: 4, bottom: 2),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.calendar_today_rounded,
                size: 12,
                color: Color(0xFFB8D4FF),
              ),
              const SizedBox(width: 6),
              Flexible(
                child: Text(
                  'Billing Date: $label',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Color(0xFFB8D4FF),
                    fontWeight: FontWeight.w600,
                    fontSize: 12.5,
                  ),
                ),
              ),
              const Icon(
                Icons.arrow_drop_down_rounded,
                size: 18,
                color: Color(0xFFB8D4FF),
              ),
            ],
          ),
        ),
      );
    }

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
      child: Align(
        alignment: Alignment.centerLeft,
        child: Material(
          color: AppColors.primary.withValues(alpha: 0.08),
          borderRadius: BorderRadius.circular(10),
          child: InkWell(
            onTap: pickDate,
            borderRadius: BorderRadius.circular(10),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.calendar_today_rounded,
                    size: 16,
                    color: AppColors.textSecondary,
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Text(
                          'Billing Date',
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.w600,
                            color: AppColors.textSecondary,
                            height: 1.1,
                          ),
                        ),
                        const SizedBox(height: 1),
                        Text(
                          label,
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w800,
                            color: AppColors.navy,
                            height: 1.15,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 4),
                  const Icon(
                    Icons.arrow_drop_down_rounded,
                    size: 22,
                    color: AppColors.textSecondary,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
