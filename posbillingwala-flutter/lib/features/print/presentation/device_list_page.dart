import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class DeviceListPage extends ConsumerStatefulWidget {
  const DeviceListPage({super.key});

  @override
  ConsumerState<DeviceListPage> createState() => DeviceListPageState();
}

class DeviceListPageState extends ConsumerState<DeviceListPage> {
  List<Map<String, dynamic>> devices = const [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    final cached = await CloudScreenCache.loadMapList(
      CloudScreenCache.posDevices,
    );
    if (cached.isNotEmpty && mounted) {
      setState(() {
        devices = cached;
        loading = false;
      });
    }
    final response = await ref
        .read(apiClientProvider)
        .dio
        .post<dynamic>(
          ApiEndpoints.getPosDeviceList,
          data: {'userId': session.licenceUserId},
          options: Options(contentType: Headers.formUrlEncodedContentType),
        );
    final data = asJsonMap(response.data);
    final raw = data['deviceResponse'];
    if (!mounted) return;
    setState(() {
      devices = raw is List
          ? raw
                .whereType<Map>()
                .map((e) => Map<String, dynamic>.from(e))
                .toList()
          : const [];
      loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final canManage = ref
        .watch(permissionControllerProvider)
        .allows('device.manage');
    final pad = AppBreakpoints.pagePaddingFor(context.widthClass);
    return Scaffold(
      appBar: AppBar(title: const Text('Devices')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : ResponsiveScrollShell(
              dashboard: true,
              child: ListView.builder(
                padding: EdgeInsets.fromLTRB(pad, 8, pad, 24),
                itemCount: devices.isEmpty ? 1 : devices.length,
                itemBuilder: (context, index) {
                  if (devices.isEmpty) {
                    return const Padding(
                      padding: EdgeInsets.all(24),
                      child: Text(
                        'No devices registered',
                        textAlign: TextAlign.center,
                      ),
                    );
                  }
                  final device = devices[index];
                  return ListTile(
                    title: Text(
                      device['deviceName']?.toString() ?? 'Device',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    subtitle: Text(
                      '${device['platform'] ?? ''} · ${device['status'] ?? ''}',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    trailing: canManage
                        ? TextButton(
                            onPressed: () async {
                              final session = ref
                                  .read(authControllerProvider)
                                  .session;
                              if (session == null) return;
                              await ref
                                  .read(apiClientProvider)
                                  .dio
                                  .post<dynamic>(
                                    ApiEndpoints.revokePosDevice,
                                    data: {
                                      'userId': session.licenceUserId,
                                      'id': device['id'],
                                    },
                                    options: Options(
                                      contentType:
                                          Headers.formUrlEncodedContentType,
                                    ),
                                  );
                              await load();
                            },
                            child: const Text('Revoke'),
                          )
                        : null,
                  );
                },
              ),
            ),
    );
  }
}
