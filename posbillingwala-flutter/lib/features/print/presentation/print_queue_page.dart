import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_dispatcher.dart';

class PrintQueuePage extends ConsumerStatefulWidget {
  const PrintQueuePage({super.key});

  @override
  ConsumerState<PrintQueuePage> createState() => PrintQueuePageState();
}

class PrintQueuePageState extends ConsumerState<PrintQueuePage> {
  List<Map<String, dynamic>> jobs = const [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  Future<void> load() async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    final list = await ref.read(storePrinterApiProvider).queue(session.licenceUserId);
    if (!mounted) return;
    setState(() {
      jobs = list;
      loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Print queue')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: load,
              child: ListView.builder(
                itemCount: jobs.length,
                itemBuilder: (context, index) {
                  final job = jobs[index];
                  final status = job['status']?.toString() ?? '';
                  return ListTile(
                    title: Text('${job['documentType']} #${job['documentId']}'),
                    subtitle: Text(status),
                    trailing: status == 'FAILED'
                        ? TextButton(
                            onPressed: () async {
                              final session = ref.read(authControllerProvider).session;
                              if (session == null) return;
                              await ref.read(storePrinterApiProvider).retry(
                                    session.licenceUserId,
                                    job['id'].toString(),
                                  );
                              await load();
                            },
                            child: const Text('Retry'),
                          )
                        : null,
                  );
                },
              ),
            ),
    );
  }
}
