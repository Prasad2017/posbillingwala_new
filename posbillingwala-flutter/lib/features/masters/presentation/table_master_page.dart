import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

class TableMasterPage extends ConsumerStatefulWidget {
  const TableMasterPage({super.key});

  @override
  ConsumerState<TableMasterPage> createState() => _TableMasterPageState();
}

class _TableMasterPageState extends ConsumerState<TableMasterPage> {
  int _tab = 0;
  bool _busy = false;

  Future<void> _addArea({DiningArea? existing}) async {
    final name = TextEditingController(text: existing?.areaName ?? '');
    final ok = await showAppBottomSheet<bool>(
      context: context,
      title: existing == null ? 'Add Area' : 'Edit Area',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          MasterOutlinedField(controller: name, hint: 'Area name'),
          const SizedBox(height: 18),
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
    final value = name.text.trim();
    name.dispose();
    if (ok != true || value.isEmpty) return;

    setState(() => _busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      if (existing != null) {
        await db.updateLocalDiningArea(
          areaId: existing.areaId,
          areaName: value,
        );
      } else {
        final userId = ref.read(authControllerProvider).session?.userId ?? '';
        await ref.read(mastersRepositoryProvider).createDiningArea(
              userId: userId,
              areaName: value,
            );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _removeArea(DiningArea area) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Remove area',
      message: 'Remove ${area.areaName}?',
      confirmLabel: 'Remove',
      confirmVariant: AppButtonVariant.danger,
    );
    if (!ok) return;
    await ref.read(appDatabaseProvider).deactivateDiningArea(area.areaId);
  }

  Future<void> _addType({TableType? existing}) async {
    final name = TextEditingController(text: existing?.tableTypeName ?? '');
    final seats = TextEditingController(
      text: _seatsFromTypeName(existing?.tableTypeName ?? '') ?? '4',
    );
    final ok = await showAppBottomSheet<bool>(
      context: context,
      title: existing == null ? 'Add Table Type' : 'Edit Table Type',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          MasterOutlinedField(controller: name, hint: 'Type name'),
          const SizedBox(height: 12),
          MasterOutlinedField(
            controller: seats,
            hint: 'Default seats',
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 18),
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
    final typeName = name.text.trim();
    name.dispose();
    seats.dispose();
    if (ok != true || typeName.isEmpty) return;

    setState(() => _busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      if (existing != null) {
        await db.updateLocalTableType(
          tableTypeId: existing.tableTypeId,
          tableTypeName: typeName,
        );
      } else {
        final userId = ref.read(authControllerProvider).session?.userId ?? '';
        await ref.read(mastersRepositoryProvider).createTableType(
              userId: userId,
              tableTypeName: typeName,
            );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _removeType(TableType type) async {
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Remove type',
      message: 'Remove ${type.tableTypeName}?',
      confirmLabel: 'Remove',
      confirmVariant: AppButtonVariant.danger,
    );
    if (!ok) return;
    await ref.read(appDatabaseProvider).deactivateTableType(type.tableTypeId);
  }

  Future<void> _addTable({PosTable? existing}) async {
    final areas = ref.read(diningAreasProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <DiningArea>[],
        );
    final types = ref.read(tableTypesProvider).maybeWhen(
          data: (v) => v,
          orElse: () => const <TableType>[],
        );
    final floor = ref.read(floorTablesProvider);
    final nextNo = existing?.tableNumber ?? '${floor.length + 1}';

    final number = TextEditingController(text: nextNo);
    final name = TextEditingController(
      text: existing?.displayName.isNotEmpty == true
          ? existing!.displayName
          : 'T$nextNo',
    );
    final seats = TextEditingController(
      text: '${existing?.capacity ?? 2}',
    );
    DiningArea? area;
    if (existing?.areaId != null) {
      for (final a in areas) {
        if (a.areaId == existing!.areaId) {
          area = a;
          break;
        }
      }
    }
    area ??= areas.isNotEmpty ? areas.first : null;
    TableType? type = types.isNotEmpty ? types.first : null;
    if (existing != null && types.isNotEmpty) {
      for (final t in types) {
        final seatsHint = _seatsFromTypeName(t.tableTypeName);
        if (seatsHint != null && seatsHint == '${existing.capacity}') {
          type = t;
          break;
        }
      }
    }

    final ok = await showAppBottomSheet<bool>(
      context: context,
      title: existing == null ? 'Add Table' : 'Edit Table',
      child: StatefulBuilder(
        builder: (context, setLocal) => Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            MasterOutlinedField(
              controller: number,
              hint: 'Table No',
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 12),
            MasterOutlinedField(controller: name, hint: 'Table Name'),
            const SizedBox(height: 12),
            MasterOutlinedField(
              controller: seats,
              hint: 'Seats',
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 12),
            MasterDropdown<DiningArea>(
              value: area,
              items: areas,
              hint: 'Area',
              itemLabel: (a) => a.areaName,
              onChanged: (v) => setLocal(() => area = v),
            ),
            const SizedBox(height: 12),
            MasterDropdown<TableType>(
              value: type,
              items: types,
              hint: 'Table Type',
              itemLabel: (t) {
                final s = _seatsFromTypeName(t.tableTypeName);
                return s == null
                    ? t.tableTypeName
                    : '${t.tableTypeName} ($s seats)';
              },
              onChanged: (v) {
                setLocal(() {
                  type = v;
                  final s = _seatsFromTypeName(v?.tableTypeName ?? '');
                  if (s != null) seats.text = s;
                });
              },
            ),
            const SizedBox(height: 18),
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

    final tableNo = number.text.trim();
    final display = name.text.trim().isEmpty
        ? 'T$tableNo'
        : name.text.trim();
    final capacity = int.tryParse(seats.text.trim()) ?? 2;
    number.dispose();
    name.dispose();
    seats.dispose();
    if (ok != true || tableNo.isEmpty) return;

    setState(() => _busy = true);
    try {
      final userId = ref.read(authControllerProvider).session?.userId ?? '';
      final repo = ref.read(mastersRepositoryProvider);
      if (existing != null) {
        await repo.updatePosTable(
          userId: userId,
          tableId: existing.tableId,
          tableNumber: tableNo,
          displayName: display,
          capacity: capacity,
          areaId: area?.areaId,
        );
      } else {
        await repo.createPosTable(
          userId: userId,
          tableNumber: tableNo,
          displayName: display,
          capacity: capacity,
          areaId: area?.areaId,
        );
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(existing == null ? 'Table saved' : 'Table updated'),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _removeTable(PosTable table) async {
    final label = table.displayName.isEmpty
        ? 'Table ${table.tableNumber}'
        : table.displayName;
    final ok = await showAppConfirmBottomSheet(
      context: context,
      title: 'Remove table',
      message: 'Remove $label?',
      confirmLabel: 'Remove',
      confirmVariant: AppButtonVariant.danger,
    );
    if (!ok) return;
    await ref.read(appDatabaseProvider).deactivatePosTable(table.tableId);
  }

  String? _seatsFromTypeName(String name) {
    final match = RegExp(r'(\d+)').firstMatch(name);
    return match?.group(1);
  }

  String _typeSubtitle(TableType type) {
    final seats = _seatsFromTypeName(type.tableTypeName);
    return seats == null ? 'Seating type' : '$seats seats default';
  }

  String _tableSubtitle(PosTable table, Map<int, String> areas) {
    final area = table.areaId == null ? null : areas[table.areaId!];
    final parts = <String>[
      if (area != null && area.isNotEmpty) area,
      '${table.capacity} seats',
    ];
    return parts.join(' · ');
  }

  @override
  Widget build(BuildContext context) {
    final areas = ref.watch(diningAreasProvider);
    final types = ref.watch(tableTypesProvider);
    final floor = ref.watch(floorTablesProvider);
    final areaNames = areas.maybeWhen(
      data: (rows) => {for (final a in rows) a.areaId: a.areaName},
      orElse: () => const <int, String>{},
    );

    final hints = [
      'Create areas like Hall, AC, Non-AC, Garden',
      'Create seating types like 2 Seater / 4 Seater',
      'Using local table count: ${floor.length}',
    ];
    final addLabels = ['+ Add Area', '+ Add Type', '+ Add Table'];

    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        title: const Text('Table Master'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
            child: MasterPillTabs(
              labels: const ['Areas', 'Types', 'Tables'],
              index: _tab,
              onChanged: (i) => setState(() => _tab = i),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(
                hints[_tab],
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 12.5,
                  color: AppColors.navy.withValues(alpha: .48),
                ),
              ),
            ),
          ),
          Expanded(
            child: IndexedStack(
              index: _tab,
              children: [
                areas.when(
                  data: (rows) => rows.isEmpty
                      ? const Center(
                          child: MasterEmptyState(
                            title: 'No areas yet',
                            subtitle: 'Add Hall, AC, Garden and more.',
                          ),
                        )
                      : ResponsiveScrollShell(
        dashboard: true,
        child: ListView.separated(
                          padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            0,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16),
                          itemCount: rows.length,
                          separatorBuilder: (_, _) =>
                              const SizedBox(height: 10),
                          itemBuilder: (context, i) {
                            final a = rows[i];
                            return _MasterEntityCard(
                              title: a.areaName,
                              subtitle: 'Floor area filter',
                              onEdit: () => _addArea(existing: a),
                              onRemove: () => _removeArea(a),
                            );
                          },
                        ),
      ),
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Center(child: Text('$e')),
                ),
                types.when(
                  data: (rows) => rows.isEmpty
                      ? const Center(
                          child: MasterEmptyState(
                            title: 'No types yet',
                            subtitle: 'Add 2 Seater / 4 Seater types.',
                          ),
                        )
                      : ResponsiveScrollShell(
        dashboard: true,
        child: ListView.separated(
                          padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            0,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16),
                          itemCount: rows.length,
                          separatorBuilder: (_, _) =>
                              const SizedBox(height: 10),
                          itemBuilder: (context, i) {
                            final t = rows[i];
                            return _MasterEntityCard(
                              title: t.tableTypeName,
                              subtitle: _typeSubtitle(t),
                              onEdit: () => _addType(existing: t),
                              onRemove: () => _removeType(t),
                            );
                          },
                        ),
      ),
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Center(child: Text('$e')),
                ),
                floor.isEmpty
                    ? const Center(
                        child: MasterEmptyState(
                          title: 'No tables yet',
                          subtitle: 'Add tables with number and seats.',
                        ),
                      )
                    : ResponsiveScrollShell(
        dashboard: true,
        child: ListView.separated(
                        padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            0,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16),
                        itemCount: floor.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 10),
                        itemBuilder: (context, i) {
                          final t = floor[i].table;
                          final title = t.displayName.isEmpty
                              ? 'T${t.tableNumber}'
                              : '${t.displayName} · No ${t.tableNumber}';
                          return _MasterEntityCard(
                            title: title,
                            subtitle: _tableSubtitle(t, areaNames),
                            onEdit: () => _addTable(existing: t),
                            onRemove: () => _removeTable(t),
                          );
                        },
                      ),
      ),
              ],
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              child: SizedBox(
                height: 52,
                child: FilledButton(
                  onPressed: _busy
                      ? null
                      : () {
                          switch (_tab) {
                            case 0:
                              _addArea();
                            case 1:
                              _addType();
                            default:
                              _addTable();
                          }
                        },
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(28),
                    ),
                  ),
                  child: Text(
                    addLabels[_tab],
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w700,
                      fontSize: 15.5,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MasterEntityCard extends StatelessWidget {
  const _MasterEntityCard({
    required this.title,
    required this.subtitle,
    required this.onEdit,
    required this.onRemove,
  });

  final String title;
  final String subtitle;
  final VoidCallback onEdit;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return MasterCard(
      padding: const EdgeInsets.fromLTRB(14, 12, 8, 12),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: AppFonts.family,
                    fontWeight: FontWeight.w700,
                    fontSize: 15,
                    color: AppColors.navy,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  subtitle,
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    fontSize: 12.5,
                    color: AppColors.navy.withValues(alpha: .45),
                  ),
                ),
              ],
            ),
          ),
          TextButton(
            onPressed: onEdit,
            child: const Text(
              'Edit',
              style: TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.primary,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          TextButton(
            onPressed: onRemove,
            child: const Text(
              'Remove',
              style: TextStyle(
                fontFamily: AppFonts.family,
                color: AppColors.red,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
