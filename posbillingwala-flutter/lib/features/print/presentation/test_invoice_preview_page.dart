import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/sample_receipt_data.dart';
import 'package:pos_billingwala_v2/features/print/presentation/paper_size_preview.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_device_picker_page.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* Matches `activity_test_invoice_bluetooth_print.xml`: */
/* preview card + bottom Connect / Test Print. */
class TestInvoicePreviewPage extends ConsumerStatefulWidget {
  const TestInvoicePreviewPage({
    super.key,
    required this.channel,
  });

  final PrinterChannelKind channel;

  @override
  ConsumerState<TestInvoicePreviewPage> createState() =>
      TestInvoicePreviewPageState();
}

class TestInvoicePreviewPageState extends ConsumerState<TestInvoicePreviewPage> {
  bool printing = false;
  bool connecting = false;

  bool get isKot => widget.channel == PrinterChannelKind.kot;

  String get testInvoicePreviewPageTitle => isKot ? 'KOT Preview' : 'Invoice Preview';

  String? get testInvoicePreviewPageShopName =>
      ref.read(authControllerProvider).session?.shopName;

  Future<void> testInvoicePreviewPagePrint() async {
    setState(() => printing = true);
    try {
      final result = await ref.read(printServiceProvider).printTest(
            widget.channel,
            shopName: testInvoicePreviewPageShopName,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result.message ?? result.outcome.name)),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Print failed: $e')),
      );
    } finally {
      if (mounted) setState(() => printing = false);
    }
  }

  Future<void> testInvoicePreviewPageConnect() async {
    var settings = ref.read(printerSettingsProvider);
    final transport = settings.transportFor(isKot: isKot);
    var mac = settings.bluetoothFor(isKot: isKot);
    var usbId = settings.usbIdFor(isKot: isKot);
    var usbName = settings.usbNameFor(isKot: isKot);

    if ((transport == PosPrinterTransport.usb && usbId.isEmpty) ||
        (transport == PosPrinterTransport.bluetooth && mac.isEmpty)) {
      final picked = await Navigator.of(context).push<PickedPrinter>(
        MaterialPageRoute(
          builder: (_) => PrinterDevicePickerPage(
            channel: widget.channel,
            initialTransport: transport,
            showNetwork: false,
            lockToInitialTransport: true,
            title: transport == PosPrinterTransport.usb
                ? 'Select USB printer'
                : 'Select Bluetooth printer',
          ),
        ),
      );
      if (picked == null || !mounted) return;
      final current = ref.read(printerSettingsProvider);
      settings = isKot
          ? current.copyWith(
              kotTransport: picked.transport,
              kotBluetoothAddress:
                  picked.transport == PosPrinterTransport.bluetooth
                      ? picked.bluetoothMac
                      : current.kotBluetoothAddress,
              kotUsbIdentifier: picked.transport == PosPrinterTransport.usb
                  ? picked.usbIdentifier
                  : current.kotUsbIdentifier,
              kotUsbName: picked.transport == PosPrinterTransport.usb
                  ? picked.usbName
                  : current.kotUsbName,
            )
          : current.copyWith(
              billTransport: picked.transport,
              billBluetoothAddress:
                  picked.transport == PosPrinterTransport.bluetooth
                      ? picked.bluetoothMac
                      : current.billBluetoothAddress,
              billUsbIdentifier: picked.transport == PosPrinterTransport.usb
                  ? picked.usbIdentifier
                  : current.billUsbIdentifier,
              billUsbName: picked.transport == PosPrinterTransport.usb
                  ? picked.usbName
                  : current.billUsbName,
            );
      await ref.read(printerSettingsProvider.notifier).update(settings);
      mac = settings.bluetoothFor(isKot: isKot);
      usbId = settings.usbIdFor(isKot: isKot);
      usbName = settings.usbNameFor(isKot: isKot);
    }

    setState(() => connecting = true);
    try {
      var ok = false;
      if (transport == PosPrinterTransport.usb) {
        ok = await EscPosTransportHub.instance.connectUsb(
          identifier: usbId,
          name: usbName,
        );
      } else {
        ok = await BluetoothPrinterHub.instance.connect(
          widget.channel,
          address: mac,
          fromUser: true,
        );
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            ok
                ? '${transport.label} printer connected'
                : 'Could not connect ${transport.label} printer',
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => connecting = false);
    }
  }

  @override
  void initState() {
    super.initState();
    if (const bool.fromEnvironment('AUTO_TEST_PRINT')) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) testInvoicePreviewPagePrint();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(printerSettingsProvider);
    final strings = AppStrings.of(ref);
    final service = ref.watch(printServiceProvider);
    final shop = testInvoicePreviewPageShopName;
    final connected = BluetoothPrinterHub.instance.isReady;
    final sample = isKot
        ? service.kotPreviewText(paperSize: settings.kotPaperSize)
        : null;
    final bill = SampleReceiptData.sampleBill();

    return Scaffold(
      appBar: AppBar(title: Text(testInvoicePreviewPageTitle)),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            24),
        children: [
          Text(
            connected
                ? (isKot
                    ? 'Printer ready — KOT layout follows your KOT paper size, prefix & copies.'
                    : 'Printer ready — layout follows your paper size, customer, payment & logo options.')
                : (isKot
                    ? 'Preview uses your KOT options (paper size, prefix, copies). Connect to Test Print.'
                    : 'Preview uses your printer options (paper size, customer, payment, logo). Connect to Test Print.'),
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Colors.black54,
                ),
          ),
          const SizedBox(height: 8),
          Text(
            isKot
                ? [
                    settings.kotPaperSize.dbValue,
                    if (settings.kotPrefix.trim().isNotEmpty)
                      'Prefix ${settings.kotPrefix.trim()}',
                    'Copies ${settings.kotCopies}',
                    if (settings.kotEnable) 'KOT ON' else 'KOT OFF',
                    if (settings.kotAutoPrint) 'Auto print ON' else 'Auto print OFF',
                    if (settings.kotPreview) 'Preview ON' else 'Preview OFF',
                  ].join(' · ')
                : [
                    settings.paperSize.dbValue,
                    if (settings.customerUse) 'Customer ON' else 'Customer OFF',
                    if (settings.paymentUse) 'Payment ON' else 'Payment OFF',
                    if (settings.logoUse) 'Logo ON' else 'Logo OFF',
                  ].join(' · '),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 16),
          if (isKot)
            PaperSizePreviewCard(
              title: 'KOT · ${settings.kotPaperSize.dbValue}',
              text: sample!,
              paperSize: settings.kotPaperSize,
            )
          else
            PaperSizePreviewCard(
              title: 'Bill · ${settings.paperSize.dbValue}',
              text: service.billPreviewText(
                invoice: bill.invoice,
                items: bill.items,
                shopName: shop,
                paperSize: settings.paperSize,
              ),
              paperSize: settings.paperSize,
            ),
        ],
      ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child: Row(
            children: [
              Expanded(
                child: AppButton(
                  label: 'Connect',
                  variant: AppButtonVariant.outlined,
                  isLoading: connecting,
                  onPressed: printing ? null : testInvoicePreviewPageConnect,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: AppButton(
                  label: strings.testPrint,
                  isLoading: printing,
                  onPressed: connecting ? null : testInvoicePreviewPagePrint,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
