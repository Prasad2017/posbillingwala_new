import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/company/data/company_api.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_device_picker_page.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

class SettingsPage extends ConsumerStatefulWidget {
  const SettingsPage({super.key});

  @override
  ConsumerState<SettingsPage> createState() => SettingsPageState();
}

class SettingsPageState extends ConsumerState<SettingsPage> {
  late final TextEditingController settingsPageBillMac;
  late final TextEditingController settingsPageKotMac;
  late final TextEditingController host;
  late final TextEditingController settingsPagePort;
  late final TextEditingController settingsPageFeed;
  late final TextEditingController settingsPageKotFeed;
  late final TextEditingController settingsPageInvoiceTitle;
  late final TextEditingController settingsPageInvoiceTerms;
  late final TextEditingController settingsPageInvoicePrefix;
  late final TextEditingController settingsPageKotPrefix;
  late final TextEditingController settingsPageKotCopies;
  bool companyBusy = false;
  bool btBusy = false;
  String btStatus = 'Checking…';
  String usbStatus = 'USB idle';
  final hub = BluetoothPrinterHub.instance;
  final usbHub = EscPosTransportHub.instance;

  @override
  void initState() {
    super.initState();
    final settings = ref.read(printerSettingsProvider);
    settingsPageBillMac = TextEditingController(
      text: settings.billBluetoothAddress,
    );
    settingsPageKotMac = TextEditingController(
      text: settings.kotBluetoothAddress,
    );
    host = TextEditingController(text: settings.networkHost);
    settingsPagePort = TextEditingController(text: '${settings.networkPort}');
    settingsPageFeed = TextEditingController(text: '${settings.feedLines}');
    settingsPageKotFeed = TextEditingController(
      text: '${settings.kotFeedLines}',
    );
    settingsPageInvoiceTitle = TextEditingController(
      text: settings.invoiceTitle,
    );
    settingsPageInvoiceTerms = TextEditingController(
      text: settings.invoiceTerms,
    );
    settingsPageInvoicePrefix = TextEditingController(
      text: settings.invoicePrefix,
    );
    settingsPageKotPrefix = TextEditingController(text: settings.kotPrefix);
    settingsPageKotCopies = TextEditingController(
      text: '${settings.kotCopies}',
    );
    Future.microtask(() async {
      await loadPrinterCloud();
      await syncHubAndAutoConnect();
      await refreshBtStatus();
    });
  }

  Future<void> syncHubAndAutoConnect() async {
    final s = ref.read(printerSettingsProvider);
    hub.updateSavedAddresses(
      billMac: s.billBluetoothAddress,
      kotMac: s.kotBluetoothAddress,
    );
    usbHub.updateSavedUsb(identifier: s.billUsbIdentifier, name: s.billUsbName);
    if (s.billTransport == PosPrinterTransport.bluetooth) {
      await hub.autoConnect(PrinterChannelKind.bill);
    } else if (s.billTransport == PosPrinterTransport.usb &&
        s.billUsbIdentifier.isNotEmpty) {
      await usbHub.connectUsb(
        identifier: s.billUsbIdentifier,
        name: s.billUsbName,
      );
    }
    if (s.kotTransport == PosPrinterTransport.bluetooth &&
        s.kotBluetoothAddress.trim().isNotEmpty &&
        s.kotBluetoothAddress.trim().toLowerCase() !=
            s.billBluetoothAddress.trim().toLowerCase()) {
      await hub.autoConnect(PrinterChannelKind.kot);
    }
  }

  Future<void> refreshBtStatus() async {
    final on = await hub.isBluetoothOn();
    final linked = await hub.connectionStatus();
    final usbLinked = usbHub.isConnected;
    if (!mounted) return;
    setState(() {
      if (!on) {
        btStatus = 'Bluetooth off';
      } else if (linked && hub.connectedAddress.isNotEmpty) {
        btStatus = 'BT connected: ${hub.connectedAddress}';
      } else if (hub.isConnecting) {
        btStatus = 'Connectingâ€¦';
      } else {
        btStatus = 'Bluetooth not connected';
      }
      if (usbLinked && usbHub.savedUsbId.isNotEmpty) {
        usbStatus =
            'USB connected: ${usbHub.savedUsbName.isEmpty ? usbHub.savedUsbId : usbHub.savedUsbName}';
      } else if (usbHub.savedUsbId.isNotEmpty) {
        usbStatus = 'USB saved: ${usbHub.savedUsbId}';
      } else {
        usbStatus = 'USB not configured';
      }
    });
  }

  Future<bool> pickPrinter(PrinterChannelKind channel) async {
    final settings = ref.read(printerSettingsProvider);
    final initial = localTransport(
      channel == PrinterChannelKind.bill
          ? settings.billTransport
          : settings.kotTransport,
    );
    final picked = await Navigator.of(context).push<PickedPrinter>(
      MaterialPageRoute(
        builder: (_) => PrinterDevicePickerPage(
          channel: channel,
          initialTransport: initial == PosPrinterTransport.usb
              ? PosPrinterTransport.usb
              : PosPrinterTransport.bluetooth,
          showNetwork: false,
          lockToInitialTransport: true,
          title: initial == PosPrinterTransport.usb
              ? 'Select USB printer'
              : 'Select Bluetooth printer',
        ),
      ),
    );
    if (picked == null || !mounted) return false;

    if (channel == PrinterChannelKind.bill) {
      if (picked.transport == PosPrinterTransport.bluetooth) {
        settingsPageBillMac.text = picked.bluetoothMac;
      }
      if (picked.transport == PosPrinterTransport.network) {
        host.text = picked.networkHost;
        settingsPagePort.text = '${picked.networkPort}';
      }
    } else {
      if (picked.transport == PosPrinterTransport.bluetooth) {
        settingsPageKotMac.text = picked.bluetoothMac;
      }
      if (picked.transport == PosPrinterTransport.network) {
        host.text = picked.networkHost;
        settingsPagePort.text = '${picked.networkPort}';
      }
    }

    final current = ref.read(printerSettingsProvider);
    final updated = channel == PrinterChannelKind.bill
        ? current.copyWith(
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
            networkHost: picked.transport == PosPrinterTransport.network
                ? picked.networkHost
                : current.networkHost,
            networkPort: picked.transport == PosPrinterTransport.network
                ? picked.networkPort
                : current.networkPort,
          )
        : current.copyWith(
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
            networkHost: picked.transport == PosPrinterTransport.network
                ? picked.networkHost
                : current.networkHost,
            networkPort: picked.transport == PosPrinterTransport.network
                ? picked.networkPort
                : current.networkPort,
          );
    await ref.read(printerSettingsProvider.notifier).update(updated);
    hub.updateSavedAddresses(
      billMac: updated.billBluetoothAddress,
      kotMac: updated.kotBluetoothAddress,
    );
    usbHub.updateSavedUsb(
      identifier: updated.usbIdFor(isKot: channel == PrinterChannelKind.kot),
      name: updated.usbNameFor(isKot: channel == PrinterChannelKind.kot),
    );
    await refreshBtStatus();
    if (!mounted) return false;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${picked.transport.label} printer selected')),
    );
    return true;
  }

  Future<void> connectChannel(PrinterChannelKind channel) async {
    setState(() => btBusy = true);
    try {
      await settingsPageSave(showSnack: false);
      var settings = ref.read(printerSettingsProvider);
      final transport = localTransport(
        channel == PrinterChannelKind.bill
            ? settings.billTransport
            : settings.kotTransport,
      );

      var ok = false;
      switch (transport) {
        case PosPrinterTransport.bluetooth:
          var mac = channel == PrinterChannelKind.bill
              ? settingsPageBillMac.text.trim()
              : settingsPageKotMac.text.trim();
          if (mac.isEmpty) {
            final picked = await pickPrinter(channel);
            if (!picked || !mounted) return;
            mac = channel == PrinterChannelKind.bill
                ? settingsPageBillMac.text.trim()
                : settingsPageKotMac.text.trim();
          }
          if (mac.isEmpty) return;
          ok = await hub.connect(channel, address: mac, fromUser: true);
        case PosPrinterTransport.usb:
          settings = ref.read(printerSettingsProvider);
          var id = settings.usbIdFor(isKot: channel == PrinterChannelKind.kot);
          if (id.isEmpty) {
            final picked = await pickPrinter(channel);
            if (!picked || !mounted) return;
            settings = ref.read(printerSettingsProvider);
            id = settings.usbIdFor(isKot: channel == PrinterChannelKind.kot);
          }
          if (id.isEmpty) return;
          ok = await usbHub.connectUsb(
            identifier: id,
            name: settings.usbNameFor(isKot: channel == PrinterChannelKind.kot),
          );
        case PosPrinterTransport.network:
          await pickPrinter(channel);
          return;
      }

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            ok
                ? '${transport.label} printer ready'
                : 'Connect failed — pick a ${transport.label} printer',
          ),
        ),
      );
      if (!ok) await pickPrinter(channel);
      await refreshBtStatus();
    } finally {
      if (mounted) setState(() => btBusy = false);
    }
  }

  Future<void> disconnectChannel(PrinterChannelKind channel) async {
    setState(() => btBusy = true);
    try {
      final settings = ref.read(printerSettingsProvider);
      final transport = localTransport(
        channel == PrinterChannelKind.bill
            ? settings.billTransport
            : settings.kotTransport,
      );
      if (transport == PosPrinterTransport.usb) {
        await usbHub.clearSavedUsb();
      } else {
        await hub.disconnect(channel);
      }
      if (channel == PrinterChannelKind.bill) {
        settingsPageBillMac.clear();
        await ref
            .read(printerSettingsProvider.notifier)
            .update(
              settings.copyWith(
                billBluetoothAddress: '',
                billUsbIdentifier: '',
                billUsbName: '',
              ),
            );
      } else {
        settingsPageKotMac.clear();
        await ref
            .read(printerSettingsProvider.notifier)
            .update(
              settings.copyWith(
                kotBluetoothAddress: '',
                kotUsbIdentifier: '',
                kotUsbName: '',
              ),
            );
      }
      await refreshBtStatus();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Printer disconnected')));
    } finally {
      if (mounted) setState(() => btBusy = false);
    }
  }

  @override
  void dispose() {
    settingsPageBillMac.dispose();
    settingsPageKotMac.dispose();
    host.dispose();
    settingsPagePort.dispose();
    settingsPageFeed.dispose();
    settingsPageKotFeed.dispose();
    settingsPageInvoiceTitle.dispose();
    settingsPageInvoiceTerms.dispose();
    settingsPageInvoicePrefix.dispose();
    settingsPageKotPrefix.dispose();
    settingsPageKotCopies.dispose();
    super.dispose();
  }

  Future<void> loadPrinterCloud() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    setState(() => companyBusy = true);
    try {
      final api = CompanyApi(ref.read(apiClientProvider));
      final printers = await api.getCompanyPrinterSetting(userId);
      if (printers.isNotEmpty) {
        final p = printers.first;
        final current = ref.read(printerSettingsProvider);
        final updated = current.copyWith(
          paperSize: PrinterPaperSizeX.fromDb(p.paperSize),
          kotPaperSize: PrinterPaperSizeX.fromDb(
            p.kotPaperSize.isEmpty ? p.paperSize : p.kotPaperSize,
          ),
          billTransport: PosPrinterTransportX.fromLocalStorage(
            p.billConnectionType,
          ),
          kotTransport: PosPrinterTransportX.fromLocalStorage(
            p.kotConnectionType,
          ),
          billBluetoothAddress: p.bluetoothAddress.isNotEmpty
              ? p.bluetoothAddress
              : current.billBluetoothAddress,
          kotBluetoothAddress: p.bluetoothKotAddress.isNotEmpty
              ? p.bluetoothKotAddress
              : current.kotBluetoothAddress,
          billUsbIdentifier: p.billUsbIdentifier.isNotEmpty
              ? p.billUsbIdentifier
              : current.billUsbIdentifier,
          billUsbName: p.billUsbName.isNotEmpty
              ? p.billUsbName
              : current.billUsbName,
          kotUsbIdentifier: p.kotUsbIdentifier.isNotEmpty
              ? p.kotUsbIdentifier
              : current.kotUsbIdentifier,
          kotUsbName: p.kotUsbName.isNotEmpty
              ? p.kotUsbName
              : current.kotUsbName,
          feedLines: int.tryParse(p.printerFeedLines) ?? current.feedLines,
          kotFeedLines:
              int.tryParse(p.kotPrinterFeedLines) ?? current.kotFeedLines,
          invoiceTitle: p.invoiceTitle.isNotEmpty
              ? p.invoiceTitle
              : current.invoiceTitle,
          invoiceTerms: p.invoiceTermsCondition.isNotEmpty
              ? p.invoiceTermsCondition
              : current.invoiceTerms,
          customerUse: printerFlagOn(p.customerUse),
          paymentUse: printerFlagOn(p.paymentUse),
          duplicateBillUse: printerFlagOn(p.duplicateBillUse),
          logoUse: printerFlagOn(p.logoUse),
          kotEnable: p.kotEnable != '0',
          productQuantityUpdate: p.productQuantityUpdate == '1',
          kotAutoPrint: p.kotAutoPrint == '1' || p.kotAutoPrint == 'on',
          kotPreview: p.kotPreview != '0' && p.kotPreview != 'off',
          kotCopies: int.tryParse(p.kotCopies) ?? current.kotCopies,
          invoicePrefix: p.invoicePrefix.isNotEmpty
              ? p.invoicePrefix
              : current.invoicePrefix,
          kotPrefix: p.kotPrefix.isNotEmpty ? p.kotPrefix : current.kotPrefix,
        );
        await ref.read(printerSettingsProvider.notifier).update(updated);
        await ref
            .read(appDatabaseProvider)
            .upsertLocalCompanyPrinterSettings(p);
        settingsPageBillMac.text = updated.billBluetoothAddress;
        settingsPageKotMac.text = updated.kotBluetoothAddress;
        settingsPageFeed.text = '${updated.feedLines}';
        settingsPageKotFeed.text = '${updated.kotFeedLines}';
        settingsPageInvoiceTitle.text = updated.invoiceTitle;
        settingsPageInvoiceTerms.text = updated.invoiceTerms;
        settingsPageInvoicePrefix.text = updated.invoicePrefix;
        settingsPageKotPrefix.text = updated.kotPrefix;
        settingsPageKotCopies.text = '${updated.kotCopies}';
        hub.updateSavedAddresses(
          billMac: updated.billBluetoothAddress,
          kotMac: updated.kotBluetoothAddress,
        );
        await hub.autoConnect(PrinterChannelKind.bill);
      }
    } catch (_) {
      /* Keep local fields if cloud load fails. */
    } finally {
      if (mounted) setState(() => companyBusy = false);
    }
  }

  Future<void> savePrinterCloud() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Please login first')));
      return;
    }
    setState(() => companyBusy = true);
    try {
      final api = CompanyApi(ref.read(apiClientProvider));
      await settingsPageSave(showSnack: false);
      final settings = ref.read(printerSettingsProvider);
      final printerDto = CompanyPrinterSettingDto(
        bluetoothAddress: settings.billBluetoothAddress,
        bluetoothKotAddress: settings.kotBluetoothAddress,
        printerFeedLines: '${settings.feedLines}',
        kotPrinterFeedLines: '${settings.kotFeedLines}',
        invoiceTitle: settings.invoiceTitle,
        invoiceTermsCondition: settings.invoiceTerms,
        invoicePrefix: settings.invoicePrefix,
        kotPrefix: settings.kotPrefix,
        customerUse: printerFlagValue(settings.customerUse),
        paymentUse: printerFlagValue(settings.paymentUse),
        duplicateBillUse: printerFlagValue(settings.duplicateBillUse),
        logoUse: printerFlagValue(settings.logoUse),
        kotEnable: settings.kotEnable ? '1' : '0',
        productQuantityUpdate: printerFlagValue(
          settings.productQuantityUpdate,
        ),
        kotAutoPrint: settings.kotAutoPrint ? '1' : '0',
        kotPreview: settings.kotPreview ? '1' : '0',
        kotCopies: '${settings.kotCopies}',
        paperSize: settings.paperSize.dbValue,
        kotPaperSize: settings.kotPaperSize.dbValue,
        billConnectionType: settings.billTransport.dbValue,
        kotConnectionType: settings.kotTransport.dbValue,
        billUsbIdentifier: settings.billUsbIdentifier,
        billUsbName: settings.billUsbName,
        kotUsbIdentifier: settings.kotUsbIdentifier,
        kotUsbName: settings.kotUsbName,
      );
      await ref
          .read(appDatabaseProvider)
          .upsertLocalCompanyPrinterSettings(printerDto);
      await api.insertCompanyPrinterSetting(
        userId: userId,
        setting: printerDto,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Printer settings saved')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => companyBusy = false);
    }
  }

  Future<void> settingsPageSave({bool showSnack = true}) async {
    final current = ref.read(printerSettingsProvider);
    final feed =
        int.tryParse(settingsPageFeed.text.trim()) ?? current.feedLines;
    final kotFeed =
        int.tryParse(settingsPageKotFeed.text.trim()) ?? current.kotFeedLines;
    final port =
        int.tryParse(settingsPagePort.text.trim()) ?? current.networkPort;
    final copies =
        int.tryParse(settingsPageKotCopies.text.trim()) ?? current.kotCopies;
    final updated = current.copyWith(
      billBluetoothAddress: settingsPageBillMac.text.trim(),
      kotBluetoothAddress: settingsPageKotMac.text.trim(),
      networkHost: host.text.trim(),
      networkPort: port.clamp(1, 65535),
      feedLines: feed.clamp(1, 10),
      kotFeedLines: kotFeed.clamp(1, 10),
      invoiceTitle: settingsPageInvoiceTitle.text.trim(),
      invoiceTerms: settingsPageInvoiceTerms.text.trim(),
      invoicePrefix: settingsPageInvoicePrefix.text.trim().isEmpty
          ? 'PB'
          : settingsPageInvoicePrefix.text.trim(),
      kotPrefix: settingsPageKotPrefix.text.trim().isEmpty
          ? 'KOT'
          : settingsPageKotPrefix.text.trim(),
      kotCopies: copies.clamp(1, 5),
    );
    await ref.read(printerSettingsProvider.notifier).update(updated);
    hub.updateSavedAddresses(
      billMac: updated.billBluetoothAddress,
      kotMac: updated.kotBluetoothAddress,
    );
    if (!mounted) return;
    if (showSnack) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Printer settings saved')));
    }
  }

  Future<void> updateSettings() async {
    await savePrinterCloud();
  }

  Future<void> openTestPreview(PrinterChannelKind channel) async {
    await settingsPageSave(showSnack: false);
    if (!mounted) return;
    final mode = channel == PrinterChannelKind.kot ? 'kot' : 'invoice';
    await context.push('/settings/test-print?mode=$mode');
    if (!mounted) return;
    await refreshBtStatus();
  }

  String billStatusLine(PrinterSettings settings) {
    final parts = <String>[
      settings.billTransport.label,
      if (settings.billTransport == PosPrinterTransport.usb &&
          settings.billUsbName.isNotEmpty)
        settings.billUsbName,
      if (settings.billTransport == PosPrinterTransport.bluetooth &&
          settings.billBluetoothAddress.isNotEmpty)
        settings.billBluetoothAddress,
    ];
    return parts.join(' · ');
  }

  String kotStatusLine(PrinterSettings settings) {
    final parts = <String>[
      settings.kotTransport.label,
      if (settings.kotTransport == PosPrinterTransport.usb &&
          settings.kotUsbName.isNotEmpty)
        settings.kotUsbName,
      if (settings.kotTransport == PosPrinterTransport.bluetooth &&
          settings.kotBluetoothAddress.isNotEmpty)
        settings.kotBluetoothAddress,
    ];
    return parts.join(' · ');
  }

  PosPrinterTransport localTransport(PosPrinterTransport value) =>
      value == PosPrinterTransport.usb
      ? PosPrinterTransport.usb
      : PosPrinterTransport.bluetooth;

  bool isChannelConnected(
    PrinterChannelKind channel,
    PrinterSettings settings,
  ) {
    final transport = localTransport(
      channel == PrinterChannelKind.bill
          ? settings.billTransport
          : settings.kotTransport,
    );
    switch (transport) {
      case PosPrinterTransport.usb:
        final id = settings.usbIdFor(isKot: channel == PrinterChannelKind.kot);
        return id.isNotEmpty &&
            usbHub.isConnected &&
            (usbHub.savedUsbId.isEmpty ||
                usbHub.savedUsbId.toLowerCase() == id.toLowerCase());
      case PosPrinterTransport.bluetooth:
        final mac = channel == PrinterChannelKind.bill
            ? settings.billBluetoothAddress.trim()
            : settings.kotBluetoothAddress.trim();
        if (mac.isEmpty || hub.connectedAddress.isEmpty) return false;
        return hub.connectedAddress.toLowerCase() == mac.toLowerCase();
      case PosPrinterTransport.network:
        return false;
    }
  }

  Widget typeAndSizeBlock({
    required String stringsPaper2,
    required String stringsPaper3,
    required PosPrinterTransport transport,
    required PrinterPaperSize paperSize,
    required String statusLine,
    required bool connected,
    required ValueChanged<PosPrinterTransport> onTransport,
    required ValueChanged<PrinterPaperSize> onPaper,
    required VoidCallback onConnect,
    required VoidCallback onDisconnect,
  }) {
    final type = localTransport(transport);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Printer type',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 8),
        SegmentedButton<PosPrinterTransport>(
          segments: const [
            ButtonSegment(
              value: PosPrinterTransport.bluetooth,
              icon: Icon(Icons.bluetooth_rounded),
              label: Text('Bluetooth'),
            ),
            ButtonSegment(
              value: PosPrinterTransport.usb,
              icon: Icon(Icons.usb_rounded),
              label: Text('USB'),
            ),
          ],
          selected: {type},
          onSelectionChanged: (v) => onTransport(v.first),
        ),
        const SizedBox(height: 16),
        const Text('Page size', style: TextStyle(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        SegmentedButton<PrinterPaperSize>(
          segments: [
            ButtonSegment(
              value: PrinterPaperSize.inch2,
              label: Text(stringsPaper2),
            ),
            ButtonSegment(
              value: PrinterPaperSize.inch3,
              label: Text(stringsPaper3),
            ),
          ],
          selected: {paperSize},
          onSelectionChanged: (v) => onPaper(v.first),
        ),
        const SizedBox(height: 8),
        Text(
          statusLine.isEmpty
              ? (type == PosPrinterTransport.usb
                    ? 'Tap Connect to pick a USB printer'
                    : 'Tap Connect to pick a Bluetooth printer')
              : statusLine,
          style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
        ),
        const SizedBox(height: 10),
        AppButton(
          label: connected ? 'Disconnect' : 'Connect',
          icon: connected
              ? Icons.link_off_rounded
              : (type == PosPrinterTransport.usb
                    ? Icons.usb_rounded
                    : Icons.bluetooth_rounded),
          variant: connected
              ? AppButtonVariant.outlined
              : AppButtonVariant.primary,
          onPressed: btBusy ? null : (connected ? onDisconnect : onConnect),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(printerSettingsProvider);
    final strings = AppStrings.of(ref);
    final billPicked = billStatusLine(settings);
    final kotPicked = kotStatusLine(settings);

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Printer Details'),
            SizedBox(height: 2),
            Text(
              'Printer, KOT, bill options & terms',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w400,
                color: Colors.white70,
              ),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: ResponsiveScrollShell(
              dashboard: true,
              child: ListView(
                padding: EdgeInsets.fromLTRB(
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  16,
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  24,
                ),
                children: [
                  PrinterSectionCard(
                    accent: AppColors.purple,
                    icon: Icons.print_rounded,
                    title: 'BILL SETTING',
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        typeAndSizeBlock(
                          stringsPaper2: strings.paper2Inch,
                          stringsPaper3: strings.paper3Inch,
                          transport: settings.billTransport,
                          paperSize: settings.paperSize,
                          statusLine: billPicked,
                          connected: isChannelConnected(
                            PrinterChannelKind.bill,
                            settings,
                          ),
                          onTransport: (v) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(billTransport: v));
                          },
                          onPaper: (v) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(paperSize: v));
                          },
                          onConnect: () =>
                              connectChannel(PrinterChannelKind.bill),
                          onDisconnect: () =>
                              disconnectChannel(PrinterChannelKind.bill),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          '$btStatus · $usbStatus',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 14),
                        AppTextField(
                          controller: settingsPageInvoicePrefix,
                          label: 'Sales Invoice Prefix',
                          hint: 'PB',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPageInvoiceTitle,
                          label: 'Invoice title',
                          hint: 'TAX INVOICE',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPageFeed,
                          label: 'Print Feed Lines',
                          keyboardType: TextInputType.number,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  PrinterSectionCard(
                    accent: AppColors.purple,
                    icon: Icons.print_rounded,
                    title: 'KOT SETTING',
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        SettingSwitchTile(
                          title: 'Enable KOT',
                          value: settings.kotEnable,
                          showDivider: false,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(kotEnable: value));
                          },
                        ),
                        const SizedBox(height: 8),
                        typeAndSizeBlock(
                          stringsPaper2: strings.paper2Inch,
                          stringsPaper3: strings.paper3Inch,
                          transport: settings.kotTransport,
                          paperSize: settings.kotPaperSize,
                          statusLine: kotPicked,
                          connected: isChannelConnected(
                            PrinterChannelKind.kot,
                            settings,
                          ),
                          onTransport: (v) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(kotTransport: v));
                          },
                          onPaper: (v) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(kotPaperSize: v));
                          },
                          onConnect: () =>
                              connectChannel(PrinterChannelKind.kot),
                          onDisconnect: () =>
                              disconnectChannel(PrinterChannelKind.kot),
                        ),
                        const SizedBox(height: 14),
                        AppTextField(
                          controller: settingsPageKotPrefix,
                          label: 'KOT Prefix',
                          hint: 'KOT',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPageKotCopies,
                          label: 'KOT Copies',
                          keyboardType: TextInputType.number,
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPageKotFeed,
                          label: 'KOT Print Feed Lines',
                          keyboardType: TextInputType.number,
                        ),
                        const SizedBox(height: 4),
                        SettingSwitchTile(
                          title: 'Auto Print KOT',
                          value: settings.kotAutoPrint,
                          showDivider: true,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(kotAutoPrint: value));
                          },
                        ),
                        SettingSwitchTile(
                          title: 'KOT Preview',
                          value: settings.kotPreview,
                          showDivider: false,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(kotPreview: value));
                          },
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  PrinterSectionCard(
                    accent: AppColors.green,
                    icon: Icons.settings_rounded,
                    title: 'BILL OPTIONS',
                    child: Column(
                      children: [
                        SettingSwitchTile(
                          title: 'Use Logo on Bill',
                          value: settings.logoUse,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(logoUse: value));
                          },
                        ),
                        SettingSwitchTile(
                          title: 'Use Payment QR on Bill',
                          value: settings.paymentUse,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(paymentUse: value));
                          },
                        ),
                        SettingSwitchTile(
                          title: 'Use Customer Details on Bill',
                          value: settings.customerUse,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(settings.copyWith(customerUse: value));
                          },
                        ),
                        SettingSwitchTile(
                          title: 'Product Quantity Update',
                          value: settings.productQuantityUpdate,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(
                                  settings.copyWith(
                                    productQuantityUpdate: value,
                                  ),
                                );
                          },
                        ),
                        SettingSwitchTile(
                          title: 'Duplicate Bill Copy (Invoice List)',
                          value: settings.duplicateBillUse,
                          subtitle:
                              'When ON, bills printed from Invoice List are marked as Duplicate Copy. Does not affect new billing.',
                          showDivider: false,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(
                                  settings.copyWith(duplicateBillUse: value),
                                );
                          },
                        ),
                        const Divider(height: 1, color: AppColors.border),
                        SettingSwitchTile(
                          title: 'Share / print prompt after save',
                          value: settings.autoShareOnSave,
                          showDivider: false,
                          onChanged: (value) {
                            ref
                                .read(printerSettingsProvider.notifier)
                                .update(
                                  settings.copyWith(autoShareOnSave: value),
                                );
                          },
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  PrinterSectionCard(
                    accent: AppColors.primary,
                    icon: Icons.description_outlined,
                    title: 'TERMS & CONDITIONS',
                    child: AppTextField(
                      controller: settingsPageInvoiceTerms,
                      label: 'Invoice Terms & Conditions',
                      hint: 'Invoice Terms & Conditions',
                      maxLines: 4,
                      minLines: 3,
                    ),
                  ),
                  const SizedBox(height: 12),
                  PrinterSectionCard(
                    accent: AppColors.primary,
                    icon: Icons.print_outlined,
                    title: 'PRINT PREVIEW',
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const Text(
                          'Open sample invoice or KOT on the next screen',
                          style: TextStyle(
                            fontSize: 13,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: PreviewActionButton(
                                icon: Icons.receipt_long_rounded,
                                label: 'Invoice Preview',
                                onPressed: btBusy
                                    ? null
                                    : () => openTestPreview(
                                          PrinterChannelKind.bill,
                                        ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: PreviewActionButton(
                                icon: Icons.print_rounded,
                                label: 'KOT Preview',
                                onPressed: btBusy
                                    ? null
                                    : () => openTestPreview(
                                          PrinterChannelKind.kot,
                                        ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: AppButton(
                label: 'UPDATE SETTINGS',
                icon: Icons.save_rounded,
                isLoading: companyBusy,
                onPressed: updateSettings,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class PrinterSectionCard extends StatelessWidget {
  const PrinterSectionCard({
    super.key,
    required this.accent,
    required this.icon,
    required this.title,
    required this.child,
  });

  final Color accent;
  final IconData icon;
  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Icon(icon, size: 18, color: accent),
              const SizedBox(width: 8),
              Text(
                title,
                style: TextStyle(
                  color: accent,
                  fontWeight: FontWeight.w800,
                  fontSize: 12,
                  letterSpacing: 0.7,
                ),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Divider(
                  height: 1,
                  thickness: 1,
                  color: AppColors.border,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }
}

class PreviewActionButton extends StatelessWidget {
  const PreviewActionButton({
    super.key,
    required this.icon,
    required this.label,
    required this.onPressed,
  });

  final IconData icon;
  final String label;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.primaryLight,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onPressed,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 10),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 18, color: AppColors.primary),
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  label,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w700,
                    fontSize: 13,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/* Kept for call-site compatibility — same as [AppSwitchTile]. */
typedef SettingSwitchTile = AppSwitchTile;

