import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/permissions/app_permission_service.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/company/data/company_api.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/sample_receipt_data.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_device_picker_page.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

class SettingsPage extends ConsumerStatefulWidget {
  const SettingsPage({super.key});

  @override
  ConsumerState<SettingsPage> createState() => SettingsPageState();
}

class SettingsPageState extends ConsumerState<SettingsPage> {
  static const settingsPagePermissions = AppPermissionService();

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
  bool permissionBusy = false;
  bool btBusy = false;
  String btStatus = 'Checkingâ€¦';
  String usbStatus = 'USB idle';
  Map<Permission, PermissionStatus> permissionStatuses = const {};
  final hub = BluetoothPrinterHub.instance;
  final usbHub = EscPosTransportHub.instance;

  @override
  void initState() {
    super.initState();
    final settings = ref.read(printerSettingsProvider);
    settingsPageBillMac = TextEditingController(text: settings.billBluetoothAddress);
    settingsPageKotMac = TextEditingController(text: settings.kotBluetoothAddress);
    host = TextEditingController(text: settings.networkHost);
    settingsPagePort = TextEditingController(text: '${settings.networkPort}');
    settingsPageFeed = TextEditingController(text: '${settings.feedLines}');
    settingsPageKotFeed = TextEditingController(text: '${settings.kotFeedLines}');
    settingsPageInvoiceTitle = TextEditingController(text: settings.invoiceTitle);
    settingsPageInvoiceTerms = TextEditingController(text: settings.invoiceTerms);
    settingsPageInvoicePrefix = TextEditingController(text: settings.invoicePrefix);
    settingsPageKotPrefix = TextEditingController(text: settings.kotPrefix);
    settingsPageKotCopies = TextEditingController(text: '${settings.kotCopies}');
    Future.microtask(() async {
      await loadPrinterCloud();
      await refreshPermissions();
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
    usbHub.updateSavedUsb(
      identifier: s.billUsbIdentifier,
      name: s.billUsbName,
    );
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

  Future<void> pickPrinter(PrinterChannelKind channel) async {
    final settings = ref.read(printerSettingsProvider);
    final initial = channel == PrinterChannelKind.bill
        ? settings.billTransport
        : settings.kotTransport;
    final picked = await Navigator.of(context).push<PickedPrinter>(
      MaterialPageRoute(
        builder: (_) => PrinterDevicePickerPage(
          channel: channel,
          initialTransport: initial,
        ),
      ),
    );
    if (picked == null || !mounted) return;

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
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          '${picked.transport.label} printer selected',
        ),
      ),
    );
  }

  Future<void> connectChannel(PrinterChannelKind channel) async {
    setState(() => btBusy = true);
    try {
      await settingsPageSave(showSnack: false);
      final settings = ref.read(printerSettingsProvider);
      final transport = channel == PrinterChannelKind.bill
          ? settings.billTransport
          : settings.kotTransport;

      var ok = false;
      switch (transport) {
        case PosPrinterTransport.bluetooth:
          final mac = channel == PrinterChannelKind.bill
              ? settingsPageBillMac.text.trim()
              : settingsPageKotMac.text.trim();
          if (mac.isEmpty) {
            await pickPrinter(channel);
            return;
          }
          ok = await hub.connect(channel, address: mac, fromUser: true);
        case PosPrinterTransport.usb:
          final id = settings.usbIdFor(
            isKot: channel == PrinterChannelKind.kot,
          );
          if (id.isEmpty) {
            await pickPrinter(channel);
            return;
          }
          ok = await usbHub.connectUsb(
            identifier: id,
            name: settings.usbNameFor(
              isKot: channel == PrinterChannelKind.kot,
            ),
          );
        case PosPrinterTransport.network:
          ok = settings.networkHost.trim().isNotEmpty;
      }

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            ok
                ? '${transport.label} printer ready'
                : 'Connect failed â€” pick a printer',
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
      final transport = channel == PrinterChannelKind.bill
          ? settings.billTransport
          : settings.kotTransport;
      if (transport == PosPrinterTransport.usb) {
        await usbHub.clearSavedUsb();
      } else {
        await hub.disconnect(channel);
      }
      if (channel == PrinterChannelKind.bill) {
        settingsPageBillMac.clear();
        await ref.read(printerSettingsProvider.notifier).update(
              settings.copyWith(
                billBluetoothAddress: '',
                billUsbIdentifier: '',
                billUsbName: '',
              ),
            );
      } else {
        settingsPageKotMac.clear();
        await ref.read(printerSettingsProvider.notifier).update(
              settings.copyWith(
                kotBluetoothAddress: '',
                kotUsbIdentifier: '',
                kotUsbName: '',
              ),
            );
      }
      await refreshBtStatus();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Printer disconnected')),
      );
    } finally {
      if (mounted) setState(() => btBusy = false);
    }
  }

  Future<void> openTestPreview(PrinterChannelKind channel) async {
    await settingsPageSave(showSnack: false);
    if (!mounted) return;
    final mode =
        channel == PrinterChannelKind.kot ? 'kot' : 'invoice';
    await context.push('/settings/test-print?mode=$mode');
    if (!mounted) return;
    await refreshBtStatus();
  }

  Future<void> refreshPermissions() async {
    final statuses = await settingsPagePermissions.checkAll();
    if (!mounted) return;
    setState(() => permissionStatuses = statuses);
  }

  Future<void> requestPermissions() async {
    setState(() => permissionBusy = true);
    try {
      final statuses = await settingsPagePermissions.requestAll();
      final blocked = statuses.values.any((s) => s.isPermanentlyDenied);
      if (!mounted) return;
      setState(() {
        permissionStatuses = statuses;
        permissionBusy = false;
      });
      if (blocked) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text(
              'Some permissions are blocked. Open system settings to enable them.',
            ),
            action: SnackBarAction(
              label: 'Open',
              onPressed: settingsPagePermissions.openAppSettingsPage,
            ),
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Permissions updated')),
        );
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => permissionBusy = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Permission request failed: $e')),
      );
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
          billBluetoothAddress: p.bluetoothAddress.isNotEmpty
              ? p.bluetoothAddress
              : current.billBluetoothAddress,
          kotBluetoothAddress: p.bluetoothKotAddress.isNotEmpty
              ? p.bluetoothKotAddress
              : current.kotBluetoothAddress,
          feedLines: int.tryParse(p.printerFeedLines) ?? current.feedLines,
          kotFeedLines:
              int.tryParse(p.kotPrinterFeedLines) ?? current.kotFeedLines,
          invoiceTitle: p.invoiceTitle.isNotEmpty
              ? p.invoiceTitle
              : current.invoiceTitle,
          invoiceTerms: p.invoiceTermsCondition.isNotEmpty
              ? p.invoiceTermsCondition
              : current.invoiceTerms,
          customerUse: p.customerUse == '1',
          paymentUse: p.paymentUse == '1',
          duplicateBillUse: p.duplicateBillUse == '1',
          logoUse: p.logoUse == '1',
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
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please login first')),
      );
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
        customerUse: settings.customerUse ? '1' : '0',
        paymentUse: settings.paymentUse ? '1' : '0',
        duplicateBillUse: settings.duplicateBillUse ? '1' : '0',
        logoUse: settings.logoUse ? '1' : '0',
        kotEnable: settings.kotEnable ? '1' : '0',
        productQuantityUpdate: settings.productQuantityUpdate ? '1' : '0',
        kotAutoPrint: settings.kotAutoPrint ? '1' : '0',
        kotPreview: settings.kotPreview ? '1' : '0',
        kotCopies: '${settings.kotCopies}',
      );
      await ref
          .read(appDatabaseProvider)
          .upsertLocalCompanyPrinterSettings(printerDto);
      final printerOk = await api.insertCompanyPrinterSetting(
        userId: userId,
        setting: printerDto,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            printerOk
                ? 'Printer settings saved to cloud'
                : 'Cloud save finished with issues',
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e')),
      );
    } finally {
      if (mounted) setState(() => companyBusy = false);
    }
  }

  Future<void> settingsPageSave({bool showSnack = true}) async {
    final current = ref.read(printerSettingsProvider);
    final feed = int.tryParse(settingsPageFeed.text.trim()) ?? current.feedLines;
    final kotFeed = int.tryParse(settingsPageKotFeed.text.trim()) ?? current.kotFeedLines;
    final port = int.tryParse(settingsPagePort.text.trim()) ?? current.networkPort;
    final copies = int.tryParse(settingsPageKotCopies.text.trim()) ?? current.kotCopies;
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
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Printer settings saved')),
      );
    }
  }

  String settingsPagePaperLabel(PrinterPaperSize size) =>
      size == PrinterPaperSize.inch3 ? '3-Inch' : '2-Inch';

  PrinterPaperSize paperFromLabel(String? label) =>
      label == '3-Inch' ? PrinterPaperSize.inch3 : PrinterPaperSize.inch2;

  Future<void> updateSettings() async {
    await savePrinterCloud();
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

  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(printerSettingsProvider);
    final paperLabel = settingsPagePaperLabel(settings.paperSize);

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
            24),
              children: [
                AppCard(
                  padding: EdgeInsets.zero,
                  child: ListTile(
                    leading: const Icon(
                      Icons.storefront_rounded,
                      color: AppColors.primary,
                    ),
                    title: const Text(
                      'Shop Details',
                      style: TextStyle(fontWeight: FontWeight.w800),
                    ),
                    subtitle: const Text('Company profile used on bills'),
                    trailing: const Icon(Icons.chevron_right_rounded),
                    onTap: () => context.push('/settings/company'),
                  ),
                ),
                const SizedBox(height: 12),
                PrinterSectionCard(
                  accent: AppColors.purple,
                  icon: Icons.print_rounded,
                  title: 'PRINTER SETTING',
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      PrinterConnectRow(
                        label: 'Bill Printer Name',
                        value: paperLabel,
                        busy: btBusy,
                        onPaperChanged: (v) {
                          if (v == null) return;
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(
                                  paperSize: paperFromLabel(v),
                                ),
                              );
                        },
                        onConnect: () =>
                            connectChannel(PrinterChannelKind.bill),
                        onPick: () => pickPrinter(PrinterChannelKind.bill),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        billStatusLine(settings),
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
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(kotEnable: value),
                              );
                        },
                      ),
                      const SizedBox(height: 8),
                      PrinterConnectRow(
                        label: 'KOT Print Printer Name',
                        value: paperLabel,
                        busy: btBusy,
                        onPaperChanged: (v) {
                          if (v == null) return;
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(
                                  paperSize: paperFromLabel(v),
                                ),
                              );
                        },
                        onConnect: () =>
                            connectChannel(PrinterChannelKind.kot),
                        onPick: () => pickPrinter(PrinterChannelKind.kot),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        kotStatusLine(settings),
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
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
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(kotAutoPrint: value),
                              );
                        },
                      ),
                      SettingSwitchTile(
                        title: 'KOT Preview',
                        value: settings.kotPreview,
                        showDivider: false,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(kotPreview: value),
                              );
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
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(logoUse: value),
                              );
                        },
                      ),
                      SettingSwitchTile(
                        title: 'Use Payment QR on Bill',
                        value: settings.paymentUse,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(paymentUse: value),
                              );
                        },
                      ),
                      SettingSwitchTile(
                        title: 'Use Customer Details on Bill',
                        value: settings.customerUse,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(customerUse: value),
                              );
                        },
                      ),
                      SettingSwitchTile(
                        title: 'Product Quantity Update',
                        value: settings.productQuantityUpdate,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
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
                          ref.read(printerSettingsProvider.notifier).update(
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
                          ref.read(printerSettingsProvider.notifier).update(
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
                        'Test sample invoice or KOT on your printer',
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
                      const SizedBox(height: 14),
                      const LivePaperPreviews(),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Theme(
                  data: Theme.of(context)
                      .copyWith(dividerColor: Colors.transparent),
                  child: AppCard(
                    child: ExpansionTile(
                      tilePadding: EdgeInsets.zero,
                      childrenPadding: EdgeInsets.zero,
                      initiallyExpanded: false,
                      title: const Text(
                        'Connection & permissions',
                        style: TextStyle(fontWeight: FontWeight.w800),
                      ),
                      subtitle: Text(
                        '$btStatus · $usbStatus',
                        style: const TextStyle(fontSize: 12),
                      ),
                      children: [
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          dense: true,
                          leading: AppSvgIconChip(
                            assetPath: AppAssets.svgPrint,
                            color: AppColors.primary,
                            size: 40,
                          ),
                          title: const Text(
                            'Status',
                            style: TextStyle(fontWeight: FontWeight.w700),
                          ),
                          subtitle: Text('$btStatus\n$usbStatus'),
                          trailing: IconButton(
                            tooltip: 'Refresh',
                            onPressed: btBusy ? null : refreshBtStatus,
                            icon: const AppSvg(
                              AppAssets.svgRefresh,
                              width: 20,
                              height: 20,
                              color: AppColors.primary,
                            ),
                          ),
                        ),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            AppButton(
                              label: 'Pick bill printer',
                              icon: Icons.devices_rounded,
                              expanded: false,
                              onPressed: btBusy
                                  ? null
                                  : () =>
                                      pickPrinter(PrinterChannelKind.bill),
                            ),
                            AppButton(
                              label: 'Pick KOT printer',
                              icon: Icons.devices_rounded,
                              variant: AppButtonVariant.outlined,
                              expanded: false,
                              onPressed: btBusy
                                  ? null
                                  : () =>
                                      pickPrinter(PrinterChannelKind.kot),
                            ),
                            TextButton(
                              onPressed: btBusy
                                  ? null
                                  : () => disconnectChannel(
                                        PrinterChannelKind.bill,
                                      ),
                              child: const Text('Disconnect bill'),
                            ),
                            TextButton(
                              onPressed: btBusy
                                  ? null
                                  : () => disconnectChannel(
                                        PrinterChannelKind.kot,
                                      ),
                              child: const Text('Disconnect KOT'),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPageBillMac,
                          label: 'Bill Bluetooth MAC (optional)',
                          hint: 'AA:BB:CC:DD:EE:FF',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPageKotMac,
                          label: 'KOT Bluetooth MAC (optional)',
                          hint: 'Uses bill printer if empty',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: host,
                          label: 'Network printer IP (optional)',
                          hint: '192.168.1.50',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPagePort,
                          label: 'Network port',
                          hint: '9100',
                          keyboardType: TextInputType.number,
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: settingsPageInvoiceTitle,
                          label: 'Invoice title',
                          hint: 'TAX INVOICE',
                        ),
                        const SizedBox(height: 12),
                        if (permissionStatuses.isEmpty)
                          const Padding(
                            padding: EdgeInsets.symmetric(vertical: 8),
                            child: Text('Checking permissions…'),
                          )
                        else
                          ...permissionStatuses.entries.map((entry) {
                            final allowed = entry.value.isGranted ||
                                entry.value.isLimited ||
                                entry.value.isProvisional;
                            return ListTile(
                              contentPadding: EdgeInsets.zero,
                              dense: true,
                              leading: Icon(
                                allowed
                                    ? Icons.check_circle_rounded
                                    : Icons.error_outline_rounded,
                                color: allowed
                                    ? AppColors.success
                                    : AppColors.warning,
                              ),
                              title: Text(settingsPagePermissions.labelFor(entry.key)),
                              trailing: Text(
                                settingsPagePermissions.statusLabel(entry.value),
                                style: TextStyle(
                                  fontWeight: FontWeight.w600,
                                  color: allowed
                                      ? AppColors.success
                                      : AppColors.warning,
                                ),
                              ),
                            );
                          }),
                        AppButton(
                          label: 'Allow location, camera, Bluetooth',
                          icon: Icons.security_rounded,
                          isLoading: permissionBusy,
                          onPressed: requestPermissions,
                        ),
                        TextButton(
                          onPressed: settingsPagePermissions.openAppSettingsPage,
                          child: const Text('Open system app settings'),
                        ),
                        AppButton(
                          label: 'Load from cloud',
                          icon: Icons.cloud_download_rounded,
                          variant: AppButtonVariant.outlined,
                          isLoading: companyBusy,
                          onPressed: loadPrinterCloud,
                        ),
                      ],
                    ),
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
  const PrinterSectionCard({super.key, 
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

class PrinterConnectRow extends StatelessWidget {
  const PrinterConnectRow({super.key, 
    required this.label,
    required this.value,
    required this.busy,
    required this.onPaperChanged,
    required this.onConnect,
    required this.onPick,
  });

  final String label;
  final String value;
  final bool busy;
  final ValueChanged<String?> onPaperChanged;
  final VoidCallback onConnect;
  final VoidCallback onPick;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: GestureDetector(
            onLongPress: busy ? null : onPick,
            child: StringDropdownField(
              label: label,
              value: value,
              options: const ['2-Inch', '3-Inch'],
              enableSearch: false,
              onChanged: onPaperChanged,
            ),
          ),
        ),
        const SizedBox(width: 10),
        Padding(
          padding: const EdgeInsets.only(top: 4),
          child: FilledButton(
            onPressed: busy ? null : onConnect,
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              elevation: 0,
              padding:
                  const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            child: const Text(
              'Connect',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ),
      ],
    );
  }
}

class SettingSwitchTile extends StatelessWidget {
  const SettingSwitchTile({super.key, 
    required this.title,
    required this.value,
    required this.onChanged,
    this.subtitle,
    this.showDivider = true,
  });

  final String title;
  final String? subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          title: Text(
            title,
            style: const TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 15,
              color: AppColors.textPrimary,
            ),
          ),
          subtitle: subtitle == null
              ? null
              : Text(
                  subtitle!,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.textSecondary,
                    height: 1.35,
                  ),
                ),
          value: value,
          activeThumbColor: Colors.white,
          activeTrackColor: AppColors.green,
          onChanged: onChanged,
        ),
        if (showDivider)
          const Divider(height: 1, thickness: 1, color: AppColors.border),
      ],
    );
  }
}

class PreviewActionButton extends StatelessWidget {
  const PreviewActionButton({super.key, 
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

class LivePaperPreviews extends ConsumerWidget {
  const LivePaperPreviews({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    final service = ref.watch(printServiceProvider);
    final sample = SampleReceiptData.sampleBill();
    final shop = ref.watch(authControllerProvider).session?.shopName;
    final inch2 = service.billPreviewText(
      invoice: sample.invoice,
      items: sample.items,
      shopName: shop,
      paperSize: PrinterPaperSize.inch2,
    );
    final inch3 = service.billPreviewText(
      invoice: sample.invoice,
      items: sample.items,
      shopName: shop,
      paperSize: PrinterPaperSize.inch3,
    );
    final kot = service.previewText(PrinterChannelKind.kot, shopName: shop);
    Widget card(String title, String text) {
      return Container(
        width: double.infinity,
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: const Color(0xFFE2E8F2)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
            const SizedBox(height: 6),
            SelectableText(
              text,
              style: const TextStyle(
                fontFamily: 'monospace',
                fontSize: 10,
                height: 1.3,
              ),
            ),
          ],
        ),
      );
    }

    return Column(
      children: [
        card(strings.paper2Inch, inch2),
        card(strings.paper3Inch, inch3),
        card(strings.sendKot, kot),
      ],
    );
  }
}


