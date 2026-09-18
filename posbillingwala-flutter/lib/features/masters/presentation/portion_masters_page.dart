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

class PortionMastersPage extends ConsumerStatefulWidget {
  const PortionMastersPage({super.key});

  @override
  ConsumerState<PortionMastersPage> createState() => PortionMastersPageState();
}

class PortionMastersPageState extends ConsumerState<PortionMastersPage> {
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
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Enter portion name')));
      return;
    }
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    setState(() => busy = true);
    try {
      await ref
          .read(mastersRepositoryProvider)
          .createPortionMaster(userId: userId, portionName: name);
      nameCtrl.clear();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Portion master saved')));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> edit(PortionMaster row) async {
    final controller = TextEditingController(text: row.portionName);
    final ok = await showAppBottomSheet<bool>(
      context: context,
      title: 'Edit Portion',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          MasterOutlinedField(
            required: true,
            controller: controller,
            hint: 'Portion Name',
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
        .read(appDatabaseProvider)
        .updateLocalPortionMaster(
          portionMasterId: row.portionMasterId,
          portionName: name,
        );
  }

  Future<void> delete(PortionMaster row) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Delete portion',
      message: 'Remove ${row.portionName}?',
      confirmLabel: 'Delete',
      confirmVariant: AppButtonVariant.danger,
      icon: Icons.delete_outline_rounded,
    );
    if (!ok) return;
    await ref
        .read(mastersRepositoryProvider)
        .deletePortionMaster(
          userId:
              ref.read(authControllerProvider).session?.catalogOwnerId ?? '',
          portionMasterId: row.portionMasterId,
        );
  }

  @override
  Widget build(BuildContext context) {
    final list = ref.watch(portionMastersProvider);

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: const Text('Portion Master List'),
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
            const MasterSectionLabel('Portion Master Detail'),
            const SizedBox(height: 10),
            MasterCard(
              child: Column(
                children: [
                  MasterOutlinedField(
                    required: true,
                    controller: nameCtrl,
                    hint: 'Portion Name',
                  ),
                  const SizedBox(height: 12),
                  MasterPrimaryButton(
                    label: 'Add Portion',
                    isLoading: busy,
                    onPressed: busy ? null : add,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const MasterSectionLabel('Portion Master List'),
            const SizedBox(height: 10),
            MasterCard(
              padding: EdgeInsets.zero,
              child: list.when(
                data: (rows) {
                  if (rows.isEmpty) {
                    return const MasterEmptyState(
                      title: 'No data found',
                      subtitle: 'Add portion sizes like Half / Full.',
                    );
                  }
                  return Column(
                    children: [
                      for (var i = 0; i < rows.length; i++)
                        MasterListRow(
                          index: i + 1,
                          title: rows[i].portionName,
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
