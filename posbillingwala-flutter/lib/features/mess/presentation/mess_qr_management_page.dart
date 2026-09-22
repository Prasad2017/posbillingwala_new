import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_page.dart';

/* Dedicated QR Management screen (opened from Mess hub card). */
class MessQrManagementPage extends ConsumerStatefulWidget {
  const MessQrManagementPage({super.key});

  @override
  ConsumerState<MessQrManagementPage> createState() =>
      MessQrManagementPageState();
}

class MessQrManagementPageState extends ConsumerState<MessQrManagementPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(messCommonQrProvider.notifier).load();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('QR Management')),
      body: const ResponsiveScrollShell(
        dashboard: true,
        child: CommonQrTab(),
      ),
    );
  }
}
