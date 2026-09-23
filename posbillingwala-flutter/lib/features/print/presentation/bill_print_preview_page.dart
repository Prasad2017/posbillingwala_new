import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_image_share.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/presentation/woosim_ticket.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Invoice / duplicate bill preview + print — layout matches selected paper size. */
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
  final ticketKey = GlobalKey();

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings.of(ref);
    return FutureBuilder<(Invoice, List<InvoiceItem>)?>(
      future: loadInvoice(ref),
      builder: (context, snap) {
        if (!snap.hasData && snap.connectionState != ConnectionState.done) {
          return Scaffold(
            backgroundColor: Colors.transparent,
            appBar: AppBar(title: Text(strings.invoicePreview)),
            body: const Center(child: CircularProgressIndicator()),
          );
        }
        final data = snap.data;
        if (data == null) {
          return Scaffold(
            backgroundColor: Colors.transparent,
            appBar: AppBar(title: Text(strings.invoicePreview)),
            body: Center(child: Text(strings.invoiceNotFound)),
          );
        }
        final invoice = data.$1;
        final items = data.$2;
        final service = ref.watch(printServiceProvider);
        final settings = ref.watch(printerSettingsProvider);
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
        final is3Inch = settings.paperSize == PrinterPaperSize.inch3;
        final widthMm = is3Inch ? 72.0 : 48.0;

        return Scaffold(
          backgroundColor: Colors.transparent,
          appBar: AppBar(
            title: Text(
              widget.duplicate
                  ? strings.duplicatePrint
                  : strings.invoicePreview,
            ),
          ),
          body: Column(
            children: [
              Expanded(
                child: Center(
                  child: SingleChildScrollView(
                    padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      context.isShortHeight ? 6 : 12,
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      12,
                    ),
                    child: Center(
                      child: ConstrainedBox(
                        constraints: BoxConstraints(
                          maxWidth: AppBreakpoints.contentMaxWidthFor(
                            AppWidthClass.mobile,
                          ),
                        ),
                        child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: RepaintBoundary(
                          key: ticketKey,
                          child: WoosimTicket(
                            ticket: ticket,
                            widthMm: widthMm,
                            showLogo: settings.logoUse,
                            logoPath: shop.logoLocalPath,
                          ),
                        ),
                      ),
                      ),
                    ),
                  ),
                ),
              ),
              SafeArea(
                top: false,
                child: Padding(
                  padding: EdgeInsets.fromLTRB(
                    AppBreakpoints.pagePaddingFor(context.widthClass),
                    8,
                    AppBreakpoints.pagePaddingFor(context.widthClass),
                    12,
                  ),
                  child: context.isCompactWidth
                      ? Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            AppButton(
                              label: 'Share Bill',
                              icon: Icons.share_rounded,
                              variant: AppButtonVariant.outlined,
                              onPressed: () async {
                                try {
                                  await shareTicketWidgetAsImage(
                                    boundaryKey: ticketKey,
                                    label: widget.duplicate
                                        ? 'Duplicate bill'
                                        : 'Invoice',
                                  );
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(content: Text(strings.shared)),
                                  );
                                } catch (e) {
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(
                                    context,
                                  ).showSnackBar(SnackBar(content: Text('$e')));
                                }
                              },
                            ),
                            const SizedBox(height: 8),
                            AppButton(
                              label: 'Print Bill',
                              icon: Icons.print_rounded,
                              onPressed: () async {
                                final result = await printInvoiceById(
                                  ref,
                                  widget.invoiceId,
                                  duplicate: widget.duplicate,
                                );
                                if (!context.mounted) return;
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      result.message ?? strings.printed,
                                    ),
                                  ),
                                );
                              },
                            ),
                          ],
                        )
                      : Row(
                    children: [
                      Expanded(
                        child: AppButton(
                          label: 'Share Bill',
                          icon: Icons.share_rounded,
                          variant: AppButtonVariant.outlined,
                          onPressed: () async {
                            try {
                              await shareTicketWidgetAsImage(
                                boundaryKey: ticketKey,
                                label: widget.duplicate
                                    ? 'Duplicate bill'
                                    : 'Invoice',
                              );
                              if (!context.mounted) return;
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(content: Text(strings.shared)),
                              );
                            } catch (e) {
                              if (!context.mounted) return;
                              ScaffoldMessenger.of(
                                context,
                              ).showSnackBar(SnackBar(content: Text('$e')));
                            }
                          },
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: AppButton(
                          label: 'Print Bill',
                          icon: Icons.print_rounded,
                          onPressed: () async {
                            final result = await printInvoiceById(
                              ref,
                              widget.invoiceId,
                              duplicate: widget.duplicate,
                            );
                            if (!context.mounted) return;
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(result.message ?? strings.printed),
                              ),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                ),
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
