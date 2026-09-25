import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_slip_preview.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_device_picker_page.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

enum TestMessPreviewKind { qrToken, coupon, commonQr }

/* Settings → Printer Details → PRINT PREVIEW — same pattern as invoice. */
class TestMessPreviewPage extends ConsumerStatefulWidget {
  const TestMessPreviewPage({super.key, required this.kind});

  final TestMessPreviewKind kind;

  @override
  ConsumerState<TestMessPreviewPage> createState() =>
      TestMessPreviewPageState();
}

class TestMessPreviewPageState extends ConsumerState<TestMessPreviewPage> {
  bool printing = false;
  bool connecting = false;

  bool get isQr => widget.kind == TestMessPreviewKind.qrToken;
  bool get isCommonQr => widget.kind == TestMessPreviewKind.commonQr;

  String get title {
    if (isCommonQr) return 'Mess QR Code Preview';
    if (isQr) return 'Mess QR Token Preview';
    return 'Mess Coupon Preview';
  }

  static const sampleName = 'Demo Member';
  static const sampleMobile = '9876543210';
  static const sampleMeal = 'Lunch';
  static const sampleToken = 'ABCD1234';
  static const sampleQrPayload = 'MESS|DEMO|ABCD1234|Lunch';
  static const sampleCommonQrUrl =
      'https://posbillingwala.com/mess?code=DEMOQR';

  Future<void> connect() async {
    var settings = ref.read(printerSettingsProvider);
    final transport = settings.transportFor(isKot: false);
    var mac = settings.bluetoothFor(isKot: false);
    var usbId = settings.usbIdFor(isKot: false);
    var usbName = settings.usbNameFor(isKot: false);

    if ((transport == PosPrinterTransport.usb && usbId.isEmpty) ||
        (transport == PosPrinterTransport.bluetooth && mac.isEmpty)) {
      final picked = await Navigator.of(context).push<PickedPrinter>(
        MaterialPageRoute(
          builder: (_) => PrinterDevicePickerPage(
            channel: PrinterChannelKind.bill,
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
      settings = current.copyWith(
        billTransport: picked.transport,
        billBluetoothAddress: picked.transport == PosPrinterTransport.bluetooth
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
      mac = settings.bluetoothFor(isKot: false);
      usbId = settings.usbIdFor(isKot: false);
      usbName = settings.usbNameFor(isKot: false);
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
          PrinterChannelKind.bill,
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

  Future<void> testPrint() async {
    setState(() => printing = true);
    try {
      final profile = ref.read(shopReceiptProfileProvider);
      final width = ref.read(printerSettingsProvider).charsPerLine;
      final MessSlipLayout layout;
      if (isCommonQr) {
        layout = MessSlipBuilder.commonQrLayout(
          profile: profile,
          messTitle: 'MESS QR',
          qrPayload: sampleCommonQrUrl,
        );
      } else if (isQr) {
        layout = MessSlipBuilder.qrTokenLayout(
          profile: profile,
          memberName: sampleName,
          memberMobile: sampleMobile,
          messType: sampleMeal,
          tokenCode: sampleToken,
          qrPayload: sampleQrPayload,
        );
      } else {
        layout = MessSlipBuilder.couponLayout(
          profile: profile,
          memberName: sampleName,
          messType: sampleMeal,
          couponNo: 1,
        );
      }
      final result = await ref.read(printServiceProvider).printMessSlip(
        layout.toPlainText(width: width),
        layout: layout,
        label: isCommonQr
            ? 'Mess QR'
            : (isQr ? 'Mess QR token' : 'Mess coupon'),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result.message ?? result.outcome.name)),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Print failed: $e')));
    } finally {
      if (mounted) setState(() => printing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(printerSettingsProvider);
    final profile = ref.watch(shopReceiptProfileProvider);
    final strings = AppStrings.of(ref);
    final connected = BluetoothPrinterHub.instance.isReady;
    final paper = settings.paperSizeFor(isKot: false);

    final MessSlipLayout layout;
    if (isCommonQr) {
      layout = MessSlipBuilder.commonQrLayout(
        profile: profile,
        messTitle: 'MESS QR',
        qrPayload: sampleCommonQrUrl,
      );
    } else if (isQr) {
      layout = messQrTokenPreviewLayout(
        profile: profile,
        memberName: sampleName,
        memberMobile: sampleMobile,
        messType: sampleMeal,
        tokenCode: sampleToken,
        qrPayload: sampleQrPayload,
      );
    } else {
      layout = messCouponPreviewLayout(
        profile: profile,
        memberName: sampleName,
        messType: sampleMeal,
        couponNo: 1,
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF3F6FB),
      appBar: AppBar(title: Text(title)),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
          padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            24,
          ),
          children: [
            Text(
              connected
                  ? 'Printer ready — preview and test print use ${paper.dbValue}.'
                  : 'Connect a printer, then test print using ${paper.dbValue}.',
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: Colors.black54),
            ),
            const SizedBox(height: 8),
            Text(
              [
                'Paper ${paper.dbValue}',
                if (settings.logoUse)
                  profile.logoLocalPath.trim().isNotEmpty
                      ? 'Logo ON'
                      : 'Logo ON (no shop logo file)'
                else
                  'Logo OFF',
                '${settings.charsPerLine} chars',
              ].join(' · '),
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: AppColors.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 16),
            MessSlipPreview(
              layout: layout,
              showStatusLine: false,
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
                  onPressed: printing ? null : connect,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: AppButton(
                  label: strings.testPrint,
                  isLoading: printing,
                  onPressed: connecting ? null : testPrint,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
