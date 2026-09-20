import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/company/data/company_api.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/features/company/data/company_logo.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_prefetch.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_push.dart';
import 'package:pos_billingwala_v2/features/sync/domain/fetch_local_counts.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_progress.dart';
import 'package:pos_billingwala_v2/features/sync/domain/sync_providers.dart';
import 'package:pos_billingwala_v2/features/tables/data/dining_session_api.dart';
import 'package:pos_billingwala_v2/features/tables/domain/tables_providers.dart';

enum FullSyncMode { uploadOnly, downloadOnly, both }

const masterStepIds = [
  'categories',
  'subcategories',
  'products',
  'portion_master',
  'portions',
  'combos',
  'combo_items',
  'dining_areas',
  'table_types',
  'tables',
];

const invoiceStepIds = [
  'invoice_item_deletes',
  'invoice_items',
  'invoice_combo_items',
  'invoices',
];

const messStepIds = [
  'mess_members',
  'mess_payments',
  'mess_invoices',
  'mess_tokens',
];

const inventoryStepIds = ['inventory', 'expenses'];
const companyStepIds = ['printer_settings', 'shop_details'];

class FullSyncResult {
  const FullSyncResult({
    required this.message,
    this.mode = FullSyncMode.both,
    this.mastersUploaded = 0,
    this.invoicesUploaded = 0,
    this.invoicesDownloaded = 0,
    this.inventorySynced = false,
    this.messUploaded = 0,
    this.diningUploaded = 0,
    this.diningDownloaded = 0,
    this.companySynced = false,
    this.failed = 0,
    this.localCounts,
  });

  final String message;
  final FullSyncMode mode;
  final int mastersUploaded;
  final int invoicesUploaded;
  final int invoicesDownloaded;
  final bool inventorySynced;
  final int messUploaded;
  final int diningUploaded;
  final int diningDownloaded;
  final bool companySynced;
  final int failed;

  /* Populated after download/fetch — rows currently stored in Drift. */
  final FetchLocalCounts? localCounts;
}

/* Android-parity sync orchestrator: */
/* - Upload pending offline rows to server */
/* - Download / refresh cloud data into local DB */
class FullSyncController extends Notifier<AsyncValue<FullSyncResult?>> {
  @override
  AsyncValue<FullSyncResult?> build() => const AsyncData(null);

  Future<FullSyncResult> syncEverything() =>
      run(FullSyncMode.both, trackProgress: false);

  /* Background refresh: upload pending then download all data without UI loading. */
  Future<FullSyncResult> syncEverythingSilent() =>
      run(FullSyncMode.both, trackProgress: false, silent: true);

  Future<FullSyncResult> uploadAll() =>
      run(FullSyncMode.uploadOnly, trackProgress: false);

  Future<FullSyncResult> downloadAll({bool silent = false}) =>
      run(FullSyncMode.downloadOnly, trackProgress: false, silent: silent);

  /* Settings → Offline Data Synchronize with Cloud. */
  Future<FullSyncResult> uploadWithProgress() =>
      run(FullSyncMode.uploadOnly, trackProgress: true);

  /* Settings → Fetch Data From Cloud (after confirm). */
  /* Blocks when unsynced bills exist (Android parity). */
  Future<FullSyncResult> resetAndFetchWithProgress() async {
    final progress = ref.read(syncProgressProvider.notifier);
    progress.begin(SyncScreenMode.fetch);

    final userId = ref.read(authControllerProvider).session?.licenceUserId;
    if (userId == null || userId.isEmpty) {
      progress.setBlocked(
        headline: 'Please login first',
        subtitle: 'Sign in before fetching cloud data.',
      );
      const result = FullSyncResult(message: 'Please login first', failed: 1);
      state = const AsyncData(result);
      return result;
    }

    final pendingBills = await ref
        .read(appDatabaseProvider)
        .countPendingSyncInvoices();
    if (pendingBills > 0) {
      progress.setBlocked(
        headline: 'Unsynced bills found',
        subtitle:
            'Sync offline bills first. Unsynced bills cannot be overwritten.',
      );
      final result = FullSyncResult(
        message: '$pendingBills unsynced bill(s) — sync them first',
        failed: 1,
      );
      state = AsyncData(result);
      return result;
    }

    state = const AsyncLoading();
    try {
      await ref.read(appDatabaseProvider).resetOperationalDataForFetch();
      return await run(FullSyncMode.downloadOnly, trackProgress: true);
    } catch (e) {
      progress.finish(failed: 1, hadPending: true);
      final result = FullSyncResult(message: '$e', failed: 1);
      state = AsyncData(result);
      return result;
    }
  }

  /* Android Home "Fetch Data": confirm wipe → reset local ops tables → download. */
  Future<FullSyncResult> resetAndFetchAll() async {
    final userId = ref.read(authControllerProvider).session?.licenceUserId;
    if (userId == null || userId.isEmpty) {
      const result = FullSyncResult(message: 'Please login first', failed: 1);
      state = const AsyncData(result);
      return result;
    }
    state = const AsyncLoading();
    try {
      await ref.read(appDatabaseProvider).resetOperationalDataForFetch();
      final result = await run(FullSyncMode.downloadOnly, trackProgress: false);
      return result;
    } catch (e) {
      final result = FullSyncResult(message: '$e', failed: 1);
      state = AsyncData(result);
      return result;
    }
  }

  SyncProgressController? fullSyncControllerProgress(bool track) =>
      track ? ref.read(syncProgressProvider.notifier) : null;

  Future<FullSyncResult> run(
    FullSyncMode mode, {
    required bool trackProgress,
    bool silent = false,
  }) async {
    final session = ref.read(authControllerProvider).session;
    /* Sync is always licence-wide (never staff-filtered). */
    final userId = session?.licenceUserId;
    final ownerId = session?.catalogOwnerId;
    if (userId == null ||
        userId.isEmpty ||
        ownerId == null ||
        ownerId.isEmpty) {
      const result = FullSyncResult(message: 'Please login first', failed: 1);
      if (!silent) state = const AsyncData(result);
      return result;
    }

    final progress = fullSyncControllerProgress(trackProgress);
    if (trackProgress && mode == FullSyncMode.uploadOnly) {
      progress!.begin(SyncScreenMode.upload);
    } else if (trackProgress &&
        mode == FullSyncMode.downloadOnly &&
        !ref.read(syncProgressProvider).isRunning) {
      progress!.begin(SyncScreenMode.fetch);
    }

    if (!silent) state = const AsyncLoading();
    final db = ref.read(appDatabaseProvider);
    var failed = 0;
    final notes = <String>[];
    var hadPending = false;

    var mastersUploaded = 0;
    var invoicesUploaded = 0;
    var invoicesDownloaded = 0;
    var inventorySynced = false;
    var messUploaded = 0;
    var diningUploaded = 0;
    var diningDownloaded = 0;
    var companySynced = false;
    FetchLocalCounts? localCounts;

    final doUpload =
        mode == FullSyncMode.uploadOnly || mode == FullSyncMode.both;
    final doDownload =
        mode == FullSyncMode.downloadOnly || mode == FullSyncMode.both;

    /* ---- UPLOAD (local → server) — same spirit as Android UserSynchronizeData */
    if (doUpload) {
      final snap = await db.getSyncPendingSnapshot();
      hadPending = snap.total > 0;

      progress?.markRunning(masterStepIds);
      try {
        mastersUploaded = 0;
        for (var round = 0; round < 20; round++) {
          final n = await ref
              .read(mastersRepositoryProvider)
              .uploadPendingMasters(ownerId: ownerId, licenceUserId: userId);
          mastersUploaded += n;
          if (n == 0) break;
        }
        notes.add('masters↑$mastersUploaded');
        progress?.markComplete(masterStepIds);
      } catch (_) {
        failed++;
        notes.add('masters↑ error');
        progress?.markError(masterStepIds);
      }

      progress?.markRunning(companyStepIds);
      try {
        companySynced = await uploadCompanyAndPrinter(userId);
        notes.add(companySynced ? 'company↑ ok' : 'company↑ skip');
        progress?.markComplete(companyStepIds);
      } catch (_) {
        failed++;
        notes.add('company↑ error');
        progress?.markError(companyStepIds);
      }

      progress?.markRunning(invoiceStepIds);
      try {
        invoicesUploaded = 0;
        var invoiceFailed = 0;
        for (var round = 0; round < 40; round++) {
          final upload = await ref
              .read(invoiceSyncControllerProvider.notifier)
              .uploadPending();
          invoicesUploaded += upload.uploaded;
          invoiceFailed += upload.failed;
          if (upload.uploaded == 0) break;
        }
        failed += invoiceFailed;
        notes.add('bills↑$invoicesUploaded');
        if (invoiceFailed > 0) {
          progress?.markError(invoiceStepIds);
        } else {
          progress?.markComplete(invoiceStepIds);
        }
      } catch (_) {
        failed++;
        notes.add('bills↑ error');
        progress?.markError(invoiceStepIds);
      }

      progress?.markRunning(messStepIds);
      try {
        messUploaded = 0;
        for (var round = 0; round < 20; round++) {
          final n = await uploadPendingMess(userId, db);
          messUploaded += n;
          if (n == 0) break;
        }
        notes.add('mess↑$messUploaded');
        progress?.markComplete(messStepIds);
      } catch (_) {
        failed++;
        notes.add('mess↑ error');
        progress?.markError(messStepIds);
      }

      progress?.markRunning(const ['dining_sessions']);
      try {
        diningUploaded = 0;
        for (var round = 0; round < 20; round++) {
          final n = await uploadPendingDining(userId, db);
          diningUploaded += n;
          if (n == 0) break;
        }
        notes.add('dining↑$diningUploaded');
        progress?.markComplete(const ['dining_sessions']);
      } catch (_) {
        failed++;
        notes.add('dining↑ error');
        progress?.markError(const ['dining_sessions']);
      }

      progress?.markRunning(inventoryStepIds);
      try {
        if (mode == FullSyncMode.uploadOnly) {
          await ref
              .read(inventoryControllerProvider.notifier)
              .uploadPendingIfOnline();
          inventorySynced = true;
          notes.add('inventory↑ ok');
        } else {
          await ref.read(inventoryControllerProvider.notifier).syncAll();
          inventorySynced = true;
          notes.add('inventory↑ ok');
        }
        progress?.markComplete(inventoryStepIds);
      } catch (_) {
        failed++;
        notes.add('inventory↑ error');
        progress?.markError(inventoryStepIds);
      }

      /* Screen prefs / cached API screens with save endpoints. */
      const screenUploadIds = [
        'mess_shop_payer',
        'meal_sessions',
        'staff',
        'salary',
        'store_printers',
        'printer_routes',
      ];
      progress?.markRunning([screenUploadIds.first]);
      try {
        final screenPush = await CloudScreenPush.run(ref, userId: userId);
        failed += screenPush.failed;
        notes.add('screens↑${screenPush.totalRows}');
        for (final id in screenUploadIds) {
          final ok = screenPush.stepOk[id] ?? true;
          if (ok) {
            progress?.markComplete([id]);
          } else {
            progress?.markError([id]);
          }
        }
      } catch (_) {
        failed++;
        notes.add('screens↑ error');
        await progress?.completeSequentially(screenUploadIds, error: true);
      }
    }

    /* ---- DOWNLOAD (server → local) — same spirit as Android NetworkDataFetcher */
    if (doDownload) {
      const masterFetchIds = [
        'food_types',
        'categories',
        'subcategories',
        'products',
        'portion_master',
        'portions',
        'combos',
        'combo_items',
      ];
      progress?.markRunning([masterFetchIds.first]);
      try {
        await ref
            .read(mastersRepositoryProvider)
            .syncFromCloud(ownerId: ownerId, licenceUserId: userId);
        notes.add('masters↓ ok');
        await progress?.completeSequentially(masterFetchIds);
      } catch (_) {
        failed++;
        notes.add('masters↓ error');
        await progress?.completeSequentially(masterFetchIds, error: true);
      }

      const tableFetchIds = ['dining_areas', 'table_types', 'tables'];
      progress?.markRunning([tableFetchIds.first]);
      try {
        await ref.read(tablesControllerProvider.notifier).syncTables();
        notes.add('tables↓ ok');
        await progress?.completeSequentially(tableFetchIds);
      } catch (_) {
        /* Masters sync already pulls tables; ignore secondary failure. */
        notes.add('tables↓ skip');
        await progress?.completeSequentially(tableFetchIds);
      }

      const invoiceFetchIds = [
        'invoice_items',
        'invoice_combo_items',
        'invoices',
      ];
      progress?.markRunning([invoiceFetchIds.first]);
      try {
        /* Full bill history like WithTable InvoiceWorker (no month filter). */
        final download = await ref
            .read(invoiceSyncControllerProvider.notifier)
            .downloadInvoices();
        invoicesDownloaded = download.downloaded;
        failed += download.failed;
        notes.add('bills↓$invoicesDownloaded');
        await progress?.completeSequentially(
          invoiceFetchIds,
          error: download.failed > 0,
        );
      } catch (_) {
        failed++;
        notes.add('bills↓ error');
        await progress?.completeSequentially(invoiceFetchIds, error: true);
      }

      if (!doUpload) {
        /* When download-only, still refresh inventory/expenses from cloud. */
        progress?.markRunning([inventoryStepIds.first]);
        try {
          await ref.read(inventoryControllerProvider.notifier).syncAll();
          inventorySynced = true;
          notes.add('inventory↓ ok');
          await progress?.completeSequentially(inventoryStepIds);
        } catch (_) {
          failed++;
          notes.add('inventory↓ error');
          await progress?.completeSequentially(inventoryStepIds, error: true);
        }
      } else {
        await progress?.completeSequentially(inventoryStepIds);
      }

      progress?.markRunning([messStepIds.first]);
      try {
        await ref.read(messControllerProvider.notifier).syncMembers();
        notes.add('mess↓ ok');
        await progress?.completeSequentially(messStepIds);
      } catch (_) {
        failed++;
        notes.add('mess↓ error');
        await progress?.completeSequentially(messStepIds, error: true);
      }

      try {
        diningDownloaded = await downloadDining(userId, db);
        notes.add('dining↓$diningDownloaded');
        progress?.markRunning(const ['dining_sessions']);
        progress?.markComplete(const ['dining_sessions']);
      } catch (_) {
        failed++;
        notes.add('dining↓ error');
        progress?.markError(const ['dining_sessions']);
      }

      progress?.markRunning([companyStepIds.first]);
      try {
        final ok = await downloadCompanyAndPrinter(userId);
        companySynced = companySynced || ok;
        notes.add(ok ? 'company↓ ok' : 'company↓ skip');
        await progress?.completeSequentially(companyStepIds);
      } catch (_) {
        failed++;
        notes.add('company↓ error');
        await progress?.completeSequentially(companyStepIds, error: true);
      }

      /* API-only screens — always on download so every screen has local data. */
      const screenFetchIds = [
        'staff',
        'salary',
        'role_defaults',
        'meal_sessions',
        'meal_tokens',
        'pending_meal_tokens',
        'mess_shop_payer',
        'mess_common_qr',
        'store_printers',
        'printer_routes',
        'print_jobs',
        'support_tickets',
        'pos_devices',
        'home_overview',
      ];
      progress?.markRunning([screenFetchIds.first]);
      try {
        final screenPrefetch = await CloudScreenPrefetch.run(
          ref,
          userId: userId,
        );
        failed += screenPrefetch.failed;
        notes.add('screens↓${screenPrefetch.totalRows}');
        for (final id in screenFetchIds) {
          final ok = screenPrefetch.stepOk[id] ?? true;
          if (ok) {
            progress?.markComplete([id]);
          } else {
            progress?.markError([id]);
          }
        }
      } catch (_) {
        failed++;
        notes.add('screens↓ error');
        await progress?.completeSequentially(screenFetchIds, error: true);
      }

      try {
        localCounts = await FetchLocalCounts.load(db);
        notes.add('local ${localCounts.totalSaved} rows');
      } catch (_) {
        notes.add('local counts skip');
      }

      try {
        invalidateAfterCloudFetch(ref);
      } catch (_) {}
    }

    final label = switch (mode) {
      FullSyncMode.uploadOnly => 'Upload to server',
      FullSyncMode.downloadOnly => 'Download from server',
      FullSyncMode.both => 'Sync everything',
    };

    final result = FullSyncResult(
      message: failed == 0
          ? '$label complete · ${notes.join(' · ')}'
          : '$label finished with $failed issue(s) · ${notes.join(' · ')}',
      mode: mode,
      mastersUploaded: mastersUploaded,
      invoicesUploaded: invoicesUploaded,
      invoicesDownloaded: invoicesDownloaded,
      inventorySynced: inventorySynced,
      messUploaded: messUploaded,
      diningUploaded: diningUploaded,
      diningDownloaded: diningDownloaded,
      companySynced: companySynced,
      failed: failed,
      localCounts: localCounts,
    );
    if (!silent) state = AsyncData(result);
    progress?.finish(failed: failed, hadPending: hadPending);
    return result;
  }

  Future<int> uploadPendingMess(String userId, AppDatabase db) async {
    final api = MessApi(ref.read(apiClientProvider));
    var uploaded = 0;

    for (final member in await db.getPendingMessMembers()) {
      final ok = await api.insertMessMember(
        userId: userId,
        member: MessMemberDto(
          memberId: member.memberId,
          memberName: member.memberName,
          memberMobileNumber: member.memberMobileNumber,
          memberAltenetMobileNumber: member.memberAltenetMobileNumber,
          memberAddress: member.memberAddress,
          registrationNo: member.registrationNo,
          memberType: member.memberType,
          rollNo: member.rollNo,
          college: member.college,
          studentYear: member.studentYear,
          company: member.company,
          memberStatus: member.memberStatus,
          memberNetworkStatus: member.memberNetworkStatus,
        ),
      );
      if (ok) {
        await db.markMessMemberSynced(member.memberId);
        uploaded++;
      }
    }

    for (final payment in await db.getPendingMessPayments()) {
      final ok = await api.insertMemberPayment(
        userId: userId,
        memberId: payment.memberId,
        memberName: payment.memberName,
        paymentMessAmount: payment.paymentMessAmount.toStringAsFixed(2),
        paymentPaidAmount: payment.paymentPaidAmount.toStringAsFixed(2),
        messTotalDays: payment.messTotalDays,
        paymentDate: payment.paymentDate,
        paymentNetworkStatus: payment.paymentNetworkStatus,
        paymentStatus: payment.paymentStatus,
      );
      if (ok) {
        await db.markMessPaymentSynced(payment.localPaymentId);
        uploaded++;
      }
    }

    for (final token in await db.getPendingMessTokens()) {
      final ok = await api.insertMessToken(
        userId: userId,
        tokenCode: token.tokenCode,
        memberId: token.memberId ?? '',
        memberName: token.memberName ?? '',
        memberMobile: token.memberMobile ?? '',
        memberType: token.memberType,
        messType: token.messType,
        tokenAmount: token.tokenAmount.toStringAsFixed(2),
        tokenDate: token.tokenDate,
        tokenNetworkStatus: token.tokenNetworkStatus ?? '',
      );
      if (ok) {
        await db.markMessTokenSynced(token.tokenId);
        uploaded++;
      }
    }

    /* Verified offline → verifyMessToken.php (WithTable verify queue). */
    for (final token in await db.getPendingMessTokenVerifies()) {
      final ok = await api.verifyMessToken(
        userId: userId,
        tokenCode: token.tokenCode,
        verifiedDate: token.verifiedDate,
        verifyNetworkStatus: token.verifyNetworkStatus ?? '',
      );
      if (ok) {
        await db.markMessTokenVerifySynced(token.tokenId);
        uploaded++;
      }
    }

    for (final coupon in await db.getPendingMessInvoices()) {
      final ok = await api.insertMessInvoice(
        userId: userId,
        memberName: coupon.memberName,
        messType: coupon.messType,
        messInvoiceDate: DateFormat(
          'yyyy-MM-dd HH:mm:ss',
        ).format(coupon.messInvoiceDate),
        messInvoiceNetworkStatus: coupon.messInvoiceNetworkStatus,
        messInvoiceStatus: '0',
      );
      if (ok) {
        await db.markMessInvoiceSynced(coupon.invoiceId);
        uploaded++;
      }
    }
    return uploaded;
  }

  Future<int> uploadPendingDining(String userId, AppDatabase db) async {
    final diningApi = DiningSessionApi(ref.read(apiClientProvider));
    var diningUploaded = 0;
    for (final session in await db.getPendingDiningSessions()) {
      final dto = DiningSessionDto(
        sessionId: session.sessionId,
        localSessionId: '${session.sessionId}',
        primaryTableNumber: session.primaryTableNumber,
        joinedTableNumbers: session.joinedTableNumbers,
        sessionStatus: session.sessionStatus,
        guestCount: session.guestCount,
        startedAt: session.startedAt,
        closedAt: session.closedAt,
        customerName: session.customerName,
        customerMobile: session.customerMobile,
        waiterName: session.waiterName,
        unpaidInvoiceNumber: session.unpaidInvoiceNumber,
        paidAmount: session.paidAmount,
        sessionVersion: session.sessionVersion,
        sessionNetworkStatus: session.sessionNetworkStatus,
      );
      final ok = await diningApi.insertDiningSession(
        userId: userId,
        session: dto,
      );
      if (ok) {
        await db.markDiningSessionSynced(session.sessionId);
        diningUploaded++;
      }
    }
    return diningUploaded;
  }

  Future<int> downloadDining(String userId, AppDatabase db) async {
    final diningApi = DiningSessionApi(ref.read(apiClientProvider));
    final cloudSessions = await diningApi.fetchDiningSessions(
      userId,
      openOnly: false,
    );
    final companions = cloudSessions
        .where((e) => e.primaryTableNumber.trim().isNotEmpty)
        .map(
          (e) => DiningSessionsCompanion.insert(
            primaryTableNumber: e.primaryTableNumber,
            joinedTableNumbers: Value(e.joinedTableNumbers),
            sessionStatus: Value(e.sessionStatus),
            guestCount: Value(e.guestCount),
            startedAt: e.startedAt ?? DateTime.now(),
            closedAt: Value(e.closedAt),
            customerName: Value(e.customerName),
            customerMobile: Value(e.customerMobile),
            waiterName: Value(e.waiterName),
            unpaidInvoiceNumber: Value(e.unpaidInvoiceNumber),
            paidAmount: Value(e.paidAmount),
            sessionNetworkStatus: Value(e.sessionNetworkStatus),
            sessionSyncStatus: const Value('1'),
            sessionVersion: Value(e.sessionVersion),
          ),
        )
        .toList();
    return db.upsertDiningSessionsFromCloud(companions);
  }

  Future<bool> downloadCompanyAndPrinter(String userId) async {
    final api = CompanyApi(ref.read(apiClientProvider));
    final db = ref.read(appDatabaseProvider);
    final companies = await api.getCompanyList(userId);
    final printers = await api.getCompanyPrinterSetting(userId);
    if (companies.isEmpty && printers.isEmpty) return false;

    if (companies.isNotEmpty) {
      final company = companies.first;
      final pending = await ref
          .read(shopReceiptProfileProvider.notifier)
          .isPendingUpload();
      final local = await db.getLocalCompany();
      final cloudEmpty =
          company.companyName.trim().isEmpty &&
          (company.shopName1 ?? '').trim().isEmpty;
      final localHas =
          local != null &&
          ((local.companyName ?? '').trim().isNotEmpty ||
              (local.shopName1 ?? '').trim().isNotEmpty);
      /* Keep local edits until they successfully upload. */
      if (pending || (cloudEmpty && localHas)) {
        /* Skip applying cloud company. */
      } else {
        await db.upsertLocalCompany(company);
        await ref
            .read(shopReceiptProfileProvider.notifier)
            .saveFromCompany(company);
      }
    }

    if (printers.isNotEmpty) {
      final pendingPrinter = await ref
          .read(printerSettingsProvider.notifier)
          .isPendingUpload();
      if (pendingPrinter) {
        /* Keep local printer edits until they successfully upload. */
      } else {
        final p = printers.first;
        await db.upsertLocalCompanyPrinterSettings(p);
        final current = ref.read(printerSettingsProvider);
        await ref
            .read(printerSettingsProvider.notifier)
            .update(
              current.copyWith(
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
                invoicePrefix: p.invoicePrefix.isNotEmpty
                    ? p.invoicePrefix
                    : current.invoicePrefix,
                kotPrefix: p.kotPrefix.isNotEmpty
                    ? p.kotPrefix
                    : current.kotPrefix,
                customerUse: printerFlagOn(p.customerUse),
                paymentUse: printerFlagOn(p.paymentUse),
                duplicateBillUse: printerFlagOn(p.duplicateBillUse),
                logoUse: printerFlagOn(p.logoUse),
                kotEnable: p.kotEnable != '0' && p.kotEnable != 'off',
                productQuantityUpdate: printerFlagOn(p.productQuantityUpdate),
                kotAutoPrint: p.kotAutoPrint == '1' || p.kotAutoPrint == 'on',
                kotPreview: p.kotPreview != '0' && p.kotPreview != 'off',
                kotCopies: int.tryParse(p.kotCopies) ?? current.kotCopies,
              ),
              fromCloud: true,
            );
      }
    }
    return true;
  }

  /* Drift row → API DTO (full shop details for upload). */
  CompanyDto companyDtoFromLocal(Company row) {
    return CompanyDto(
      companyId: row.companyId,
      companyName: row.companyName ?? '',
      cashierName: row.cashierName,
      companyMobile: row.companyMobile,
      companyAddress: row.companyAddress,
      shopName1: row.shopName1,
      shopName2: row.shopName2,
      addressLine1: row.addressLine1,
      addressLine2: row.addressLine2,
      addressLine3: row.addressLine3,
      phoneNo1: row.phoneNo1,
      phoneNo2: row.phoneNo2,
      currencyName: row.currencyName,
      countryName: row.countryName,
      stateName: row.stateName,
      tableStatus: row.tableStatus,
      noOfTable: row.noOfTable,
      gstStatus: row.gstStatus,
      gstNumber: row.gstNumber,
      shopCgst: row.shopCgst,
      shopSgst: row.shopSgst,
      panNumber: row.panNumber,
      companyFssis: row.companyFssis,
      companyLogo: row.companyLogo,
      paymentLogo: row.paymentLogo,
      openingMinutes: row.openingMinutes,
      closingMinutes: row.closingMinutes,
      companyStatus: row.companyStatus,
    );
  }

  Future<bool> uploadCompanyAndPrinter(String userId) async {
    final api = CompanyApi(ref.read(apiClientProvider));
    final db = ref.read(appDatabaseProvider);
    final session = ref.read(authControllerProvider).session;
    final settings = ref.read(printerSettingsProvider);
    final profile = ref.read(shopReceiptProfileProvider);

    /* Offline-first: Drift is source of truth. Prefs only if no local row. */
    final localCompany = await db.getLocalCompany();
    CompanyDto companyDto;
    if (localCompany != null) {
      companyDto = companyDtoFromLocal(localCompany);
    } else {
      companyDto = CompanyDto(
        companyName: profile.companyName.isNotEmpty
            ? profile.companyName
            : (profile.shopName1.isNotEmpty
                  ? profile.shopName1
                  : (session?.shopName ?? '')),
        companyMobile: profile.companyMobile.isNotEmpty
            ? profile.companyMobile
            : profile.phoneNo1,
        companyAddress: profile.companyAddress.isNotEmpty
            ? profile.companyAddress
            : [
                profile.addressLine1,
                profile.addressLine2,
                profile.addressLine3,
              ].where((e) => e.isNotEmpty).join(', '),
        shopName1: profile.shopName1,
        shopName2: profile.shopName2,
        addressLine1: profile.addressLine1,
        addressLine2: profile.addressLine2,
        addressLine3: profile.addressLine3,
        phoneNo1: profile.phoneNo1,
        phoneNo2: profile.phoneNo2,
        gstStatus: profile.gstEnabled ? '1' : '0',
        gstNumber: profile.gstNumber,
        panNumber: profile.panNumber,
        companyFssis: profile.companyFssis,
        paymentLogo: profile.paymentLogo,
        shopCgst: profile.shopCgst,
        shopSgst: profile.shopSgst,
        cashierName: profile.cashierName,
      );
      if (companyDto.companyName.trim().isNotEmpty ||
          (companyDto.shopName1 ?? '').trim().isNotEmpty) {
        await db.upsertLocalCompany(companyDto);
      }
    }

    /* Always attach local shop logo file for cloud `companyLogo`. */
    final logoPath = profile.logoLocalPath.trim().isNotEmpty
        ? profile.logoLocalPath
        : null;
    final logoData = await encodeShopLogoFile(logoPath) ??
        (companyDto.companyLogo != null &&
                companyDto.companyLogo!.startsWith('data:image')
            ? companyDto.companyLogo
            : null);
    if (logoData != null && logoData.isNotEmpty) {
      companyDto = CompanyDto(
        companyId: companyDto.companyId,
        companyLogo: logoData,
        companyName: companyDto.companyName,
        cashierName: companyDto.cashierName,
        companyMobile: companyDto.companyMobile,
        companyAddress: companyDto.companyAddress,
        shopName1: companyDto.shopName1,
        shopName2: companyDto.shopName2,
        addressLine1: companyDto.addressLine1,
        addressLine2: companyDto.addressLine2,
        addressLine3: companyDto.addressLine3,
        phoneNo1: companyDto.phoneNo1,
        phoneNo2: companyDto.phoneNo2,
        currencyName: companyDto.currencyName,
        countryName: companyDto.countryName,
        stateName: companyDto.stateName,
        tableStatus: companyDto.tableStatus,
        noOfTable: companyDto.noOfTable,
        gstStatus: companyDto.gstStatus,
        gstNumber: companyDto.gstNumber,
        shopCgst: companyDto.shopCgst,
        shopSgst: companyDto.shopSgst,
        panNumber: companyDto.panNumber,
        companyFssis: companyDto.companyFssis,
        paymentLogo: companyDto.paymentLogo,
        openingMinutes: companyDto.openingMinutes,
        closingMinutes: companyDto.closingMinutes,
        companyStatus: companyDto.companyStatus,
      );
      await db.upsertLocalCompany(companyDto);
    }

    await db.upsertLocalCompanyPrinterFromSettings(settings);

    final hasShop =
        companyDto.companyName.trim().isNotEmpty ||
        (companyDto.shopName1 ?? '').trim().isNotEmpty;
    final companyOk = hasShop
        ? await api.insertCompanyDetail(userId: userId, company: companyDto)
        : false;
    if (companyOk) {
      await ref
          .read(shopReceiptProfileProvider.notifier)
          .setPendingUpload(false);
    }
    final printerOk = await api.insertCompanyPrinterSetting(
      userId: userId,
      setting: CompanyPrinterSettingDto(
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
      ),
    );
    if (printerOk) {
      await ref
          .read(printerSettingsProvider.notifier)
          .setPendingUpload(false);
    }
    return companyOk || printerOk;
  }
}

final fullSyncControllerProvider =
    NotifierProvider<FullSyncController, AsyncValue<FullSyncResult?>>(
      FullSyncController.new,
    );
