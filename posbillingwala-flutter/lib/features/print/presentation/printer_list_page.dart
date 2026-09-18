import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class PrinterListPage extends ConsumerStatefulWidget {
  const PrinterListPage({super.key});

  @override
  ConsumerState<PrinterListPage> createState() => PrinterListPageState();
}

class PrinterListPageState extends ConsumerState<PrinterListPage> {
  List<StorePrinter> printers = const [];
  bool loading = true;
  String? error;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    setState(() => loading = true);
    try {
      final cached = await CloudScreenCache.loadMapList(
        CloudScreenCache.storePrinters,
      );
      if (cached.isNotEmpty && mounted) {
        setState(() {
          printers = cached.map(StorePrinter.fromJson).toList();
          loading = false;
          error = null;
        });
      }
      final list = await ref
          .read(storePrinterApiProvider)
          .list(session.licenceUserId);
      if (!mounted) return;
      setState(() {
        printers = list;
        loading = false;
        error = null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        error = e.toString().replaceFirst('Exception: ', '');
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final canManage = ref
        .watch(permissionControllerProvider)
        .allows('printer.manage');
    return Scaffold(
      appBar: AppBar(
        title: const Text('Extra printers'),
        actions: [
          IconButton(
            onPressed: () => context.push('/settings/print-queue'),
            icon: const Icon(Icons.queue),
          ),
          IconButton(
            onPressed: () => context.push('/settings/printer-routing'),
            icon: const Icon(Icons.alt_route),
          ),
        ],
      ),
      floatingActionButton: canManage
          ? FloatingActionButton(
              onPressed: () async {
                await context.push('/settings/printers/add');
                await load();
              },
              child: const Icon(Icons.add),
            )
          : null,
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : error != null
          ? Center(child: Text(error!))
          : RefreshIndicator(
              onRefresh: load,
              child: ListView.builder(
                itemCount: printers.isEmpty ? 1 : printers.length,
                itemBuilder: (context, index) {
                  if (printers.isEmpty) {
                    return const Padding(
                      padding: EdgeInsets.all(24),
                      child: Text(
                        'No extra printers yet. Tap + to add a kitchen or packing printer.',
                        textAlign: TextAlign.center,
                      ),
                    );
                  }
                  final printer = printers[index];
                  return ListTile(
                    title: Text(printer.printerName),
                    subtitle: Text(
                      '${printer.connectionLabel} · ${printer.paperSizeLabel} · ${printer.purpose} · ${printer.area}',
                    ),
                    trailing: printer.enabled ? null : const Text('Off'),
                    onTap: () async {
                      await context.push(
                        '/settings/printers/edit',
                        extra: printer,
                      );
                      await load();
                    },
                  );
                },
              ),
            ),
    );
  }
}
