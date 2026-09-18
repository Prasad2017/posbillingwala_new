import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';

class PrinterRoutingPage extends ConsumerStatefulWidget {
  const PrinterRoutingPage({super.key});

  @override
  ConsumerState<PrinterRoutingPage> createState() => PrinterRoutingPageState();
}

class PrinterRoutingPageState extends ConsumerState<PrinterRoutingPage> {
  List<StorePrinter> printers = const [];
  String kitchenId = '';
  String barId = '';
  String billId = '';
  bool loading = true;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    final api = ref.read(storePrinterApiProvider);
    final list = await api.list(session.licenceUserId);
    final routes = await api.routes(session.licenceUserId);
    String idFor(String food, String doc) {
      for (final route in routes) {
        if (route.documentType == doc &&
            route.foodTypeCode.toLowerCase() == food) {
          return route.printerId;
        }
      }
      return '';
    }

    if (!mounted) return;
    setState(() {
      printers = list;
      kitchenId = idFor('food', 'KOT');
      barId = idFor('beverage', 'KOT');
      billId = idFor('', 'BILL');
      for (final p in list) {
        if (billId.isEmpty && p.purpose == 'BILL') billId = p.id;
        if (kitchenId.isEmpty && p.area == 'KITCHEN') kitchenId = p.id;
        if (barId.isEmpty && p.area == 'BAR') barId = p.id;
      }
      loading = false;
    });
  }

  Future<void> save() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    final routes = <PrinterRouteRule>[
      if (kitchenId.isNotEmpty)
        PrinterRouteRule(
          printerId: kitchenId,
          documentType: 'KOT',
          foodTypeCode: 'food',
        ),
      if (barId.isNotEmpty)
        PrinterRouteRule(
          printerId: barId,
          documentType: 'KOT',
          foodTypeCode: 'beverage',
        ),
      if (billId.isNotEmpty)
        PrinterRouteRule(printerId: billId, documentType: 'BILL'),
    ];
    await ref
        .read(storePrinterApiProvider)
        .saveRoutes(session.licenceUserId, routes);
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Routing saved')));
  }

  @override
  Widget build(BuildContext context) {
    final routeIds = <String>['', ...printers.map((p) => p.id)];
    String labelFor(String id) {
      if (id.isEmpty) return 'Not set';
      for (final p in printers) {
        if (p.id == id) return p.printerName;
      }
      return id;
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Printer routing')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                AppDropdownFormField<String>(
                  label: 'Food → Kitchen printer',
                  items: routeIds,
                  itemLabel: labelFor,
                  value: kitchenId,
                  onChanged: (v) => setState(() => kitchenId = v ?? ''),
                ),
                const SizedBox(height: 12),
                AppDropdownFormField<String>(
                  label: 'Beverage → Bar printer',
                  items: routeIds,
                  itemLabel: labelFor,
                  value: barId,
                  onChanged: (v) => setState(() => barId = v ?? ''),
                ),
                const SizedBox(height: 12),
                AppDropdownFormField<String>(
                  label: 'Bills → Counter printer',
                  items: routeIds,
                  itemLabel: labelFor,
                  value: billId,
                  onChanged: (v) => setState(() => billId = v ?? ''),
                ),
                const SizedBox(height: 16),
                AppButton(label: 'Save routing', onPressed: save),
              ],
            ),
    );
  }
}
