import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/company/data/company_api.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
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
}

/* Android-parity sync orchestrator: */
/* - Upload pending offline rows to server */
/* - Download / refresh cloud data into local DB */
class FullSyncController extends Notifier<AsyncValue<FullSyncResult?>> {
  @override
  AsyncValue<FullSyncResult?> build() => const AsyncData(null);

  Future<FullSyncResult> syncEverything() =>
      run(FullSyncMode.both, trackProgress: false);

  /* Web background refresh: upload pending then download without UI loading. */
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

    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      progress.setBlocked(
        headline: 'Please login first',
        subtitle: 'Sign in before fetching cloud data.',
      );
      const result = FullSyncResult(message: 'Please login first', failed: 1);
      state = const AsyncData(result);
      return result;
    }

    final pendingBills =
        await ref.read(appDatabaseProvider).countPendingSyncInvoices();
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
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      const result = FullSyncResult(message: 'Please login first', failed: 1);
      state = const AsyncData(result);
      return result;
    }
    state = const AsyncLoading();
    try {
      await ref.read(appDatabaseProvider).resetOperationalDataForFetch();
      final result =
          await run(FullSyncMode.downloadOnly, trackProgress: false);
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
        mastersUploaded = await ref
            .read(mastersRepositoryProvider)
            .uploadPendingMasters(
              ownerId: ownerId,
              licenceUserId: userId,
            );
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
        final upload =
            await ref.read(invoiceSyncControllerProvider.notifier).uploadPending();
        invoicesUploaded = upload.uploaded;
        failed += upload.failed;
        notes.add('bills↑$invoicesUploaded');
        if (upload.failed > 0) {
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
        messUploaded = await uploadPendingMess(userId, db);
        notes.add('mess↑$messUploaded');
        progress?.markComplete(messStepIds);
      } catch (_) {
        failed++;
        notes.add('mess↑ error');
        progress?.markError(messStepIds);
      }

      try {
        diningUploaded = await uploadPendingDining(userId, db);
        notes.add('dining↑$diningUploaded');
      } catch (_) {
        failed++;
        notes.add('dining↑ error');
      }

      progress?.markRunning(inventoryStepIds);
      try {
        await ref.read(inventoryControllerProvider.notifier).syncAll();
        inventorySynced = true;
        notes.add('inventory↑ ok');
        progress?.markComplete(inventoryStepIds);
      } catch (_) {
        failed++;
        notes.add('inventory↑ error');
        progress?.markError(inventoryStepIds);
      }
    }

    /* ---- DOWNLOAD (server → local) — same spirit as Android NetworkDataFetcher */
    if (doDownload) {
      const masterFetchIds = [
        'categories',
        'subcategories',
        'products',
        'portion_master',
        'portions',
        'combos',
        'combo_items',
      ];
      /* Food types are also replaced inside syncFromCloud (no separate UI step). */
      progress?.markRunning([masterFetchIds.first]);
      try {
        await ref.read(mastersRepositoryProvider).syncFromCloud(
              ownerId: ownerId,
              licenceUserId: userId,
            );
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
      } catch (_) {
        failed++;
        notes.add('dining↓ error');
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
        messInvoiceDate:
            DateFormat('yyyy-MM-dd HH:mm:ss').format(coupon.messInvoiceDate),
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
    final cloudSessions =
        await diningApi.fetchDiningSessions(userId, openOnly: true);
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
      await db.upsertLocalCompany(company);
      await ref.read(shopReceiptProfileProvider.notifier).saveFromCompany(company);
    }

    if (printers.isNotEmpty) {
      final p = printers.first;
      await db.upsertLocalCompanyPrinterSettings(p);
      final current = ref.read(printerSettingsProvider);
      await ref.read(printerSettingsProvider.notifier).update(
            current.copyWith(
              billBluetoothAddress: p.bluetoothAddress.isNotEmpty
                  ? p.bluetoothAddress
                  : current.billBluetoothAddress,
              kotBluetoothAddress: p.bluetoothKotAddress.isNotEmpty
                  ? p.bluetoothKotAddress
                  : current.kotBluetoothAddress,
              feedLines:
                  int.tryParse(p.printerFeedLines) ?? current.feedLines,
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
              kotPrefix: p.kotPrefix.isNotEmpty ? p.kotPrefix : current.kotPrefix,
              customerUse: p.customerUse == '1',
              paymentUse: p.paymentUse == '1',
              duplicateBillUse: p.duplicateBillUse == '1',
              logoUse: p.logoUse == '1',
              kotEnable: p.kotEnable != '0' && p.kotEnable != 'off',
              productQuantityUpdate: p.productQuantityUpdate == '1',
              kotAutoPrint: p.kotAutoPrint == '1' || p.kotAutoPrint == 'on',
              kotPreview: p.kotPreview != '0' && p.kotPreview != 'off',
              kotCopies: int.tryParse(p.kotCopies) ?? current.kotCopies,
            ),
          );
    }
    return true;
  }

  Future<bool> uploadCompanyAndPrinter(String userId) async {
    final api = CompanyApi(ref.read(apiClientProvider));
    final db = ref.read(appDatabaseProvider);
    final session = ref.read(authControllerProvider).session;
    final settings = ref.read(printerSettingsProvider);
    final profile = ref.read(shopReceiptProfileProvider);

    /* Prefer local Drift snapshot, then prefs profile, then cloud/session. */
    final localCompany = await db.getLocalCompany();
    final companies = await api.getCompanyList(userId);
    final base = companies.isNotEmpty
        ? companies.first
        : CompanyDto(companyName: session?.shopName ?? '');
    final companyDto = CompanyDto(
      companyId: localCompany?.companyId ?? base.companyId,
      companyName: profile.companyName.isNotEmpty
          ? profile.companyName
          : (base.companyName.isNotEmpty
              ? base.companyName
              : (session?.shopName ?? '')),
      companyMobile: profile.companyMobile.isNotEmpty
          ? profile.companyMobile
          : base.companyMobile,
      companyAddress: profile.companyAddress.isNotEmpty
          ? profile.companyAddress
          : base.companyAddress,
      shopName1:
          profile.shopName1.isNotEmpty ? profile.shopName1 : base.shopName1,
      shopName2:
          profile.shopName2.isNotEmpty ? profile.shopName2 : base.shopName2,
      addressLine1: profile.addressLine1.isNotEmpty
          ? profile.addressLine1
          : base.addressLine1,
      addressLine2: profile.addressLine2.isNotEmpty
          ? profile.addressLine2
          : base.addressLine2,
      addressLine3: profile.addressLine3.isNotEmpty
          ? profile.addressLine3
          : base.addressLine3,
      phoneNo1: profile.phoneNo1.isNotEmpty ? profile.phoneNo1 : base.phoneNo1,
      phoneNo2: profile.phoneNo2.isNotEmpty ? profile.phoneNo2 : base.phoneNo2,
      gstStatus: profile.gstEnabled ? '1' : (base.gstStatus ?? '0'),
      gstNumber:
          profile.gstNumber.isNotEmpty ? profile.gstNumber : base.gstNumber,
      panNumber:
          profile.panNumber.isNotEmpty ? profile.panNumber : base.panNumber,
      companyFssis: profile.companyFssis.isNotEmpty
          ? profile.companyFssis
          : base.companyFssis,
      paymentLogo: profile.paymentLogo.isNotEmpty
          ? profile.paymentLogo
          : base.paymentLogo,
      shopCgst: profile.shopCgst.isNotEmpty ? profile.shopCgst : base.shopCgst,
      shopSgst: profile.shopSgst.isNotEmpty ? profile.shopSgst : base.shopSgst,
      cashierName: profile.cashierName.isNotEmpty
          ? profile.cashierName
          : base.cashierName,
    );
    await db.upsertLocalCompany(companyDto);
    await db.upsertLocalCompanyPrinterFromSettings(settings);

    final companyOk = await api.insertCompanyDetail(
      userId: userId,
      company: companyDto,
    );
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
        customerUse: settings.customerUse ? '1' : '0',
        paymentUse: settings.paymentUse ? '1' : '0',
        duplicateBillUse: settings.duplicateBillUse ? '1' : '0',
        logoUse: settings.logoUse ? '1' : '0',
        kotEnable: settings.kotEnable ? '1' : '0',
        productQuantityUpdate: settings.productQuantityUpdate ? '1' : '0',
        kotAutoPrint: settings.kotAutoPrint ? '1' : '0',
        kotPreview: settings.kotPreview ? '1' : '0',
        kotCopies: '${settings.kotCopies}',
      ),
    );
    return companyOk || printerOk;
  }
}

final fullSyncControllerProvider =
    NotifierProvider<FullSyncController, AsyncValue<FullSyncResult?>>(
  FullSyncController.new,
);
