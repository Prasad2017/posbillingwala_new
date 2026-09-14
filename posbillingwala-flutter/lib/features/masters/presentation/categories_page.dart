import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

class CategoriesPage extends ConsumerStatefulWidget {
  const CategoriesPage({super.key});

  @override
  ConsumerState<CategoriesPage> createState() => _CategoriesPageState();
}

class _CategoriesPageState extends ConsumerState<CategoriesPage> {
  final _nameCtrl = TextEditingController();
  FoodType? _foodType;
  bool _busy = false;

  @override
  void dispose() {
    _nameCtrl.dispose();
    super.dispose();
  }

  FoodType? _matchFoodType(List<FoodType> types, int? id) {
    if (id == null) return types.isEmpty ? null : types.first;
    for (final t in types) {
      if (t.foodTypeId == id) return t;
    }
    return types.isEmpty ? null : types.first;
  }

  Future<void> _add() async {
    final name = _nameCtrl.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter category name')),
      );
      return;
    }
    final foodTypes = ref.read(foodTypesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <FoodType>[],
        );
    final selected = _foodType ?? (foodTypes.isEmpty ? null : foodTypes.first);
    setState(() => _busy = true);
    try {
      await ref.read(mastersSyncControllerProvider.notifier).createCategory(
            name,
            foodTypeId: selected?.foodTypeId,
            foodTypeCode: selected?.foodTypeCode,
          );
      _nameCtrl.clear();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Category saved')),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _edit(ProductCategory category) async {
    final controller = TextEditingController(text: category.categoryName);
    final foodTypes = ref.read(foodTypesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <FoodType>[],
        );
    var selected = _matchFoodType(foodTypes, category.foodTypeId);
    final ok = await showAppBottomSheet<bool>(
      context: context,
      title: 'Edit Category',
      child: StatefulBuilder(
        builder: (context, setLocal) => Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            MasterOutlinedField(controller: controller, hint: 'Category Name'),
            if (foodTypes.isNotEmpty) ...[
              const SizedBox(height: 12),
              AppDropdownFormField<FoodType>(
                label: 'Food type',
                items: foodTypes,
                itemLabel: (f) => f.foodTypeName,
                value: selected,
                onChanged: (v) => setLocal(() => selected = v),
              ),
            ],
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: TextButton(
                    onPressed: () => Navigator.pop(context, false),
                    child: const Text('Cancel'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: MasterPrimaryButton(
                    label: 'Save',
                    onPressed: () => Navigator.pop(context, true),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
    final name = controller.text.trim();
    controller.dispose();
    if (ok != true || name.isEmpty) return;
    await ref.read(mastersSyncControllerProvider.notifier).updateCategory(
          categoryId: category.categoryId,
          name: name,
          foodTypeId: selected?.foodTypeId,
          foodTypeCode: selected?.foodTypeCode,
        );
  }

  Future<void> _delete(ProductCategory category) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Delete category',
      message: 'Remove ${category.categoryName}?',
      confirmLabel: 'Delete',
      confirmVariant: AppButtonVariant.danger,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok) return;
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .deleteCategory(category.categoryId);
  }

  @override
  Widget build(BuildContext context) {
    final rowsAsync = ref.watch(categoriesProvider);
    final foodTypes = ref.watch(foodTypesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <FoodType>[],
        );
    final selected = _foodType ?? (foodTypes.isEmpty ? null : foodTypes.first);

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: Text(AppStrings.of(ref).categoryList),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            28),
        children: [
          const MasterSectionLabel('Category Detail'),
          const SizedBox(height: 10),
          MasterCard(
            child: Column(
              children: [
                MasterOutlinedField(
                  controller: _nameCtrl,
                  hint: 'Category Name',
                ),
                if (foodTypes.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  AppDropdownFormField<FoodType>(
                    label: 'Food type',
                    items: foodTypes,
                    itemLabel: (f) => f.foodTypeName,
                    value: selected,
                    onChanged: (v) => setState(() => _foodType = v),
                  ),
                ],
                const SizedBox(height: 12),
                MasterPrimaryButton(
                  label: 'Add Category',
                  isLoading: _busy,
                  onPressed: _busy ? null : _add,
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const MasterSectionLabel('Category List'),
          const SizedBox(height: 10),
          MasterCard(
            padding: EdgeInsets.zero,
            child: rowsAsync.when(
              data: (rows) {
                if (rows.isEmpty) {
                  return const MasterEmptyState(
                    title: 'No data found',
                    subtitle: 'Add categories like Veg / Non Veg.',
                  );
                }
                return Column(
                  children: [
                    for (var i = 0; i < rows.length; i++)
                      MasterListRow(
                        index: i + 1,
                        title: rows[i].categoryName,
                        onEdit: () => _edit(rows[i]),
                        onDelete: () => _delete(rows[i]),
                        showDivider: i < rows.length - 1,
                      ),
                  ],
                );
              },
              loading: () => const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              ),
              error: (e, _) => Padding(
                padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
                child: Text('$e'),
              ),
            ),
          ),
        ],
      ),
      ),
    );
  }
}
