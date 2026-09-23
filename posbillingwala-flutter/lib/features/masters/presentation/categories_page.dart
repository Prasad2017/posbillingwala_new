import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class CategoriesPage extends ConsumerStatefulWidget {
  const CategoriesPage({super.key});

  @override
  ConsumerState<CategoriesPage> createState() => CategoriesPageState();
}

class CategoriesPageState extends ConsumerState<CategoriesPage> {
  final nameCtrl = TextEditingController();
  bool busy = false;

  @override
  void dispose() {
    nameCtrl.dispose();
    super.dispose();
  }

  Future<void> add() async {
    final name = nameCtrl.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(AppStrings.of(ref).enterCategoryName)),
      );
      return;
    }
    setState(() => busy = true);
    try {
      await ref
          .read(mastersSyncControllerProvider.notifier)
          .createCategory(name);
      nameCtrl.clear();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(AppStrings.of(ref).categorySaved)));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> edit(ProductCategory category) async {
    final controller = TextEditingController(text: category.categoryName);
    final ok = await showAppBottomSheet<bool>(
      context: context,
      title: 'Edit Category',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          MasterOutlinedField(
            required: true,
            controller: controller,
            hint: 'Category Name',
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
    );
    final name = controller.text.trim();
    controller.dispose();
    if (ok != true || name.isEmpty) return;
    await ref
        .read(mastersSyncControllerProvider.notifier)
        .updateCategory(categoryId: category.categoryId, name: name);
  }

  Future<void> delete(ProductCategory category) async {
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
            28,
          ),
          children: [
            ResponsiveMasterSplit(
              form: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const MasterSectionLabel('Category Detail'),
                  const SizedBox(height: 10),
                  MasterCard(
                    child: Column(
                      children: [
                        MasterOutlinedField(
                          required: true,
                          controller: nameCtrl,
                          hint: 'Category Name',
                        ),
                        const SizedBox(height: 12),
                        MasterPrimaryButton(
                          label: 'Add Category',
                          isLoading: busy,
                          onPressed: busy ? null : add,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              list: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const MasterSectionLabel('Category List'),
                  const SizedBox(height: 10),
                  MasterCard(
                    padding: EdgeInsets.zero,
                    child: rowsAsync.when(
                      data: (rows) {
                        if (rows.isEmpty) {
                          return const MasterEmptyState(
                            title: 'No data found',
                            subtitle: 'Add your first category.',
                          );
                        }
                        return Column(
                          children: [
                            for (var i = 0; i < rows.length; i++)
                              MasterListRow(
                                index: i + 1,
                                title: rows[i].categoryName,
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
          ],
        ),
      ),
    );
  }
}
