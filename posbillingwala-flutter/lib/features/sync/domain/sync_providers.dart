import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/sync/data/invoice_sync_api.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_invoice_dto.dart';

class InvoiceSyncResult {
  const InvoiceSyncResult({
    required this.uploaded,
    required this.downloaded,
    required this.failed,
    this.message,
  });

  final int uploaded;
  final int downloaded;
  final int failed;
  final String? message;

  bool get hasFailures => failed > 0;
}

final pendingInvoicesProvider = StreamProvider<List<Invoice>>((ref) {
  return ref.watch(appDatabaseProvider).watchPendingSyncInvoices();
});

final syncPendingSnapshotProvider =
    FutureProvider<SyncPendingSnapshot>((ref) async {
  // Refresh when pending invoices change.
  ref.watch(pendingInvoicesProvider);
  return ref.read(appDatabaseProvider).getSyncPendingSnapshot();
});

class InvoiceSyncController extends Notifier<AsyncValue<InvoiceSyncResult?>> {
  @override
  AsyncValue<InvoiceSyncResult?> build() => const AsyncData(null);

  Future<InvoiceSyncResult> uploadPending({int? onlyInvoiceId}) async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      const result = InvoiceSyncResult(
        uploaded: 0,
        downloaded: 0,
        failed: 0,
        message: 'Please login first',
      );
      state = const AsyncData(result);
      return result;
    }

    state = const AsyncLoading();
    final db = ref.read(appDatabaseProvider);
    final api = InvoiceSyncApi(ref.read(apiClientProvider));

    try {
      final List<Invoice> pending;
      if (onlyInvoiceId == null) {
        pending = await db.getPendingSyncInvoices();
      } else {
        final invoice = await db.getInvoiceById(onlyInvoiceId);
        pending = (invoice != null && invoice.invoiceSyncStatus == '0')
            ? [invoice]
            : const <Invoice>[];
      }

      var uploaded = 0;
      var failed = 0;
      String? lastError;

      // WithTable InvoicePendingSync + UserSynchronizeData:
      // deletes → invoice products → invoice combo items → invoice headers.
      for (final row in await db.getPendingInvoiceProductDeletes()) {
        final network = row.invoiceProductNetworkStatus?.trim();
        if (network == null || network.isEmpty) {
          await db.removeInvoiceProductDelete(row.deleteId);
          continue;
        }
        try {
          final ok = await api.deleteInvoiceProduct(
            invoiceProductNetworkStatus: network,
          );
          if (ok) {
            await db.removeInvoiceProductDelete(row.deleteId);
          }
        } catch (_) {
          // Keep queued for next sync.
        }
      }

      final comboPending = onlyInvoiceId == null
          ? await db.getAllPendingInvoiceComboItems()
          : await db.getPendingInvoiceComboItems(
              pending.isEmpty ? '' : pending.first.invoiceNumber,
            );

      final items = onlyInvoiceId == null
          ? await db.getPendingUnsyncedInvoiceItems()
          : pending.isEmpty
              ? const <InvoiceItem>[]
              : (await db.getInvoiceItems(pending.first.invoiceNumber))
                  .where((e) => e.invoiceItemSyncStatus != '1')
                  .toList();

      for (final item in items) {
        try {
          final lineNet = item.invoiceItemNetworkStatus?.trim();
          final comboNet = comboPending
              .where(
                (c) =>
                    lineNet != null &&
                    lineNet.isNotEmpty &&
                    c.invoiceProductNetworkStatus == lineNet,
              )
              .map((c) => c.comboNetworkStatus?.trim() ?? '')
              .firstWhere((s) => s.isNotEmpty, orElse: () => '');
          final ok = await api.uploadInvoiceItem(
            item: item,
            comboNetworkStatus: comboNet,
          );
          if (!ok) {
            failed++;
            lastError = 'Line upload failed for ${item.productName}';
            continue;
          }
          await db.markInvoiceItemSynced(item.invoiceItemId);
        } catch (e) {
          failed++;
          lastError = e.toString();
        }
      }

      for (final combo in comboPending) {
        try {
          final ok = await api.uploadInvoiceComboItem(item: combo);
          if (!ok) {
            failed++;
            lastError =
                'Combo line upload failed for ${combo.invoiceNumber}';
            continue;
          }
          await db.markInvoiceComboItemSynced(combo.invoiceComboItemId);
        } catch (e) {
          failed++;
          lastError = e.toString();
        }
      }

      for (final invoice in pending) {
        try {
          final headerOk = await api.uploadInvoice(
            userId: userId,
            invoice: invoice,
          );
          if (!headerOk) {
            throw StateError(
              'Invoice upload failed for ${invoice.invoiceNumber}',
            );
          }

          await db.markInvoiceSynced(invoice.invoiceId);
          uploaded++;
        } catch (e) {
          failed++;
          lastError = e.toString();
        }
      }

      final result = InvoiceSyncResult(
        uploaded: uploaded,
        downloaded: 0,
        failed: failed,
        message: failed == 0
            ? (uploaded == 0 ? 'Nothing pending' : 'Uploaded $uploaded bill(s)')
            : 'Uploaded $uploaded, failed $failed'
                '${lastError == null ? '' : ': $lastError'}',
      );
      state = AsyncData(result);
      return result;
    } catch (e, st) {
      state = AsyncError(e, st);
      return InvoiceSyncResult(
        uploaded: 0,
        downloaded: 0,
        failed: 1,
        message: e.toString(),
      );
    }
  }

  /// Downloads invoices (+ lines) from cloud and upserts as synced.
  Future<InvoiceSyncResult> downloadInvoices({String? invoiceDate}) async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      const result = InvoiceSyncResult(
        uploaded: 0,
        downloaded: 0,
        failed: 0,
        message: 'Please login first',
      );
      state = const AsyncData(result);
      return result;
    }

    state = const AsyncLoading();
    try {
      final api = InvoiceSyncApi(ref.read(apiClientProvider));
      final db = ref.read(appDatabaseProvider);

      final headers = await api.fetchInvoices(
        userId,
        invoiceDate: invoiceDate,
      );
      final lines = await api.fetchInvoiceItems(userId);
      List<CloudInvoiceComboItemDto> comboLines = const [];
      try {
        comboLines = await api.fetchInvoiceComboItems(userId);
      } catch (_) {
        // Older servers may omit combo lines; invoices still import.
        comboLines = const [];
      }

      final validHeaders = headers
          .where(
            (e) =>
                e.invoiceNumber.isNotEmpty && e.invoiceNetworkStatus.isNotEmpty,
          )
          .toList();

      final itemsByNumber = <String, List<InvoiceItemsCompanion>>{};
      for (final line in lines) {
        if (line.invoiceNumber.isEmpty || line.productName.isEmpty) continue;
        itemsByNumber
            .putIfAbsent(line.invoiceNumber, () => <InvoiceItemsCompanion>[])
            .add(line.toCompanion());
      }

      final companions = validHeaders.map((e) {
        final count = itemsByNumber[e.invoiceNumber]?.fold<int>(
              0,
              (sum, item) =>
                  sum + (item.productQuantity.present ? item.productQuantity.value : 1),
            ) ??
            0;
        return e.toCompanion().copyWith(itemCount: Value(count));
      }).toList();

      final comboCompanions = comboLines
          .where((e) => e.invoiceNumber.isNotEmpty)
          .map((e) => e.toCompanion())
          .toList();

      final resultUpsert = await db.upsertCloudInvoices(
        headers: companions,
        itemsByNumber: itemsByNumber,
        comboItems: comboCompanions,
      );
      final downloaded = resultUpsert.inserted + resultUpsert.updated;

      final result = InvoiceSyncResult(
        uploaded: 0,
        downloaded: downloaded,
        failed: 0,
        message: downloaded == 0
            ? 'No cloud bills to import'
            : 'Downloaded $downloaded bill(s)'
                ' (${resultUpsert.inserted} new, ${resultUpsert.updated} updated'
                '${resultUpsert.skipped > 0 ? ', ${resultUpsert.skipped} skipped' : ''}'
                '${resultUpsert.comboItems > 0 ? ', ${resultUpsert.comboItems} combo lines' : ''})',
      );
      state = AsyncData(result);
      return result;
    } catch (e, st) {
      state = AsyncError(e, st);
      return InvoiceSyncResult(
        uploaded: 0,
        downloaded: 0,
        failed: 1,
        message: e.toString(),
      );
    }
  }

  Future<InvoiceSyncResult> syncBothWays() async {
    final upload = await uploadPending();
    // Full history like WithTable InvoiceWorker (no month filter).
    final download = await downloadInvoices();
    final result = InvoiceSyncResult(
      uploaded: upload.uploaded,
      downloaded: download.downloaded,
      failed: upload.failed + download.failed,
      message:
          'Upload ${upload.uploaded}, download ${download.downloaded}'
          '${(upload.failed + download.failed) > 0 ? ', failed ${upload.failed + download.failed}' : ''}',
    );
    state = AsyncData(result);
    return result;
  }
}

final invoiceSyncControllerProvider =
    NotifierProvider<InvoiceSyncController, AsyncValue<InvoiceSyncResult?>>(
  InvoiceSyncController.new,
);
