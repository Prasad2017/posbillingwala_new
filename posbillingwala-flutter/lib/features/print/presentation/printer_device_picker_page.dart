import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/permissions/app_permission_service.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/esc_pos_transport_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:print_bluetooth_thermal/print_bluetooth_thermal.dart';
import 'package:unified_esc_pos_printer/unified_esc_pos_printer.dart' as esc;

/* Result returned when the user picks a printer for bill/KOT. */
class PickedPrinter {
  const PickedPrinter({
    required this.transport,
    this.bluetoothMac = '',
    this.bluetoothName = '',
    this.usbIdentifier = '',
    this.usbName = '',
    this.networkHost = '',
    this.networkPort = 9100,
  });

  final PosPrinterTransport transport;
  final String bluetoothMac;
  final String bluetoothName;
  final String usbIdentifier;
  final String usbName;
  final String networkHost;
  final int networkPort;
}

/* Pick any ESC/POS printer: Bluetooth (paired), USB (OTG), or Network. */
class PrinterDevicePickerPage extends StatefulWidget {
  const PrinterDevicePickerPage({
    super.key,
    required this.channel,
    this.initialTransport = PosPrinterTransport.bluetooth,
    this.showNetwork = true,
    this.lockToInitialTransport = false,
    this.title,
  });

  final PrinterChannelKind channel;
  final PosPrinterTransport initialTransport;
  final bool showNetwork;
  final bool lockToInitialTransport;
  final String? title;

  @override
  State<PrinterDevicePickerPage> createState() =>
      PrinterDevicePickerPageState();
}

class PrinterDevicePickerPageState extends State<PrinterDevicePickerPage>
    with SingleTickerProviderStateMixin {
  late final TabController printerDevicePickerPageTabs;
  final btHub = BluetoothPrinterHub.instance;
  final usbHub = EscPosTransportHub.instance;
  final printerDevicePickerPagePermissions = const AppPermissionService();
  final hostCtrl = TextEditingController();
  final portCtrl = TextEditingController(text: '9100');

  bool printerDevicePickerPageLoading = false;
  String? printerDevicePickerPageError;
  List<BluetoothInfo> btDevices = const [];
  List<esc.PrinterDevice> usbDevices = const [];

  @override
  void initState() {
    super.initState();
    final tabCount = widget.lockToInitialTransport
        ? 1
        : (widget.showNetwork ? 3 : 2);
    var resolved = widget.initialTransport;
    if (!widget.showNetwork && resolved == PosPrinterTransport.network) {
      resolved = PosPrinterTransport.bluetooth;
    }
    var initial = widget.lockToInitialTransport
        ? 0
        : resolved.index.clamp(0, tabCount - 1);
    printerDevicePickerPageTabs = TabController(
      length: tabCount,
      vsync: this,
      initialIndex: initial,
    );
    printerDevicePickerPageTabs.addListener(() {
      if (!printerDevicePickerPageTabs.indexIsChanging) loadCurrentTab();
    });
    loadCurrentTab();
  }

  @override
  void dispose() {
    printerDevicePickerPageTabs.dispose();
    hostCtrl.dispose();
    portCtrl.dispose();
    super.dispose();
  }

  Future<void> loadCurrentTab() async {
    if (widget.lockToInitialTransport) {
      if (widget.initialTransport == PosPrinterTransport.usb) {
        await loadUsb();
      } else {
        await loadBluetooth();
      }
      return;
    }
    final index = printerDevicePickerPageTabs.index;
    if (index == 0) {
      await loadBluetooth();
    } else if (index == 1) {
      await loadUsb();
    }
  }

  Future<void> loadBluetooth() async {
    setState(() {
      printerDevicePickerPageLoading = true;
      printerDevicePickerPageError = null;
    });
    try {
      final allowed = await printerDevicePickerPagePermissions
          .ensurePrintPermissions();
      if (!allowed) {
        setState(() {
          printerDevicePickerPageLoading = false;
          printerDevicePickerPageError =
              'Allow Bluetooth / nearby devices / location to list printers.';
        });
        return;
      }
      if (!await btHub.isBluetoothOn()) {
        setState(() {
          printerDevicePickerPageLoading = false;
          printerDevicePickerPageError = 'Turn on Bluetooth and try again.';
        });
        return;
      }
      final list = await btHub.pairedDevices();
      if (!mounted) return;
      setState(() {
        btDevices = list;
        printerDevicePickerPageLoading = false;
        if (list.isEmpty) {
          printerDevicePickerPageError =
              'No paired Bluetooth printers. Pair any ESC/POS thermal printer in system Bluetooth settings, then refresh.';
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        printerDevicePickerPageLoading = false;
        printerDevicePickerPageError = '$e';
      });
    }
  }

  Future<void> loadUsb() async {
    setState(() {
      printerDevicePickerPageLoading = true;
      printerDevicePickerPageError = null;
    });
    try {
      final list = await usbHub.scanUsb();
      if (!mounted) return;
      setState(() {
        usbDevices = list;
        printerDevicePickerPageLoading = false;
        if (list.isEmpty) {
          printerDevicePickerPageError =
              'No USB printers found. Connect any ESC/POS USB/OTG printer (Printer Class or USB-serial), grant USB permission, then refresh.';
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        printerDevicePickerPageLoading = false;
        printerDevicePickerPageError = '$e';
      });
    }
  }

  Future<void> selectBluetooth(BluetoothInfo device) async {
    final ok = await btHub.connect(
      widget.channel,
      address: device.macAdress,
      fromUser: true,
    );
    if (!mounted) return;
    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Could not connect to ${device.name}')),
      );
      return;
    }
    Navigator.pop(
      context,
      PickedPrinter(
        transport: PosPrinterTransport.bluetooth,
        bluetoothMac: device.macAdress,
        bluetoothName: device.name,
      ),
    );
  }

  Future<void> selectUsb(esc.PrinterDevice device) async {
    final ok = await usbHub.connectDevice(device);
    if (!mounted) return;
    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Could not connect to ${device.name}. Accept the USB permission dialog if shown.',
          ),
        ),
      );
      return;
    }
    final usb = device as esc.UsbPrinterDevice;
    Navigator.pop(
      context,
      PickedPrinter(
        transport: PosPrinterTransport.usb,
        usbIdentifier: usb.identifier,
        usbName: usb.name,
      ),
    );
  }

  void selectNetwork() {
    final host = hostCtrl.text.trim();
    final port = int.tryParse(portCtrl.text.trim()) ?? 9100;
    if (host.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Enter printer IP / host')));
      return;
    }
    Navigator.pop(
      context,
      PickedPrinter(
        transport: PosPrinterTransport.network,
        networkHost: host,
        networkPort: port.clamp(1, 65535),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final title =
        widget.title ??
        (widget.channel == PrinterChannelKind.bill
            ? 'Select bill printer'
            : 'Select KOT printer');
    final lockedUsb =
        widget.lockToInitialTransport &&
        widget.initialTransport == PosPrinterTransport.usb;
    final lockedBt = widget.lockToInitialTransport && !lockedUsb;
    final tabs = <Tab>[
      if (!lockedUsb) const Tab(text: 'Bluetooth'),
      if (!lockedBt) const Tab(text: 'USB'),
      if (widget.showNetwork && !widget.lockToInitialTransport)
        const Tab(text: 'Network'),
    ];
    final views = <Widget>[
      if (!lockedUsb) bluetoothTab(),
      if (!lockedBt) usbTab(),
      if (widget.showNetwork && !widget.lockToInitialTransport) networkTab(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        bottom: tabs.length > 1
            ? TabBar(controller: printerDevicePickerPageTabs, tabs: tabs)
            : null,
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: printerDevicePickerPageLoading ? null : loadCurrentTab,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: Column(
        children: [
          Container(
            margin: EdgeInsets.fromLTRB(
              AppBreakpoints.pagePaddingFor(context.widthClass),
              10,
              AppBreakpoints.pagePaddingFor(context.widthClass),
              4,
            ),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.primary.withValues(alpha: .08),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              children: [
                AppModuleIcon(
                  icon: lockedUsb ? Icons.usb_rounded : Icons.print_rounded,
                  color: AppColors.primary,
                  size: 50,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    lockedUsb
                        ? 'Connect a USB / OTG thermal printer'
                        : lockedBt
                        ? 'Connect a Bluetooth thermal printer'
                        : 'Connect your billing printer',
                    style: const TextStyle(
                      fontWeight: FontWeight.w900,
                      color: AppColors.navy,
                    ),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: tabs.length == 1
                ? views.first
                : TabBarView(
                    controller: printerDevicePickerPageTabs,
                    children: views,
                  ),
          ),
        ],
      ),
      ),
    );
  }

  Widget bluetoothTab() {
    return deviceList(
      loading:
          printerDevicePickerPageLoading &&
          (widget.lockToInitialTransport ||
              printerDevicePickerPageTabs.index == 0),
      error:
          (widget.lockToInitialTransport ||
              printerDevicePickerPageTabs.index == 0)
          ? printerDevicePickerPageError
          : null,
      emptyAction: loadBluetooth,
      children: btDevices
          .map(
            (d) => ListTile(
              leading: const Icon(Icons.bluetooth_rounded),
              title: Text(d.name.isEmpty ? 'Bluetooth printer' : d.name),
              subtitle: Text(d.macAdress),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: () => selectBluetooth(d),
            ),
          )
          .toList(),
    );
  }

  Widget usbTab() {
    return deviceList(
      loading:
          printerDevicePickerPageLoading &&
          (widget.lockToInitialTransport ||
              printerDevicePickerPageTabs.index == 1),
      error:
          (widget.lockToInitialTransport ||
              printerDevicePickerPageTabs.index == 1)
          ? printerDevicePickerPageError
          : null,
      emptyAction: loadUsb,
      children: usbDevices
          .map(
            (d) => ListTile(
              leading: const Icon(Icons.usb_rounded),
              title: Text(d.name.isEmpty ? 'USB printer' : d.name),
              subtitle: Text(
                d is esc.UsbPrinterDevice
                    ? d.identifier
                    : d.connectionType.name,
              ),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: () => selectUsb(d),
            ),
          )
          .toList(),
    );
  }

  Widget networkTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Works with any Wi‑Fi / LAN ESC/POS printer on port 9100 (or custom).',
        ),
        const SizedBox(height: 16),
        AppTextField(
          required: true,
          controller: hostCtrl,
          label: 'IP / host',
          hint: '192.168.1.50',
        ),
        const SizedBox(height: 12),
        AppTextField(
          controller: portCtrl,
          label: 'Port',
          hint: '9100',
          keyboardType: TextInputType.number,
        ),
        const SizedBox(height: 16),
        AppButton(
          label: 'Use this network printer',
          icon: Icons.wifi_rounded,
          onPressed: selectNetwork,
          expanded: false,
        ),
      ],
    );
  }

  Widget deviceList({
    required bool loading,
    required String? error,
    required Future<void> Function() emptyAction,
    required List<Widget> children,
  }) {
    if (loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (children.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(error ?? 'No devices found', textAlign: TextAlign.center),
            const SizedBox(height: 16),
            AppButton(
              label: 'Retry',
              icon: Icons.refresh_rounded,
              onPressed: emptyAction,
              expanded: false,
            ),
          ],
        ),
      );
    }
    return ListView.separated(
      itemCount: children.length,
      separatorBuilder: (_, _) => const Divider(height: 1),
      itemBuilder: (context, index) => children[index],
    );
  }
}
