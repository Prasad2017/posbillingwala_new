import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';

class SubcategoriesPage extends ConsumerStatefulWidget {
  const SubcategoriesPage({super.key});

  @override
  ConsumerState<SubcategoriesPage> createState() => SubcategoriesPageState();
}

class SubcategoriesPageState extends ConsumerState<SubcategoriesPage> {
  final subcategoriesPageNameCtrl = TextEditingController();
  ProductCategory? selectedCategory;
  bool busy = false;

  @override
  void dispose() {
    subcategoriesPageNameCtrl.dispose();
    super.dispose();
  }

  Future<void> add() async {
    final categories = ref
        .read(categoriesProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <ProductCategory>[]);
    if (categories.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Add categories first')));
      return;
    }
    final selected = selectedCategory ?? categories.first;
    final name = subcategoriesPageNameCtrl.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Enter subcategory name')));
      return;
    }
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    setState(() => busy = true);
    try {
      await ref
          .read(mastersRepositoryProvider)
          .createSubcategory(
            userId: userId,
            subcategoryName: name,
            categoryId: selected.categoryId,
            categoryNetworkStatus: selected.categoryNetworkStatus,
          );
      subcategoriesPageNameCtrl.clear();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Subcategory saved')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> edit(ProductSubcategory row) async {
    final categories = ref
        .read(categoriesProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <ProductCategory>[]);
    if (categories.isEmpty) return;
    var selected = categories.firstWhere(
      (c) => c.categoryId == row.categoryId,
      orElse: () => categories.first,
    );
    final nameCtrl = TextEditingController(text: row.subcategoryName);
    final ok = await showAppBottomSheet<bool>(
      context: context,
      title: 'Edit Subcategory',
      child: StatefulBuilder(
        builder: (context, setLocal) => Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            MasterDropdown<ProductCategory>(
              value: selected,
              items: categories,
              itemLabel: (c) => c.categoryName,
              onChanged: (v) {
                if (v != null) setLocal(() => selected = v);
              },
            ),
            const SizedBox(height: 12),
            MasterOutlinedField(
              required: true,
              controller: nameCtrl,
              hint: 'Subcategory Name',
            ),
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
    final name = nameCtrl.text.trim();
    nameCtrl.dispose();
    if (ok != true || name.isEmpty) return;
    await ref
        .read(appDatabaseProvider)
        .updateLocalSubcategory(
          subcategoryId: row.subcategoryId,
          subcategoryName: name,
          categoryId: selected.categoryId,
        );
  }

  Future<void> delete(ProductSubcategory row) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Delete subcategory',
      message: 'Remove ${row.subcategoryName}?',
      confirmLabel: 'Delete',
      confirmVariant: AppButtonVariant.danger,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok) return;
    await ref
        .read(appDatabaseProvider)
        .softDeleteSubcategory(row.subcategoryId);
  }

  @override
  Widget build(BuildContext context) {
    final rowsAsync = ref.watch(subcategoriesProvider);
    final categories = ref
        .watch(categoriesProvider)
        .maybeWhen(data: (v) => v, orElse: () => const <ProductCategory>[]);
    final categoryNames = {
      for (final c in categories) c.categoryId: c.categoryName,
    };
    final selected =
        selectedCategory != null &&
            categories.any((c) => c.categoryId == selectedCategory!.categoryId)
        ? categories.firstWhere(
            (c) => c.categoryId == selectedCategory!.categoryId,
          )
        : (categories.isNotEmpty ? categories.first : null);

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: const Text('Subcategory List'),
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
            28,
          ),
          children: [
            const MasterSectionLabel('Subcategory Detail'),
            const SizedBox(height: 10),
            MasterCard(
              child: Column(
                children: [
                  MasterDropdown<ProductCategory>(
                    required: true,
                    value: selected,
                    items: categories,
                    hint: 'Select category',
                    itemLabel: (c) => c.categoryName,
                    onChanged: (v) => setState(() => selectedCategory = v),
                  ),
                  const SizedBox(height: 12),
                  MasterOutlinedField(
                    required: true,
                    controller: subcategoriesPageNameCtrl,
                    hint: 'Subcategory Name',
                  ),
                  const SizedBox(height: 12),
                  MasterPrimaryButton(
                    label: 'Add Subcategory',
                    isLoading: busy,
                    onPressed: busy ? null : add,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const MasterSectionLabel('Subcategory List'),
            const SizedBox(height: 10),
            MasterCard(
              padding: EdgeInsets.zero,
              child: rowsAsync.when(
                data: (rows) {
                  if (rows.isEmpty) {
                    return const MasterEmptyState(
                      title: 'No data found',
                      subtitle: 'Add subcategories like Tea / Juice.',
                    );
                  }
                  return Column(
                    children: [
                      for (var i = 0; i < rows.length; i++)
                        MasterListRow(
                          index: i + 1,
                          title: rows[i].subcategoryName,
                          subtitle: categoryNames[rows[i].categoryId],
                          onEdit: () => edit(rows[i]),
                          onDelete: () => delete(rows[i]),
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
