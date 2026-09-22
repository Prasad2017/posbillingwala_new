import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/features/auth/domain/license_validator.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';
import 'package:shared_preferences/shared_preferences.dart';

Widget _themedPickerShell(BuildContext context, Widget child) {
  return Theme(
    data: Theme.of(context).copyWith(
      colorScheme: Theme.of(context).colorScheme.copyWith(
        primary: AppColors.primary,
        onPrimary: Colors.white,
        secondary: AppColors.primary,
      ),
    ),
    child: child,
  );
}

/* Registration → today bounds for report day/month/year pickers. */
class ReportPickerBounds {
  const ReportPickerBounds({required this.first, required this.last});

  final DateTime first;
  final DateTime last;

  DateTime get firstDay => DateTime(first.year, first.month, first.day);
  DateTime get lastDay => DateTime(last.year, last.month, last.day);
  DateTime get firstMonth => DateTime(first.year, first.month);
  DateTime get lastMonth => DateTime(last.year, last.month);
  int get firstYear => first.year;
  int get lastYear => last.year;

  DateTime clampDay(DateTime value) {
    final d = DateTime(value.year, value.month, value.day);
    if (d.isBefore(firstDay)) return firstDay;
    if (d.isAfter(lastDay)) return lastDay;
    return d;
  }

  DateTime clampMonth(DateTime value) {
    final m = DateTime(value.year, value.month);
    if (m.isBefore(firstMonth)) return firstMonth;
    if (m.isAfter(lastMonth)) return lastMonth;
    return m;
  }

  DateTime clampYear(DateTime value) {
    final y = value.year;
    if (y < firstYear) return DateTime(firstYear);
    if (y > lastYear) return DateTime(lastYear);
    return DateTime(y);
  }
}

Future<ReportPickerBounds> loadReportPickerBounds() async {
  final now = DateTime.now();
  final last = DateTime(now.year, now.month, now.day);
  var first = last;

  final prefs = await SharedPreferences.getInstance();
  final issuedRaw = prefs.getString('issuedAt')?.trim() ?? '';
  var issuedSec = int.tryParse(issuedRaw) ?? 0;

  /* Fallback: signed licence payload issuedAt (unix seconds). */
  if (issuedSec <= 0) {
    try {
      final payload = await LicenseValidator.verifyAndParse(prefs);
      if (payload != null && payload.issuedAt > 0) {
        issuedSec = payload.issuedAt;
      }
    } catch (_) {
      /* keep fallback below */
    }
  }

  if (issuedSec > 0) {
    final issued = DateTime.fromMillisecondsSinceEpoch(issuedSec * 1000);
    first = DateTime(issued.year, issued.month, issued.day);
  }

  if (first.isAfter(last)) {
    first = last;
  }
  return ReportPickerBounds(first: first, last: last);
}

/* Day wise → full calendar date picker. */
Future<void> pickReportDay(BuildContext context, WidgetRef ref) async {
  final period = ref.read(reportPeriodProvider);
  final bounds = await loadReportPickerBounds();
  if (!context.mounted) return;
  final initial = bounds.clampDay(
    period.kind == ReportPeriodKind.day && period.day != null
        ? period.day!
        : bounds.lastDay,
  );
  final picked = await showDatePicker(
    context: context,
    builder: (context, child) => _themedPickerShell(context, child!),
    initialDate: initial,
    firstDate: bounds.firstDay,
    lastDate: bounds.lastDay,
    helpText: 'Select day',
  );
  if (picked != null) {
    ref.read(reportPeriodProvider.notifier).useDay(picked);
  }
}

/* Month wise → month + year only (no day). */
Future<void> pickReportMonth(BuildContext context, WidgetRef ref) async {
  final period = ref.read(reportPeriodProvider);
  final bounds = await loadReportPickerBounds();
  if (!context.mounted) return;
  final initial = bounds.clampMonth(
    period.kind == ReportPeriodKind.month && period.day != null
        ? period.day!
        : bounds.lastMonth,
  );
  final picked = await showDialog<DateTime>(
    context: context,
    builder: (context) => _MonthOnlyPickerDialog(
      initial: initial,
      first: bounds.firstMonth,
      last: bounds.lastMonth,
    ),
  );
  if (picked != null) {
    ref.read(reportPeriodProvider.notifier).useMonth(picked);
  }
}

/* Year wise → year only. */
Future<void> pickReportYear(BuildContext context, WidgetRef ref) async {
  final period = ref.read(reportPeriodProvider);
  final bounds = await loadReportPickerBounds();
  if (!context.mounted) return;
  final initial = bounds.clampYear(
    period.kind == ReportPeriodKind.year && period.day != null
        ? period.day!
        : DateTime(bounds.lastYear),
  );
  final picked = await showDialog<DateTime>(
    context: context,
    builder: (context) => _YearOnlyPickerDialog(
      initial: initial,
      firstYear: bounds.firstYear,
      lastYear: bounds.lastYear,
    ),
  );
  if (picked != null) {
    ref.read(reportPeriodProvider.notifier).useYear(picked);
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

/* Applies day/month/year filter selection with the right picker. */
Future<void> applyReportPeriodFilterChoice(
  BuildContext context,
  WidgetRef ref,
  String selected,
) async {
  switch (selected) {
    case 'day':
      await pickReportDay(context, ref);
    case 'month':
      await pickReportMonth(context, ref);
    case 'year':
      await pickReportYear(context, ref);
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
              Icons.calendar_today_rounded,
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
              Icons.calendar_view_month_rounded,
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
  if (selected == 'day' || selected == 'month' || selected == 'year') {
    await applyReportPeriodFilterChoice(context, ref, selected);
    return;
  }
  if (onExtra != null) {
    await onExtra(selected);
  }
}

class _MonthOnlyPickerDialog extends StatefulWidget {
  const _MonthOnlyPickerDialog({
    required this.initial,
    required this.first,
    required this.last,
  });

  final DateTime initial;
  final DateTime first;
  final DateTime last;

  @override
  State<_MonthOnlyPickerDialog> createState() => _MonthOnlyPickerDialogState();
}

class _MonthOnlyPickerDialogState extends State<_MonthOnlyPickerDialog> {
  late int year;

  @override
  void initState() {
    super.initState();
    year = widget.initial.year;
  }

  bool _monthEnabled(int month) {
    final candidate = DateTime(year, month);
    final first = DateTime(widget.first.year, widget.first.month);
    final last = DateTime(widget.last.year, widget.last.month);
    return !candidate.isBefore(first) && !candidate.isAfter(last);
  }

  @override
  Widget build(BuildContext context) {
    final months = DateFormat().dateSymbols.SHORTMONTHS;
    final screenW = MediaQuery.sizeOf(context).width;
    return AlertDialog(
      insetPadding: EdgeInsets.symmetric(
        horizontal: screenW < 360 ? 12 : 24,
        vertical: 24,
      ),
      title: const Text('Select month'),
      content: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: (screenW - 48).clamp(240.0, 360.0)),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                IconButton(
                  tooltip: 'Previous year',
                  onPressed: year <= widget.first.year
                      ? null
                      : () => setState(() => year--),
                  icon: const Icon(Icons.chevron_left_rounded),
                ),
                Expanded(
                  child: Text(
                    '$year',
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                      fontSize: 18,
                      color: AppColors.navy,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: 'Next year',
                  onPressed: year >= widget.last.year
                      ? null
                      : () => setState(() => year++),
                  icon: const Icon(Icons.chevron_right_rounded),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LayoutBuilder(
              builder: (context, constraints) {
                final cols = AppBreakpoints.columnsForWidth(
                  constraints.maxWidth,
                  minItemWidth: 72,
                  minColumns: 2,
                  maxColumns: 4,
                  spacing: 8,
                );
                return GridView.builder(
                  shrinkWrap: true,
                  itemCount: 12,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: cols,
                    mainAxisSpacing: 8,
                    crossAxisSpacing: 8,
                    childAspectRatio: cols >= 4 ? 2.0 : 2.2,
                  ),
                  itemBuilder: (context, index) {
                    final month = index + 1;
                    final enabled = _monthEnabled(month);
                    final selected =
                        year == widget.initial.year &&
                        month == widget.initial.month;
                    return Material(
                      color: selected
                          ? AppColors.primary
                          : enabled
                          ? AppColors.primarySoft
                          : AppColors.border.withValues(alpha: 0.35),
                      borderRadius: BorderRadius.circular(10),
                      child: InkWell(
                        borderRadius: BorderRadius.circular(10),
                        onTap: !enabled
                            ? null
                            : () => Navigator.pop(
                                context,
                                DateTime(year, month, 1),
                              ),
                        child: Center(
                          child: Text(
                            months[index],
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              fontWeight: FontWeight.w700,
                              color: !enabled
                                  ? AppColors.textSecondary
                                  : selected
                                  ? Colors.white
                                  : AppColors.navy,
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
      ],
    );
  }
}

class _YearOnlyPickerDialog extends StatelessWidget {
  const _YearOnlyPickerDialog({
    required this.initial,
    required this.firstYear,
    required this.lastYear,
  });

  final DateTime initial;
  final int firstYear;
  final int lastYear;

  @override
  Widget build(BuildContext context) {
    final screenW = MediaQuery.sizeOf(context).width;
    return AlertDialog(
      insetPadding: EdgeInsets.symmetric(
        horizontal: screenW < 360 ? 12 : 24,
        vertical: 24,
      ),
      title: const Text('Select year'),
      content: SizedBox(
        width: (screenW - 48).clamp(240.0, 300.0),
        height: 300,
        child: YearPicker(
          firstDate: DateTime(firstYear),
          lastDate: DateTime(lastYear),
          selectedDate: initial,
          onChanged: (date) => Navigator.pop(context, DateTime(date.year)),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
      ],
    );
  }
}
