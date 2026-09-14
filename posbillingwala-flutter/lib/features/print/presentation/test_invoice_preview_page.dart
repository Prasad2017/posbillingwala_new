import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/presentation/bluetooth_device_picker_page.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

/// Matches `activity_test_invoice_bluetooth_print.xml`:
/// preview card + bottom Connect / Test Print.
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

  String get testInvoicePreviewPagePreviewText =>
      ref.read(printServiceProvider).previewText(
            widget.channel,
            shopName: testInvoicePreviewPageShopName,
          );

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
    final settings = ref.read(printerSettingsProvider);
    final mac = isKot
        ? settings.kotBluetoothAddress
        : settings.billBluetoothAddress;
    if (mac.trim().isEmpty) {
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => BluetoothDevicePickerPage(
            channel: widget.channel,
            title: 'Select device',
          ),
        ),
      );
      if (mounted) setState(() {});
      return;
    }
    setState(() => connecting = true);
    try {
      final ok = await BluetoothPrinterHub.instance.connect(
        widget.channel,
        address: mac,
        fromUser: true,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(ok ? 'Printer connected' : 'Could not connect'),
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
    final chars = settings.charsPerLine;
    final text = testInvoicePreviewPagePreviewText;
    final connected = BluetoothPrinterHub.instance.isReady;

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
                ? 'Printer ready — review sample layout then Test Print.'
                : 'Connect a printer, then Test Print to verify layout.',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Colors.black54,
                ),
          ),
          const SizedBox(height: 16),
          Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(
                maxWidth: chars <= 32 ? 280 : 360,
              ),
              child: AppCard(
                padding: EdgeInsets.zero,
                child: Material(
                  color: Colors.white,
                  child: Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 16,
                    ),
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.black12),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const Text(
                          'TEST COPY',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontWeight: FontWeight.w800,
                            letterSpacing: 1.2,
                          ),
                        ),
                        const SizedBox(height: 12),
                        SelectableText(
                          text,
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: chars <= 32 ? 12.5 : 11.5,
                            height: 1.35,
                            color: Colors.black87,
                            letterSpacing: 0.2,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
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
