import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_builder.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_device_picker_page.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class PrinterFormPage extends ConsumerStatefulWidget {
  const PrinterFormPage({super.key, this.existing});

  final StorePrinter? existing;

  @override
  ConsumerState<PrinterFormPage> createState() => PrinterFormPageState();
}

class PrinterFormPageState extends ConsumerState<PrinterFormPage> {
  late final name = TextEditingController(
    text: widget.existing?.printerName ?? '',
  );
  late final bt = TextEditingController(
    text: widget.existing?.bluetoothAddress ?? '',
  );
  late final usb = TextEditingController(
    text: widget.existing?.usbIdentifier ?? '',
  );
  String connection = 'BLUETOOTH';
  String paperSize = '2-Inch';
  String purpose = 'KOT';
  String area = 'KITCHEN';
  String btName = '';
  String usbName = '';
  bool bindThisDevice = true;
  bool saving = false;

  bool get isUsb => connection == 'USB';

  @override
  void initState() {
    super.initState();
    final existing = widget.existing;
    final type = (existing?.connectionType ?? 'BLUETOOTH').toUpperCase();
    connection = type == 'USB' ? 'USB' : 'BLUETOOTH';
    paperSize = existing?.paperSizeLabel ?? '2-Inch';
    purpose = existing?.purpose ?? 'KOT';
    area = existing?.area ?? 'KITCHEN';
    usbName = existing?.usbName ?? '';
    bindThisDevice = (existing?.deviceId ?? '').isNotEmpty || existing == null;
  }

  @override
  void dispose() {
    name.dispose();
    bt.dispose();
    usb.dispose();
    super.dispose();
  }

  Future<void> pickDevice() async {
    final channel = purpose == 'BILL'
        ? PrinterChannelKind.bill
        : PrinterChannelKind.kot;
    final picked = await Navigator.of(context).push<PickedPrinter>(
      MaterialPageRoute(
        builder: (_) => PrinterDevicePickerPage(
          channel: channel,
          initialTransport: isUsb
              ? PosPrinterTransport.usb
              : PosPrinterTransport.bluetooth,
          showNetwork: false,
          lockToInitialTransport: true,
          title: isUsb ? 'Select USB printer' : 'Select Bluetooth printer',
        ),
      ),
    );
    if (picked == null || !mounted) return;
    setState(() {
      if (picked.transport == PosPrinterTransport.usb) {
        connection = 'USB';
        usb.text = picked.usbIdentifier;
        usbName = picked.usbName;
      } else {
        connection = 'BLUETOOTH';
        bt.text = picked.bluetoothMac;
        btName = picked.bluetoothName;
      }
    });
  }

  Future<void> save() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    if (name.text.trim().isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Enter printer name')));
      return;
    }
    if (isUsb && usb.text.trim().isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Select a USB printer')));
      return;
    }
    if (!isUsb && bt.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Select a Bluetooth printer')),
      );
      return;
    }
    setState(() => saving = true);
    try {
      final device = await DeviceIdentityService().resolve();
      await ref.read(storePrinterApiProvider).save(session.licenceUserId, {
        'printerName': name.text.trim(),
        'connectionType': connection,
        'bluetoothAddress': bt.text.trim(),
        'usbIdentifier': usb.text.trim(),
        'usbName': usbName,
        'paperSize': paperSize,
        'purpose': purpose,
        'area': area,
        'deviceId': bindThisDevice ? device.deviceId : '',
      }, id: widget.existing?.id);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  Future<void> testPrint() async {
    final service = ref.read(printServiceProvider);
    final paper = paperSize == '3-Inch'
        ? PrinterPaperSize.inch3
        : PrinterPaperSize.inch2;
    final builder = ReceiptBuilder(
      service.settings.copyWith(paperSize: paper),
      shopProfile: service.shopProfile,
      labels: service.labels,
    );
    final bytes = await builder.testPrintBytes(
      isUsb ? 'USB $paperSize' : 'Bluetooth $paperSize',
    );
    final result = await service.dispatchToEndpoint(
      text: 'TEST $paperSize',
      bytes: bytes,
      label: 'Test print',
      transport: isUsb
          ? PosPrinterTransport.usb
          : PosPrinterTransport.bluetooth,
      bluetoothAddress: bt.text.trim(),
      usbIdentifier: usb.text.trim(),
      usbName: usbName,
    );
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(result.message ?? result.outcome.name)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStrings.of(ref);
    final selectedLabel = isUsb
        ? (usbName.isNotEmpty ? usbName : usb.text.trim())
        : (btName.isNotEmpty ? btName : bt.text.trim());
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.existing == null ? 'Add printer' : 'Edit printer'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppTextField(
            required: true,
            controller: name,
            label: 'Printer name',
            hint: 'Kitchen printer',
          ),
          const SizedBox(height: 16),
          const Text(
            'Printer type',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          SegmentedButton<String>(
            segments: const [
              ButtonSegment(
                value: 'BLUETOOTH',
                icon: Icon(Icons.bluetooth_rounded),
                label: Text('Bluetooth'),
              ),
              ButtonSegment(
                value: 'USB',
                icon: Icon(Icons.usb_rounded),
                label: Text('USB'),
              ),
            ],
            selected: {connection},
            onSelectionChanged: (v) => setState(() => connection = v.first),
          ),
          const SizedBox(height: 16),
          const Text(
            'Page size',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          SegmentedButton<String>(
            segments: [
              ButtonSegment(value: '2-Inch', label: Text(strings.paper2Inch)),
              ButtonSegment(value: '3-Inch', label: Text(strings.paper3Inch)),
            ],
            selected: {paperSize},
            onSelectionChanged: (v) => setState(() => paperSize = v.first),
          ),
          const SizedBox(height: 16),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Icon(
              isUsb ? Icons.usb_rounded : Icons.bluetooth_rounded,
              color: AppColors.primary,
            ),
            title: Text(isUsb ? 'USB printer' : 'Bluetooth printer'),
            subtitle: Text(
              selectedLabel.isEmpty
                  ? (isUsb
                        ? 'Tap to scan USB / OTG printers'
                        : 'Tap to pick a paired Bluetooth printer')
                  : selectedLabel,
            ),
            trailing: const Icon(Icons.chevron_right_rounded),
            onTap: pickDevice,
          ),
          const SizedBox(height: 8),
          AppDropdownFormField<String>(
            label: 'Purpose',
            items: const ['KOT', 'BILL', 'PACKING', 'LABEL', 'REPORT'],
            itemLabel: (v) => switch (v) {
              'BILL' => 'Bill',
              'PACKING' => 'Packing',
              'LABEL' => 'Label',
              'REPORT' => 'Report',
              _ => v,
            },
            value: purpose,
            onChanged: (v) => setState(() => purpose = v ?? purpose),
          ),
          const SizedBox(height: 8),
          AppDropdownFormField<String>(
            label: 'Area',
            items: const ['KITCHEN', 'BAR', 'COUNTER', 'PACKING', 'TAKEAWAY'],
            itemLabel: (v) => switch (v) {
              'KITCHEN' => 'Kitchen',
              'BAR' => 'Bar',
              'COUNTER' => 'Counter',
              'PACKING' => 'Packing',
              'TAKEAWAY' => 'Takeaway',
              _ => v,
            },
            value: area,
            onChanged: (v) => setState(() => area = v ?? area),
          ),
          AppSwitchTile(
            title: 'This device is the print host',
            subtitle: 'Leave off for a waiter device with no printer',
            value: bindThisDevice,
            showDivider: false,
            onChanged: (v) => setState(() => bindThisDevice = v),
          ),
          AppButton(
            label: 'Test print',
            variant: AppButtonVariant.outlined,
            onPressed: testPrint,
          ),
          const SizedBox(height: 8),
          AppButton(
            label: saving ? 'Saving…' : 'Save',
            onPressed: saving ? null : save,
          ),
        ],
      ),
    );
  }
}
