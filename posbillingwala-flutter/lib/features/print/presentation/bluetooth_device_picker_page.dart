import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/permissions/app_permission_service.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:print_bluetooth_thermal/print_bluetooth_thermal.dart';

/* Paired Bluetooth device list — same role as Android [DeviceListActivity]. */
class BluetoothDevicePickerPage extends StatefulWidget {
  const BluetoothDevicePickerPage({
    super.key,
    required this.channel,
    this.title,
  });

  final PrinterChannelKind channel;
  final String? title;

  @override
  State<BluetoothDevicePickerPage> createState() =>
      BluetoothDevicePickerPageState();
}

class BluetoothDevicePickerPageState extends State<BluetoothDevicePickerPage> {
  final hub = BluetoothPrinterHub.instance;
  final bluetoothDevicePickerPagePermissions = const AppPermissionService();

  bool loading = true;
  String? error;
  List<BluetoothInfo> bluetoothDevicePickerPageDevices = const [];

  @override
  void initState() {
    super.initState();
    bluetoothDevicePickerPageLoad();
  }

  Future<void> bluetoothDevicePickerPageLoad() async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final allowed = await bluetoothDevicePickerPagePermissions.ensurePrintPermissions();
      if (!allowed) {
        setState(() {
          loading = false;
          error =
              'Allow Bluetooth, nearby devices, and location to list printers.';
          bluetoothDevicePickerPageDevices = const [];
        });
        return;
      }
      if (!await hub.isBluetoothOn()) {
        setState(() {
          loading = false;
          error = 'Turn on Bluetooth and try again.';
          bluetoothDevicePickerPageDevices = const [];
        });
        return;
      }
      final list = await hub.pairedDevices();
      if (!mounted) return;
      setState(() {
        bluetoothDevicePickerPageDevices = list;
        loading = false;
        if (list.isEmpty) {
          error =
              'No paired printers found. Pair the thermal printer in Android Bluetooth settings, then refresh.';
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        loading = false;
        error = 'Could not load paired devices: $e';
        bluetoothDevicePickerPageDevices = const [];
      });
    }
  }

  Future<void> select(BluetoothInfo device) async {
    final ok = await hub.connect(
      widget.channel,
      address: device.macAdress,
      fromUser: true,
    );
    if (!mounted) return;
    if (ok) {
      Navigator.pop(context, device);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Could not connect to ${device.name}. Try again.'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.title ??
        (widget.channel == PrinterChannelKind.bill
            ? 'Select bill printer'
            : 'Select KOT printer');

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: loading ? null : bluetoothDevicePickerPageLoad,
            icon: const AppSvg(
              AppAssets.svgRefresh,
              width: 20,
              height: 20,
              color: Colors.white,
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: loading
                ? const AppLoadingState(message: 'Scanning paired devices…')
                : bluetoothDevicePickerPageDevices.isEmpty
                    ? AppErrorState(
                        title: 'No printers found',
                        message: error ?? 'Pair a printer and try again.',
                        onRetry: bluetoothDevicePickerPageLoad,
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                        itemCount: bluetoothDevicePickerPageDevices.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 8),
                        itemBuilder: (context, index) {
                          final d = bluetoothDevicePickerPageDevices[index];
                          final connected =
                              hub.connectedAddress == d.macAdress;
                          return AppCard(
                            accentColor: connected
                                ? AppColors.success
                                : AppColors.primary,
                            padding: EdgeInsets.zero,
                            onTap: () => select(d),
                            child: ListTile(
                              leading: AppModuleIcon(
                                svgPath: AppAssets.svgPrint,
                                color: connected
                                    ? AppColors.success
                                    : AppColors.primary,
                                size: 48,
                              ),
                              title: Text(
                                d.name.isEmpty ? 'Unknown printer' : d.name,
                                style: AppTypography.cardTitle(),
                              ),
                              subtitle: Text(d.macAdress),
                              trailing: connected
                                  ? const AppStatusBadge(
                                      label: 'Connected',
                                      color: AppColors.success,
                                      filled: true,
                                    )
                                  : const AppSvg(
                                      AppAssets.svgChevron,
                                      width: 18,
                                      height: 18,
                                      color: AppColors.textSecondary,
                                    ),
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
