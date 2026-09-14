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
      _TestInvoicePreviewPageState();
}

class _TestInvoicePreviewPageState extends ConsumerState<TestInvoicePreviewPage> {
  bool _printing = false;
  bool _connecting = false;

  bool get _isKot => widget.channel == PrinterChannelKind.kot;

  String get _title => _isKot ? 'KOT Preview' : 'Invoice Preview';

  String? get _shopName =>
      ref.read(authControllerProvider).session?.shopName;

  String get _previewText =>
      ref.read(printServiceProvider).previewText(
            widget.channel,
            shopName: _shopName,
          );

  Future<void> _print() async {
    setState(() => _printing = true);
    try {
      final result = await ref.read(printServiceProvider).printTest(
            widget.channel,
            shopName: _shopName,
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
      if (mounted) setState(() => _printing = false);
    }
  }

  Future<void> _connect() async {
    final settings = ref.read(printerSettingsProvider);
    final mac = _isKot
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
    setState(() => _connecting = true);
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
      if (mounted) setState(() => _connecting = false);
    }
  }

  @override
  void initState() {
    super.initState();
    if (const bool.fromEnvironment('AUTO_TEST_PRINT')) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _print();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(printerSettingsProvider);
    final strings = AppStrings.of(ref);
    final chars = settings.charsPerLine;
    final text = _previewText;
    final connected = BluetoothPrinterHub.instance.isReady;

    return Scaffold(
      appBar: AppBar(title: Text(_title)),
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
                  isLoading: _connecting,
                  onPressed: _printing ? null : _connect,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: AppButton(
                  label: strings.testPrint,
                  isLoading: _printing,
                  onPressed: _connecting ? null : _print,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
