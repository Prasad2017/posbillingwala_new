import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* WithTable ComboMaster list. */
class CombosPage extends ConsumerStatefulWidget {
  const CombosPage({super.key});

  @override
  ConsumerState<CombosPage> createState() => CombosPageState();
}

class CombosPageState extends ConsumerState<CombosPage> {
  final search = TextEditingController();
  String query = '';

  @override
  void dispose() {
    search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final combosAsync = ref.watch(combosListProvider);
    final currency = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    final pad = AppBreakpoints.pagePaddingFor(context.widthClass);

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(title: Text(AppStrings.of(ref).comboMaster)),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/masters/combos/form'),
        icon: const Icon(Icons.add),
        label: const Text('Add combo'),
      ),
      body: combosAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (combos) {
          final filtered = combos.where((c) {
            if (query.trim().isEmpty) return true;
            final q = query.trim().toLowerCase();
            return c.comboName.toLowerCase().contains(q) ||
                (c.comboCode?.toLowerCase().contains(q) ?? false);
          }).toList();
          return ResponsiveScrollShell(
            dashboard: true,
            child: Column(
              children: [
                Padding(
                  padding: EdgeInsets.fromLTRB(pad, 12, pad, 8),
                  child: AppTextField(
                    controller: search,
                    label: 'Search combo',
                    onChanged: (v) => setState(() => query = v),
                  ),
                ),
                Expanded(
                  child: filtered.isEmpty
                      ? const Center(child: Text('No combos yet'))
                      : ListView.separated(
                          padding: EdgeInsets.fromLTRB(pad, 0, pad, 88),
                          itemCount: filtered.length,
                          separatorBuilder: (_, _) => const SizedBox(height: 8),
                          itemBuilder: (context, index) {
                            final combo = filtered[index];
                            final price = combo.comboWithGstPrice > 0
                                ? combo.comboWithGstPrice
                                : combo.comboPrice;
                            return AppCard(
                              padding: EdgeInsets.zero,
                              child: ListTile(
                                title: Text(
                                  combo.comboName,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w800,
                                  ),
                                ),
                                subtitle: Text(combo.comboCode ?? ''),
                                trailing: Text(
                                  currency.format(price),
                                  style: const TextStyle(
                                    color: AppColors.primary,
                                    fontWeight: FontWeight.w800,
                                  ),
                                ),
                                onTap: () => context.push(
                                  '/masters/combos/form?id=${combo.comboId}',
                                ),
                                onLongPress: () async {
                                  final ok = await showAppConfirmBottomSheet(
                                    context: context,
                                    title: 'Delete combo',
                                    message: 'Remove ${combo.comboName}?',
                                    confirmLabel: 'Delete',
                                    confirmVariant: AppButtonVariant.danger,
                                  );
                                  if (ok != true) return;
                                  await ref
                                      .read(
                                        mastersSyncControllerProvider.notifier,
                                      )
                                      .deleteCombo(combo.comboId);
                                },
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
