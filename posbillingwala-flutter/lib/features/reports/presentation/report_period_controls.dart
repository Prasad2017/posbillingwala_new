import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

Future<void> pickReportMonth(BuildContext context, WidgetRef ref) async {
  final period = ref.read(reportPeriodProvider);
  final now = DateTime.now();
  final initial = period.kind == ReportPeriodKind.month && period.day != null
      ? period.day!
      : now;
  final picked = await showDatePicker(
    context: context,
    builder: (context, child) => Theme(
      data: Theme.of(context).copyWith(
        colorScheme: Theme.of(context).colorScheme.copyWith(
          primary: AppColors.primary,
          secondary: AppColors.red,
        ),
      ),
      child: child!,
    ),
    initialDate: DateTime(initial.year, initial.month, 1),
    firstDate: DateTime(now.year - 3, 1, 1),
    lastDate: DateTime(now.year, now.month, 1),
    helpText: 'Pick any day in the month',
  );
  if (picked != null) {
    ref.read(reportPeriodProvider.notifier).useMonth(picked);
  }
}

void onReportPeriodSelected(
  WidgetRef ref,
  ReportPeriodKind kind,
  ReportPeriod current,
) {
  switch (kind) {
    case ReportPeriodKind.all:
      ref.read(reportPeriodProvider.notifier).useAll();
    case ReportPeriodKind.today:
      ref.read(reportPeriodProvider.notifier).useToday();
    case ReportPeriodKind.month:
      ref.read(reportPeriodProvider.notifier).useMonth(current.day);
    case ReportPeriodKind.day:
      final day = current.kind == ReportPeriodKind.day
          ? (current.day ?? DateTime.now())
          : DateTime.now();
      ref.read(reportPeriodProvider.notifier).useDay(day);
    case ReportPeriodKind.year:
      ref.read(reportPeriodProvider.notifier).useYear(current.day);
  }
}

const List<ButtonSegment<ReportPeriodKind>> kReportPeriodSegments = [
  ButtonSegment(value: ReportPeriodKind.today, label: Text('Today')),
  ButtonSegment(value: ReportPeriodKind.month, label: Text('Month')),
  ButtonSegment(value: ReportPeriodKind.day, label: Text('Day')),
  ButtonSegment(value: ReportPeriodKind.year, label: Text('Year')),
];

/* Blue period filter menu used across refreshed report screens. */
Future<void> showReportPeriodFilterMenu(
  BuildContext context,
  WidgetRef ref, {
  List<PopupMenuEntry<String>> extraItems = const [],
  Future<void> Function(String value)? onExtra,
}) async {
  final period = ref.read(reportPeriodProvider);
  final selected = await showMenu<String>(
    context: context,
    position: const RelativeRect.fromLTRB(1000, 80, 16, 0),
    color: AppColors.primary,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    items: [
      PopupMenuItem(
        value: 'day',
        child: Row(
          children: [
            const Icon(
              Icons.calendar_month_rounded,
              color: Colors.white,
              size: 18,
            ),
            const SizedBox(width: 10),
            Text(
              AppStrings.of(ref).dayWise,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
      PopupMenuItem(
        value: 'month',
        child: Row(
          children: [
            const Icon(
              Icons.calendar_month_rounded,
              color: Colors.white,
              size: 18,
            ),
            const SizedBox(width: 10),
            Text(
              AppStrings.of(ref).monthWise,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
      PopupMenuItem(
        value: 'year',
        child: Row(
          children: [
            const Icon(
              Icons.calendar_month_rounded,
              color: Colors.white,
              size: 18,
            ),
            const SizedBox(width: 10),
            Text(
              AppStrings.of(ref).yearWise,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
      ...extraItems,
    ],
  );
  if (!context.mounted || selected == null) return;
  if (onExtra != null &&
      selected != 'day' &&
      selected != 'month' &&
      selected != 'year') {
    await onExtra(selected);
    return;
  }
  if (selected == 'month') {
    await pickReportMonth(context, ref);
    return;
  }
  if (selected == 'day') {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: period.day ?? now,
      firstDate: DateTime(now.year - 2),
      lastDate: now,
    );
    if (picked != null) {
      ref.read(reportPeriodProvider.notifier).useDay(picked);
    }
    return;
  }
  onReportPeriodSelected(
    ref,
    selected == 'year' ? ReportPeriodKind.year : ReportPeriodKind.today,
    period,
  );
}
