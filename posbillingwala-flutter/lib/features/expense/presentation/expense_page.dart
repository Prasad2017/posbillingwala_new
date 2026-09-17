import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Expense management list matching the reference table layout. */
class ExpensePage extends ConsumerWidget {
  const ExpensePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.listen(inventoryControllerProvider, (prev, next) {
      next.whenOrNull(
        data: (msg) {
          if (msg == null) return;
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(msg)));
        },
        error: (e, _) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text('$e')));
        },
      );
    });

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).expense),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 10),
            child: TextButton(
              onPressed: () => context.push('/expenses/add'),
              style: TextButton.styleFrom(
                backgroundColor: Colors.white,
                foregroundColor: AppColors.navy,
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              child: const Text(
                'Add Expense',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w700,
                  fontSize: 13,
                ),
              ),
            ),
          ),
        ],
      ),
      body: const ExpenseListBody(),
    );
  }
}

/* Shared expense table used by Expense Management and Inventory tab. */
class ExpenseListBody extends ConsumerWidget {
  const ExpenseListBody({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final expensesAsync = ref.watch(expensesProvider);
    final total = ref.watch(expensesTotalProvider);
    final currency = NumberFormat.currency(
      locale: 'en_IN',
      symbol: '₹ ',
      decimalDigits: 0,
    );
    final totalCurrency = NumberFormat.currency(
      locale: 'en_IN',
      symbol: '₹ ',
      decimalDigits: 2,
    );
    final dateFmt = DateFormat('yyyy-MM-dd');

    return expensesAsync.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text('$e')),
      data: (rows) {
        return ResponsiveScrollShell(
          dashboard: true,
          child: ListView(
            padding: EdgeInsets.fromLTRB(
              AppBreakpoints.pagePaddingFor(context.widthClass),
              14,
              AppBreakpoints.pagePaddingFor(context.widthClass),
              28,
            ),
            children: [
            MasterCard(
              padding: EdgeInsets.zero,
              child: rows.isEmpty
                  ? const MasterEmptyState(
                      title: 'No expenses yet',
                      subtitle: 'Tap Add Expense to record a shop cost.',
                    )
                  : Column(
                      children: [
                        const ExpenseTableHeader(),
                        const Divider(height: 1, thickness: 1),
                        for (var i = 0; i < rows.length; i++) ...[
                          if (i > 0)
                            Divider(
                              height: 1,
                              thickness: 1,
                              color: AppColors.border.withValues(alpha: .7),
                            ),
                          ExpenseTableRow(
                            index: i + 1,
                            row: rows[i],
                            dateFmt: dateFmt,
                            currency: currency,
                          ),
                        ],
                        Divider(
                          height: 1,
                          thickness: 1,
                          color: AppColors.border.withValues(alpha: .9),
                        ),
                        ExpenseTotalRow(
                          totalLabel: totalCurrency.format(total),
                        ),
                      ],
                    ),
            ),
          ],
          ),
        );
      },
    );
  }
}

class ExpenseTableHeader extends StatelessWidget {
  const ExpenseTableHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFFF0F2F5),
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
      child: const Row(
        children: [
          SizedBox(
            width: 42,
            child: HeaderCell('Sr No.'),
          ),
          Expanded(
            flex: 3,
            child: HeaderCell('Expense Date'),
          ),
          Expanded(
            flex: 4,
            child: HeaderCell('Expense Name'),
          ),
          Expanded(
            flex: 3,
            child: HeaderCell('AMOUNT', align: TextAlign.right),
          ),
        ],
      ),
    );
  }
}

class HeaderCell extends StatelessWidget {
  const HeaderCell(this.label, {super.key, this.align = TextAlign.left});

  final String label;
  final TextAlign align;

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      textAlign: align,
      style: TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 11.5,
        fontWeight: FontWeight.w700,
        color: AppColors.navy.withValues(alpha: .55),
      ),
    );
  }
}

class ExpenseTableRow extends StatelessWidget {
  const ExpenseTableRow({super.key, 
    required this.index,
    required this.row,
    required this.dateFmt,
    required this.currency,
  });

  final int index;
  final ShopExpense row;
  final DateFormat dateFmt;
  final NumberFormat currency;

  @override
  Widget build(BuildContext context) {
    final name =
        row.expensesName.trim().isEmpty ? 'Expense' : row.expensesName.trim();

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 14, 12, 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          SizedBox(
            width: 42,
            child: Text(
              '$index',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 13,
                color: AppColors.navy.withValues(alpha: .45),
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          Expanded(
            flex: 3,
            child: Text(
              dateFmt.format(row.expensesDate),
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 12.5,
                color: AppColors.navy.withValues(alpha: .55),
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          Expanded(
            flex: 4,
            child: Text(
              name,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 13.5,
                fontWeight: FontWeight.w700,
                color: AppColors.navy,
                height: 1.25,
              ),
            ),
          ),
          Expanded(
            flex: 3,
            child: Text(
              currency.format(row.expensesAmount),
              textAlign: TextAlign.right,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 13.5,
                fontWeight: FontWeight.w700,
                color: AppColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class ExpenseTotalRow extends StatelessWidget {
  const ExpenseTotalRow({super.key, required this.totalLabel});

  final String totalLabel;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 16, 12, 16),
      child: Row(
        children: [
          const Expanded(
            child: Text(
              'TOTAL AMOUNT',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 14,
                fontWeight: FontWeight.w800,
                color: AppColors.navy,
              ),
            ),
          ),
          Text(
            totalLabel,
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 15,
              fontWeight: FontWeight.w800,
              color: AppColors.primary,
            ),
          ),
        ],
      ),
    );
  }
}
