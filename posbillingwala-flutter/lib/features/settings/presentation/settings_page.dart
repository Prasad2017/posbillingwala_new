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
  ConsumerState<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends ConsumerState<SettingsPage> {
  static const _permissions = AppPermissionService();

  late final TextEditingController _billMac;
  late final TextEditingController _kotMac;
  late final TextEditingController _host;
  late final TextEditingController _port;
  late final TextEditingController _feed;
  late final TextEditingController _kotFeed;
  late final TextEditingController _invoiceTitle;
  late final TextEditingController _invoiceTerms;
  late final TextEditingController _invoicePrefix;
  late final TextEditingController _kotPrefix;
  late final TextEditingController _kotCopies;
  bool _companyBusy = false;
  bool _permissionBusy = false;
  bool _btBusy = false;
  String _btStatus = 'Checkingâ€¦';
  String _usbStatus = 'USB idle';
  Map<Permission, PermissionStatus> _permissionStatuses = const {};
  final _hub = BluetoothPrinterHub.instance;
  final _usbHub = EscPosTransportHub.instance;

  @override
  void initState() {
    super.initState();
    final settings = ref.read(printerSettingsProvider);
    _billMac = TextEditingController(text: settings.billBluetoothAddress);
    _kotMac = TextEditingController(text: settings.kotBluetoothAddress);
    _host = TextEditingController(text: settings.networkHost);
    _port = TextEditingController(text: '${settings.networkPort}');
    _feed = TextEditingController(text: '${settings.feedLines}');
    _kotFeed = TextEditingController(text: '${settings.kotFeedLines}');
    _invoiceTitle = TextEditingController(text: settings.invoiceTitle);
    _invoiceTerms = TextEditingController(text: settings.invoiceTerms);
    _invoicePrefix = TextEditingController(text: settings.invoicePrefix);
    _kotPrefix = TextEditingController(text: settings.kotPrefix);
    _kotCopies = TextEditingController(text: '${settings.kotCopies}');
    Future.microtask(() async {
      await _loadPrinterCloud();
      await _refreshPermissions();
      await _syncHubAndAutoConnect();
      await _refreshBtStatus();
    });
  }

  Future<void> _syncHubAndAutoConnect() async {
    final s = ref.read(printerSettingsProvider);
    _hub.updateSavedAddresses(
      billMac: s.billBluetoothAddress,
      kotMac: s.kotBluetoothAddress,
    );
    _usbHub.updateSavedUsb(
      identifier: s.billUsbIdentifier,
      name: s.billUsbName,
    );
    if (s.billTransport == PosPrinterTransport.bluetooth) {
      await _hub.autoConnect(PrinterChannelKind.bill);
    } else if (s.billTransport == PosPrinterTransport.usb &&
        s.billUsbIdentifier.isNotEmpty) {
      await _usbHub.connectUsb(
        identifier: s.billUsbIdentifier,
        name: s.billUsbName,
      );
    }
    if (s.kotTransport == PosPrinterTransport.bluetooth &&
        s.kotBluetoothAddress.trim().isNotEmpty &&
        s.kotBluetoothAddress.trim().toLowerCase() !=
            s.billBluetoothAddress.trim().toLowerCase()) {
      await _hub.autoConnect(PrinterChannelKind.kot);
    }
  }

  Future<void> _refreshBtStatus() async {
    final on = await _hub.isBluetoothOn();
    final linked = await _hub.connectionStatus();
    final usbLinked = _usbHub.isConnected;
    if (!mounted) return;
    setState(() {
      if (!on) {
        _btStatus = 'Bluetooth off';
      } else if (linked && _hub.connectedAddress.isNotEmpty) {
        _btStatus = 'BT connected: ${_hub.connectedAddress}';
      } else if (_hub.isConnecting) {
        _btStatus = 'Connectingâ€¦';
      } else {
        _btStatus = 'Bluetooth not connected';
      }
      if (usbLinked && _usbHub.savedUsbId.isNotEmpty) {
        _usbStatus =
            'USB connected: ${_usbHub.savedUsbName.isEmpty ? _usbHub.savedUsbId : _usbHub.savedUsbName}';
      } else if (_usbHub.savedUsbId.isNotEmpty) {
        _usbStatus = 'USB saved: ${_usbHub.savedUsbId}';
      } else {
        _usbStatus = 'USB not configured';
      }
    });
  }

  Future<void> _pickPrinter(PrinterChannelKind channel) async {
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
        _billMac.text = picked.bluetoothMac;
      }
      if (picked.transport == PosPrinterTransport.network) {
        _host.text = picked.networkHost;
        _port.text = '${picked.networkPort}';
      }
    } else {
      if (picked.transport == PosPrinterTransport.bluetooth) {
        _kotMac.text = picked.bluetoothMac;
      }
      if (picked.transport == PosPrinterTransport.network) {
        _host.text = picked.networkHost;
        _port.text = '${picked.networkPort}';
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
    _hub.updateSavedAddresses(
      billMac: updated.billBluetoothAddress,
      kotMac: updated.kotBluetoothAddress,
    );
    _usbHub.updateSavedUsb(
      identifier: updated.usbIdFor(isKot: channel == PrinterChannelKind.kot),
      name: updated.usbNameFor(isKot: channel == PrinterChannelKind.kot),
    );
    await _refreshBtStatus();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          '${picked.transport.label} printer selected',
        ),
      ),
    );
  }

  Future<void> _connectChannel(PrinterChannelKind channel) async {
    setState(() => _btBusy = true);
    try {
      await _save(showSnack: false);
      final settings = ref.read(printerSettingsProvider);
      final transport = channel == PrinterChannelKind.bill
          ? settings.billTransport
          : settings.kotTransport;

      var ok = false;
      switch (transport) {
        case PosPrinterTransport.bluetooth:
          final mac = channel == PrinterChannelKind.bill
              ? _billMac.text.trim()
              : _kotMac.text.trim();
          if (mac.isEmpty) {
            await _pickPrinter(channel);
            return;
          }
          ok = await _hub.connect(channel, address: mac, fromUser: true);
        case PosPrinterTransport.usb:
          final id = settings.usbIdFor(
            isKot: channel == PrinterChannelKind.kot,
          );
          if (id.isEmpty) {
            await _pickPrinter(channel);
            return;
          }
          ok = await _usbHub.connectUsb(
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
      if (!ok) await _pickPrinter(channel);
      await _refreshBtStatus();
    } finally {
      if (mounted) setState(() => _btBusy = false);
    }
  }

  Future<void> _disconnectChannel(PrinterChannelKind channel) async {
    setState(() => _btBusy = true);
    try {
      final settings = ref.read(printerSettingsProvider);
      final transport = channel == PrinterChannelKind.bill
          ? settings.billTransport
          : settings.kotTransport;
      if (transport == PosPrinterTransport.usb) {
        await _usbHub.clearSavedUsb();
      } else {
        await _hub.disconnect(channel);
      }
      if (channel == PrinterChannelKind.bill) {
        _billMac.clear();
        await ref.read(printerSettingsProvider.notifier).update(
              settings.copyWith(
                billBluetoothAddress: '',
                billUsbIdentifier: '',
                billUsbName: '',
              ),
            );
      } else {
        _kotMac.clear();
        await ref.read(printerSettingsProvider.notifier).update(
              settings.copyWith(
                kotBluetoothAddress: '',
                kotUsbIdentifier: '',
                kotUsbName: '',
              ),
            );
      }
      await _refreshBtStatus();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Printer disconnected')),
      );
    } finally {
      if (mounted) setState(() => _btBusy = false);
    }
  }

  Future<void> _openTestPreview(PrinterChannelKind channel) async {
    await _save(showSnack: false);
    if (!mounted) return;
    final mode =
        channel == PrinterChannelKind.kot ? 'kot' : 'invoice';
    await context.push('/settings/test-print?mode=$mode');
    if (!mounted) return;
    await _refreshBtStatus();
  }

  Future<void> _refreshPermissions() async {
    final statuses = await _permissions.checkAll();
    if (!mounted) return;
    setState(() => _permissionStatuses = statuses);
  }

  Future<void> _requestPermissions() async {
    setState(() => _permissionBusy = true);
    try {
      final statuses = await _permissions.requestAll();
      final blocked = statuses.values.any((s) => s.isPermanentlyDenied);
      if (!mounted) return;
      setState(() {
        _permissionStatuses = statuses;
        _permissionBusy = false;
      });
      if (blocked) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text(
              'Some permissions are blocked. Open system settings to enable them.',
            ),
            action: SnackBarAction(
              label: 'Open',
              onPressed: _permissions.openAppSettingsPage,
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
      setState(() => _permissionBusy = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Permission request failed: $e')),
      );
    }
  }

  @override
  void dispose() {
    _billMac.dispose();
    _kotMac.dispose();
    _host.dispose();
    _port.dispose();
    _feed.dispose();
    _kotFeed.dispose();
    _invoiceTitle.dispose();
    _invoiceTerms.dispose();
    _invoicePrefix.dispose();
    _kotPrefix.dispose();
    _kotCopies.dispose();
    super.dispose();
  }

  Future<void> _loadPrinterCloud() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    setState(() => _companyBusy = true);
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
        _billMac.text = updated.billBluetoothAddress;
        _kotMac.text = updated.kotBluetoothAddress;
        _feed.text = '${updated.feedLines}';
        _kotFeed.text = '${updated.kotFeedLines}';
        _invoiceTitle.text = updated.invoiceTitle;
        _invoiceTerms.text = updated.invoiceTerms;
        _invoicePrefix.text = updated.invoicePrefix;
        _kotPrefix.text = updated.kotPrefix;
        _kotCopies.text = '${updated.kotCopies}';
        _hub.updateSavedAddresses(
          billMac: updated.billBluetoothAddress,
          kotMac: updated.kotBluetoothAddress,
        );
        await _hub.autoConnect(PrinterChannelKind.bill);
      }
    } catch (_) {
      // Keep local fields if cloud load fails.
    } finally {
      if (mounted) setState(() => _companyBusy = false);
    }
  }

  Future<void> _savePrinterCloud() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please login first')),
      );
      return;
    }
    setState(() => _companyBusy = true);
    try {
      final api = CompanyApi(ref.read(apiClientProvider));
      await _save(showSnack: false);
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
      if (mounted) setState(() => _companyBusy = false);
    }
  }

  Future<void> _save({bool showSnack = true}) async {
    final current = ref.read(printerSettingsProvider);
    final feed = int.tryParse(_feed.text.trim()) ?? current.feedLines;
    final kotFeed = int.tryParse(_kotFeed.text.trim()) ?? current.kotFeedLines;
    final port = int.tryParse(_port.text.trim()) ?? current.networkPort;
    final copies = int.tryParse(_kotCopies.text.trim()) ?? current.kotCopies;
    final updated = current.copyWith(
      billBluetoothAddress: _billMac.text.trim(),
      kotBluetoothAddress: _kotMac.text.trim(),
      networkHost: _host.text.trim(),
      networkPort: port.clamp(1, 65535),
      feedLines: feed.clamp(1, 10),
      kotFeedLines: kotFeed.clamp(1, 10),
      invoiceTitle: _invoiceTitle.text.trim(),
      invoiceTerms: _invoiceTerms.text.trim(),
      invoicePrefix: _invoicePrefix.text.trim().isEmpty
          ? 'PB'
          : _invoicePrefix.text.trim(),
      kotPrefix: _kotPrefix.text.trim().isEmpty
          ? 'KOT'
          : _kotPrefix.text.trim(),
      kotCopies: copies.clamp(1, 5),
    );
    await ref.read(printerSettingsProvider.notifier).update(updated);
    _hub.updateSavedAddresses(
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

  String _paperLabel(PrinterPaperSize size) =>
      size == PrinterPaperSize.inch3 ? '3-Inch' : '2-Inch';

  PrinterPaperSize _paperFromLabel(String? label) =>
      label == '3-Inch' ? PrinterPaperSize.inch3 : PrinterPaperSize.inch2;

  Future<void> _updateSettings() async {
    await _savePrinterCloud();
  }

  String _billStatusLine(PrinterSettings settings) {
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

  String _kotStatusLine(PrinterSettings settings) {
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
    final paperLabel = _paperLabel(settings.paperSize);

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
                _PrinterSectionCard(
                  accent: AppColors.purple,
                  icon: Icons.print_rounded,
                  title: 'PRINTER SETTING',
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _PrinterConnectRow(
                        label: 'Bill Printer Name',
                        value: paperLabel,
                        busy: _btBusy,
                        onPaperChanged: (v) {
                          if (v == null) return;
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(
                                  paperSize: _paperFromLabel(v),
                                ),
                              );
                        },
                        onConnect: () =>
                            _connectChannel(PrinterChannelKind.bill),
                        onPick: () => _pickPrinter(PrinterChannelKind.bill),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        _billStatusLine(settings),
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 14),
                      AppTextField(
                        controller: _invoicePrefix,
                        label: 'Sales Invoice Prefix',
                        hint: 'PB',
                      ),
                      const SizedBox(height: 12),
                      AppTextField(
                        controller: _feed,
                        label: 'Print Feed Lines',
                        keyboardType: TextInputType.number,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                _PrinterSectionCard(
                  accent: AppColors.purple,
                  icon: Icons.print_rounded,
                  title: 'KOT SETTING',
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _SettingSwitchTile(
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
                      _PrinterConnectRow(
                        label: 'KOT Print Printer Name',
                        value: paperLabel,
                        busy: _btBusy,
                        onPaperChanged: (v) {
                          if (v == null) return;
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(
                                  paperSize: _paperFromLabel(v),
                                ),
                              );
                        },
                        onConnect: () =>
                            _connectChannel(PrinterChannelKind.kot),
                        onPick: () => _pickPrinter(PrinterChannelKind.kot),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        _kotStatusLine(settings),
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 14),
                      AppTextField(
                        controller: _kotPrefix,
                        label: 'KOT Prefix',
                        hint: 'KOT',
                      ),
                      const SizedBox(height: 12),
                      AppTextField(
                        controller: _kotCopies,
                        label: 'KOT Copies',
                        keyboardType: TextInputType.number,
                      ),
                      const SizedBox(height: 12),
                      AppTextField(
                        controller: _kotFeed,
                        label: 'KOT Print Feed Lines',
                        keyboardType: TextInputType.number,
                      ),
                      const SizedBox(height: 4),
                      _SettingSwitchTile(
                        title: 'Auto Print KOT',
                        value: settings.kotAutoPrint,
                        showDivider: true,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(kotAutoPrint: value),
                              );
                        },
                      ),
                      _SettingSwitchTile(
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
                _PrinterSectionCard(
                  accent: AppColors.green,
                  icon: Icons.settings_rounded,
                  title: 'BILL OPTIONS',
                  child: Column(
                    children: [
                      _SettingSwitchTile(
                        title: 'Use Logo on Bill',
                        value: settings.logoUse,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(logoUse: value),
                              );
                        },
                      ),
                      _SettingSwitchTile(
                        title: 'Use Payment QR on Bill',
                        value: settings.paymentUse,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(paymentUse: value),
                              );
                        },
                      ),
                      _SettingSwitchTile(
                        title: 'Use Customer Details on Bill',
                        value: settings.customerUse,
                        onChanged: (value) {
                          ref.read(printerSettingsProvider.notifier).update(
                                settings.copyWith(customerUse: value),
                              );
                        },
                      ),
                      _SettingSwitchTile(
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
                      _SettingSwitchTile(
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
                      _SettingSwitchTile(
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
                _PrinterSectionCard(
                  accent: AppColors.primary,
                  icon: Icons.description_outlined,
                  title: 'TERMS & CONDITIONS',
                  child: AppTextField(
                    controller: _invoiceTerms,
                    label: 'Invoice Terms & Conditions',
                    hint: 'Invoice Terms & Conditions',
                    maxLines: 4,
                    minLines: 3,
                  ),
                ),
                const SizedBox(height: 12),
                _PrinterSectionCard(
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
                            child: _PreviewActionButton(
                              icon: Icons.receipt_long_rounded,
                              label: 'Invoice Preview',
                              onPressed: _btBusy
                                  ? null
                                  : () => _openTestPreview(
                                        PrinterChannelKind.bill,
                                      ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: _PreviewActionButton(
                              icon: Icons.print_rounded,
                              label: 'KOT Preview',
                              onPressed: _btBusy
                                  ? null
                                  : () => _openTestPreview(
                                        PrinterChannelKind.kot,
                                      ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      const _LivePaperPreviews(),
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
                        '$_btStatus · $_usbStatus',
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
                          subtitle: Text('$_btStatus\n$_usbStatus'),
                          trailing: IconButton(
                            tooltip: 'Refresh',
                            onPressed: _btBusy ? null : _refreshBtStatus,
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
                              onPressed: _btBusy
                                  ? null
                                  : () =>
                                      _pickPrinter(PrinterChannelKind.bill),
                            ),
                            AppButton(
                              label: 'Pick KOT printer',
                              icon: Icons.devices_rounded,
                              variant: AppButtonVariant.outlined,
                              expanded: false,
                              onPressed: _btBusy
                                  ? null
                                  : () =>
                                      _pickPrinter(PrinterChannelKind.kot),
                            ),
                            TextButton(
                              onPressed: _btBusy
                                  ? null
                                  : () => _disconnectChannel(
                                        PrinterChannelKind.bill,
                                      ),
                              child: const Text('Disconnect bill'),
                            ),
                            TextButton(
                              onPressed: _btBusy
                                  ? null
                                  : () => _disconnectChannel(
                                        PrinterChannelKind.kot,
                                      ),
                              child: const Text('Disconnect KOT'),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _billMac,
                          label: 'Bill Bluetooth MAC (optional)',
                          hint: 'AA:BB:CC:DD:EE:FF',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _kotMac,
                          label: 'KOT Bluetooth MAC (optional)',
                          hint: 'Uses bill printer if empty',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _host,
                          label: 'Network printer IP (optional)',
                          hint: '192.168.1.50',
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _port,
                          label: 'Network port',
                          hint: '9100',
                          keyboardType: TextInputType.number,
                        ),
                        const SizedBox(height: 12),
                        AppTextField(
                          controller: _invoiceTitle,
                          label: 'Invoice title',
                          hint: 'TAX INVOICE',
                        ),
                        const SizedBox(height: 12),
                        if (_permissionStatuses.isEmpty)
                          const Padding(
                            padding: EdgeInsets.symmetric(vertical: 8),
                            child: Text('Checking permissions…'),
                          )
                        else
                          ..._permissionStatuses.entries.map((entry) {
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
                              title: Text(_permissions.labelFor(entry.key)),
                              trailing: Text(
                                _permissions.statusLabel(entry.value),
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
                          isLoading: _permissionBusy,
                          onPressed: _requestPermissions,
                        ),
                        TextButton(
                          onPressed: _permissions.openAppSettingsPage,
                          child: const Text('Open system app settings'),
                        ),
                        AppButton(
                          label: 'Load from cloud',
                          icon: Icons.cloud_download_rounded,
                          variant: AppButtonVariant.outlined,
                          isLoading: _companyBusy,
                          onPressed: _loadPrinterCloud,
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
                isLoading: _companyBusy,
                onPressed: _updateSettings,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PrinterSectionCard extends StatelessWidget {
  const _PrinterSectionCard({
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

class _PrinterConnectRow extends StatelessWidget {
  const _PrinterConnectRow({
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

class _SettingSwitchTile extends StatelessWidget {
  const _SettingSwitchTile({
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

class _PreviewActionButton extends StatelessWidget {
  const _PreviewActionButton({
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

class _LivePaperPreviews extends ConsumerWidget {
  const _LivePaperPreviews();

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
        card('KOT', kot),
      ],
    );
  }
}


