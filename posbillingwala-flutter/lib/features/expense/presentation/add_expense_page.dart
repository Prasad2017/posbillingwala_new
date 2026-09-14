import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

/* Full-page Add Expense form matching the reference layout. */
class AddExpensePage extends ConsumerStatefulWidget {
  const AddExpensePage({super.key});

  @override
  ConsumerState<AddExpensePage> createState() => AddExpensePageState();
}

class AddExpensePageState extends ConsumerState<AddExpensePage> {
  final nameCtrl = TextEditingController();
  final amountCtrl = TextEditingController();
  final formKey = GlobalKey<FormState>();
  var submitting = false;

  @override
  void dispose() {
    nameCtrl.dispose();
    amountCtrl.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    final name = nameCtrl.text.trim();
    final amount = double.tryParse(amountCtrl.text.trim()) ?? 0;

    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter expense name')),
      );
      return;
    }
    if (amount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid amount')),
      );
      return;
    }

    setState(() => submitting = true);
    await ref.read(inventoryControllerProvider.notifier).addExpense(
          name: name,
          amount: amount,
        );
    if (!mounted) return;
    setState(() => submitting = false);

    final result = ref.read(inventoryControllerProvider);
    result.whenOrNull(
      data: (_) => context.pop(true),
      error: (e, _) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e')),
        );
      },
    );
  }

  InputDecoration fieldDecoration(String hint) {
    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(
        fontFamily: AppFonts.family,
        color: AppColors.navy.withValues(alpha: .38),
        fontWeight: FontWeight.w400,
        fontSize: 15,
      ),
      filled: true,
      fillColor: Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 16),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(4),
        borderSide: BorderSide(color: AppColors.navy.withValues(alpha: .45)),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(4),
        borderSide: BorderSide(color: AppColors.navy.withValues(alpha: .45)),
      ),
      focusedBorder: const OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(4)),
        borderSide: BorderSide(color: AppColors.primary, width: 1.5),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(''),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: Form(
        key: formKey,
        child: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
          padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            20,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            24),
          children: [
            TextField(
              controller: nameCtrl,
              textCapitalization: TextCapitalization.sentences,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 15,
                color: AppColors.navy,
                fontWeight: FontWeight.w500,
              ),
              decoration: fieldDecoration('Expenses Name'),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: amountCtrl,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [
                FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
              ],
              style: const TextStyle(
                fontFamily: AppFonts.family,
                fontSize: 15,
                color: AppColors.navy,
                fontWeight: FontWeight.w500,
              ),
              decoration: fieldDecoration('Expenses Amount'),
            ),
          ],
        ),
      ),
      ),
      bottomNavigationBar: SafeArea(
        top: false,
        child: SizedBox(
          width: double.infinity,
          height: 52,
          child: Material(
            color: AppColors.primary,
            child: InkWell(
              onTap: submitting ? null : submit,
              child: Center(
                child: submitting
                    ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.2,
                          color: Colors.white,
                        ),
                      )
                    : const Text(
                        'ADD EXPENSE',
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w700,
                          fontSize: 15,
                          letterSpacing: 0.4,
                          color: Colors.white,
                        ),
                      ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
