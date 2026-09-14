import 'package:flutter/material.dart';
import 'package:print_bluetooth_thermal/print_bluetooth_thermal.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/permissions/app_permission_service.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/app_module_icon.dart';
import 'package:pos_billingwala_v2/core/widgets/app_states.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';

/// Paired Bluetooth device list — same role as Android [DeviceListActivity].
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
      _BluetoothDevicePickerPageState();
}

class _BluetoothDevicePickerPageState extends State<BluetoothDevicePickerPage> {
  final _hub = BluetoothPrinterHub.instance;
  final _permissions = const AppPermissionService();

  bool _loading = true;
  String? _error;
  List<BluetoothInfo> _devices = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final allowed = await _permissions.ensurePrintPermissions();
      if (!allowed) {
        setState(() {
          _loading = false;
          _error =
              'Allow Bluetooth, nearby devices, and location to list printers.';
          _devices = const [];
        });
        return;
      }
      if (!await _hub.isBluetoothOn()) {
        setState(() {
          _loading = false;
          _error = 'Turn on Bluetooth and try again.';
          _devices = const [];
        });
        return;
      }
      final list = await _hub.pairedDevices();
      if (!mounted) return;
      setState(() {
        _devices = list;
        _loading = false;
        if (list.isEmpty) {
          _error =
              'No paired printers found. Pair the thermal printer in Android Bluetooth settings, then refresh.';
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Could not load paired devices: $e';
        _devices = const [];
      });
    }
  }

  Future<void> _select(BluetoothInfo device) async {
    final ok = await _hub.connect(
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
            onPressed: _loading ? null : _load,
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
            child: _loading
                ? const AppLoadingState(message: 'Scanning paired devices…')
                : _devices.isEmpty
                    ? AppErrorState(
                        title: 'No printers found',
                        message: _error ?? 'Pair a printer and try again.',
                        onRetry: _load,
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                        itemCount: _devices.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 8),
                        itemBuilder: (context, index) {
                          final d = _devices[index];
                          final connected =
                              _hub.connectedAddress == d.macAdress;
                          return AppCard(
                            accentColor: connected
                                ? AppColors.success
                                : AppColors.primary,
                            padding: EdgeInsets.zero,
                            onTap: () => _select(d),
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
