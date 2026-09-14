import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_image_share.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/presentation/woosim_ticket.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';

/* WithTable `BluetoothPrint` / `DuplicateBluetoothPrint` / `InvoiceDetailsBluetoothPrint`: */
/* live 2" + 3" receipt, print, and bitmap share of the on-screen ticket. */
class BillPrintPreviewPage extends ConsumerStatefulWidget {
  const BillPrintPreviewPage({
    super.key,
    required this.invoiceId,
    this.duplicate = false,
  });

  final int invoiceId;
  final bool duplicate;

  @override
  ConsumerState<BillPrintPreviewPage> createState() =>
      BillPrintPreviewPageState();
}

class BillPrintPreviewPageState extends ConsumerState<BillPrintPreviewPage> {
  final ticket48Key = GlobalKey();
  final ticket72Key = GlobalKey();

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings.of(ref);
    return FutureBuilder<(Invoice, List<InvoiceItem>)?>(
      future: loadInvoice(ref),
      builder: (context, snap) {
        if (!snap.hasData && snap.connectionState != ConnectionState.done) {
          return Scaffold(
            appBar: AppBar(title: Text(strings.invoicePreview)),
            body: const Center(child: CircularProgressIndicator()),
          );
        }
        final data = snap.data;
        if (data == null) {
          return Scaffold(
            appBar: AppBar(title: Text(strings.invoicePreview)),
            body: Center(child: Text(strings.invoiceNotFound)),
          );
        }
        final invoice = data.$1;
        final items = data.$2;
        final service = ref.watch(printServiceProvider);
        final shop = ref.watch(shopReceiptProfileProvider);
        final shopName = shop.companyName.isNotEmpty
            ? shop.companyName
            : ref.watch(authControllerProvider).session?.shopName;
        final ticket = service.billTicket(
          invoice: invoice,
          items: items,
          shopName: shopName,
          duplicate: widget.duplicate,
        );
        final use3Inch = ref.watch(printerSettingsProvider).paperSize ==
            PrinterPaperSize.inch3;

        return Scaffold(
          backgroundColor: const Color(0xFFF3F6FB),
          appBar: AppBar(
            title: Text(
              widget.duplicate ? strings.duplicatePrint : strings.invoicePreview,
            ),
          ),
          body: ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 28),
            children: [
              PreviewCard(
                title: strings.paper2Inch,
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: RepaintBoundary(
                    key: ticket48Key,
                    child: WoosimTicket(ticket: ticket, widthMm: 48),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              PreviewCard(
                title: strings.paper3Inch,
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: RepaintBoundary(
                    key: ticket72Key,
                    child: WoosimTicket(ticket: ticket, widthMm: 72),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              AppButton(
                label: strings.print,
                icon: Icons.print_rounded,
                onPressed: () async {
                  final result = await printInvoiceById(
                    ref,
                    widget.invoiceId,
                    duplicate: widget.duplicate,
                  );
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(result.message ?? strings.printed)),
                  );
                },
              ),
              const SizedBox(height: 10),
              AppButton(
                label: strings.shareBillImage,
                icon: Icons.share_rounded,
                variant: AppButtonVariant.outlined,
                onPressed: () async {
                  try {
                    await shareTicketWidgetAsImage(
                      boundaryKey: use3Inch ? ticket72Key : ticket48Key,
                      label: widget.duplicate ? 'Duplicate bill' : 'Invoice',
                    );
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(strings.shared)),
                    );
                  } catch (e) {
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('$e')),
                    );
                  }
                },
              ),
              const SizedBox(height: 10),
              AppButton(
                label: strings.shareBillText,
                icon: Icons.notes_rounded,
                variant: AppButtonVariant.outlined,
                onPressed: () async {
                  final result = await printInvoiceById(
                    ref,
                    widget.invoiceId,
                    duplicate: widget.duplicate,
                    preferShare: true,
                  );
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(result.message ?? strings.shared)),
                  );
                },
              ),
            ],
          ),
        );
      },
    );
  }

  Future<(Invoice, List<InvoiceItem>)?> loadInvoice(WidgetRef ref) async {
    final db = ref.read(appDatabaseProvider);
    final invoice = await db.getInvoiceById(widget.invoiceId);
    if (invoice == null) return null;
    final items = await db.getInvoiceItems(invoice.invoiceNumber);
    return (invoice, items);
  }
}

class PreviewCard extends StatelessWidget {
  const PreviewCard({super.key, required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF3F6FB),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE2E8F2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          ColoredBox(color: Colors.white, child: child),
        ],
      ),
    );
  }
}
