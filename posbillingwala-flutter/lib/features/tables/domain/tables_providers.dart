import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/masters/data/masters_api.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_store.dart';
import 'package:pos_billingwala_v2/features/tables/data/dining_session_api.dart';

/* Android TableStatus — floor visual / operational status. */
enum FloorTableStatus {
  available,
  running,
  billRequested,
  paymentPending,
  partiallyPaid,
  blocked,
  reserved,
}

class FloorTableView {
  const FloorTableView({
    required this.table,
    required this.status,
    required this.currentAmount,
    this.remainingAmount = 0,
    this.openSession,
    this.joinedLabel,
    this.isJoinedSecondary = false,
    this.tableTypeName = '',
    this.unpaidInvoice,
    this.printRetryAvailable = false,
    this.sessionStartedAt,
    this.guestCount = 0,
  });

  final PosTable table;
  final FloorTableStatus status;
  final double currentAmount;
  final double remainingAmount;
  final DiningSession? openSession;
  final String? joinedLabel;
  final bool isJoinedSecondary;
  final String tableTypeName;
  final Invoice? unpaidInvoice;
  final bool printRetryAvailable;
  final DateTime? sessionStartedAt;
  final int guestCount;

  String get statusLabel {
    switch (status) {
      case FloorTableStatus.available:
        return 'Available';
      case FloorTableStatus.running:
        return 'Running';
      case FloorTableStatus.billRequested:
        return 'Bill Requested';
      case FloorTableStatus.paymentPending:
        return 'Payment Pending';
      case FloorTableStatus.partiallyPaid:
        return 'Partially Paid';
      case FloorTableStatus.blocked:
        return 'Blocked';
      case FloorTableStatus.reserved:
        return 'Reserved';
    }
  }

  bool get isOccupied =>
      status == FloorTableStatus.running ||
      status == FloorTableStatus.billRequested ||
      status == FloorTableStatus.paymentPending ||
      status == FloorTableStatus.partiallyPaid;

  /* Cart / billing always uses the primary table number. */
  String get billingTableNumber =>
      openSession?.primaryTableNumber ?? table.tableNumber;

  String get elapsedLabel {
    final started = sessionStartedAt ?? openSession?.startedAt;
    if (started == null) return '';
    final minutes = DateTime.now().difference(started).inMinutes;
    if (minutes < 0) return '';
    if (minutes < 60) return '$minutes min';
    final hours = minutes ~/ 60;
    final rem = minutes % 60;
    return '${hours}h ${rem}m';
  }
}

final posTablesProvider = StreamProvider<List<PosTable>>((ref) async* {
  final db = ref.watch(appDatabaseProvider);
  await db.seedDefaultTablesIfEmpty();
  yield* db.watchActivePosTables();
});

final openDiningSessionsProvider = StreamProvider<List<DiningSession>>((ref) {
  return ref.watch(appDatabaseProvider).watchOpenDiningSessions();
});

final allCartItemsProvider = StreamProvider<List<CartItem>>((ref) {
  return ref.watch(appDatabaseProvider).watchAllCartItems();
});

final unpaidTableInvoicesProvider = StreamProvider<List<Invoice>>((ref) {
  return ref.watch(appDatabaseProvider).watchUnpaidTableInvoices();
});

final failedPrintTableInvoicesProvider = StreamProvider<List<Invoice>>((ref) {
  return ref.watch(appDatabaseProvider).watchFailedPrintTableInvoices();
});

final floorTablesProvider = Provider<List<FloorTableView>>((ref) {
  final tables = ref
      .watch(posTablesProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <PosTable>[]);
  final sessions = ref
      .watch(openDiningSessionsProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <DiningSession>[]);
  final cart = ref
      .watch(allCartItemsProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <CartItem>[]);
  final unpaid = ref
      .watch(unpaidTableInvoicesProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <Invoice>[]);
  final failedPrint = ref
      .watch(failedPrintTableInvoicesProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <Invoice>[]);
  final typeRows = ref
      .watch(tableTypesProvider)
      .maybeWhen(data: (rows) => rows, orElse: () => const <TableType>[]);
  final typeNameById = <int, String>{
    for (final t in typeRows)
      if (t.tableTypeName.trim().isNotEmpty)
        t.tableTypeId: t.tableTypeName.trim(),
  };
  final db = ref.watch(appDatabaseProvider);

  final totals = <String, double>{};
  final hasCartByTable = <String, bool>{};
  for (final item in cart) {
    if (item.cartScope.isEmpty) continue;
    final line = item.unitPrice * item.quantity * (1 + item.gstPercent / 100);
    totals[item.cartScope] = (totals[item.cartScope] ?? 0) + line;
    hasCartByTable[item.cartScope] = true;
  }

  final unpaidByTable = <String, Invoice>{};
  for (final inv in unpaid) {
    final key = inv.noOfTable.trim();
    if (key.isEmpty) continue;
    unpaidByTable.putIfAbsent(key, () => inv);
  }

  final failedByTable = <String, Invoice>{};
  for (final inv in failedPrint) {
    final key = inv.noOfTable.trim();
    if (key.isEmpty) continue;
    failedByTable.putIfAbsent(key, () => inv);
  }

  final sessionByAnyTable = <String, DiningSession>{};
  for (final s in sessions) {
    sessionByAnyTable[s.primaryTableNumber] = s;
    for (final joined in db.parseJoinedTables(s.joinedTableNumbers)) {
      sessionByAnyTable[joined] = s;
    }
  }

  String typeNameFor(PosTable table) {
    final id = table.tableTypeId;
    if (id == null) return '';
    return typeNameById[id] ?? '';
  }

  FloorTableStatus statusFromSession(String? raw) {
    switch ((raw ?? '').trim().toUpperCase()) {
      case 'BILL_REQUESTED':
      case 'BILL_REQUEST':
        return FloorTableStatus.billRequested;
      case 'PAYMENT_PENDING':
        return FloorTableStatus.paymentPending;
      case 'PARTIALLY_PAID':
        return FloorTableStatus.partiallyPaid;
      case 'HOLD':
        /* Soft hold on Android — cart persists as RUNNING. */
        return FloorTableStatus.running;
      case 'RUNNING':
      default:
        return FloorTableStatus.running;
    }
  }

  final sessionsToClose = <int>{};

  final views = tables.map((table) {
    final override = table.statusOverride?.toUpperCase();
    if (override == 'BLOCKED') {
      return FloorTableView(
        table: table,
        status: FloorTableStatus.blocked,
        currentAmount: 0,
        tableTypeName: typeNameFor(table),
      );
    }
    if (override == 'RESERVED') {
      return FloorTableView(
        table: table,
        status: FloorTableStatus.reserved,
        currentAmount: 0,
        tableTypeName: typeNameFor(table),
      );
    }

    final session = sessionByAnyTable[table.tableNumber];
    final primary = session?.primaryTableNumber ?? table.tableNumber;
    final hasCart = hasCartByTable[primary] == true;
    final cartAmount = double.parse((totals[primary] ?? 0).toStringAsFixed(2));
    final unpaidInv = unpaidByTable[table.tableNumber] ?? unpaidByTable[primary];
    final failedInv = failedByTable[table.tableNumber] ?? failedByTable[primary];
    final hasUnpaid = unpaidInv != null;

    final joined = session == null
        ? const <String>[]
        : [
            session.primaryTableNumber,
            ...db.parseJoinedTables(session.joinedTableNumbers),
          ];
    final joinedLabel = joined.length > 1
        ? joined.map((e) => 'T$e').join(' + ')
        : null;
    final isSecondary =
        session != null && session.primaryTableNumber != table.tableNumber;

    FloorTableStatus status = FloorTableStatus.available;
    var amount = 0.0;
    var remaining = 0.0;
    var printRetry = false;
    Invoice? unpaidForView;
    DateTime? startedAt = session?.startedAt;
    var guests = session?.guestCount ?? 0;

    if (session != null) {
      final raw = session.sessionStatus.trim().toUpperCase();
      if (raw.isNotEmpty && raw != 'AVAILABLE') {
        status = statusFromSession(raw);
      }
    }

    if (hasCart) {
      amount = cartAmount;
      if (session != null) {
        final paid = session.paidAmount;
        remaining = (amount - paid).clamp(0, double.infinity).toDouble();
        if (paid > 0.05 && remaining > 0.05) {
          status = FloorTableStatus.partiallyPaid;
        }
      }
      if (session == null) {
        status = FloorTableStatus.running;
      } else if (status == FloorTableStatus.available) {
        status = FloorTableStatus.running;
      }
    } else if (hasUnpaid) {
      unpaidForView = unpaidInv;
      amount = unpaidInv.totalAmount;
      remaining = unpaidInv.totalAmount;
      if (status == FloorTableStatus.available ||
          status == FloorTableStatus.running) {
        status = FloorTableStatus.paymentPending;
      }
      if (unpaidInv.billPrintStatus.toUpperCase() == 'FAILED') {
        printRetry = true;
      }
    } else {
      if (failedInv != null) {
        printRetry = true;
        unpaidForView = failedInv;
      }
      if (session != null && failedInv == null) {
        sessionsToClose.add(session.sessionId);
        return FloorTableView(
          table: table,
          status: FloorTableStatus.available,
          currentAmount: 0,
          tableTypeName: typeNameFor(table),
        );
      }
      status = FloorTableStatus.available;
      amount = 0;
      remaining = 0;
      startedAt = null;
      guests = 0;
    }

    return FloorTableView(
      table: table,
      status: status,
      currentAmount: isSecondary ? 0 : amount,
      remainingAmount: isSecondary ? 0 : remaining,
      openSession: (status == FloorTableStatus.available && !printRetry)
          ? null
          : session,
      joinedLabel: status == FloorTableStatus.available ? null : joinedLabel,
      isJoinedSecondary: isSecondary && status != FloorTableStatus.available,
      tableTypeName: typeNameFor(table),
      unpaidInvoice: unpaidForView,
      printRetryAvailable: printRetry,
      sessionStartedAt: startedAt,
      guestCount: guests,
    );
  }).toList();

  if (sessionsToClose.isNotEmpty) {
    Future.microtask(() => db.closeDiningSessions(sessionsToClose));
  }

  return views;
});

class TablesController extends Notifier<AsyncValue<void>> {
  @override
  AsyncValue<void> build() => const AsyncData(null);

  Future<void> syncTables() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      state = AsyncError('Please login first', StackTrace.current);
      return;
    }

    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final db = ref.read(appDatabaseProvider);
      final api = MastersApi(ref.read(apiClientProvider));
      try {
        final tables = await api.fetchPosTables(userId);
        if (tables.isNotEmpty) {
          await db.replacePosTables(
            tables
                .where((e) => e.tableId > 0 && e.tableNumber.trim().isNotEmpty)
                .map(
                  (e) => PosTablesCompanion.insert(
                    tableId: Value(e.tableId),
                    tableNumber: e.tableNumber,
                    displayName: Value(
                      e.tableName.isEmpty
                          ? 'Table ${e.tableNumber}'
                          : e.tableName,
                    ),
                    capacity: Value(e.capacity),
                    areaId: Value(e.areaId),
                    tableTypeId: Value(e.tableTypeId),
                    tableActive: Value(e.tableActive),
                    sortOrder: Value(e.sortOrder),
                    statusOverride: Value(e.statusOverride),
                    posTableNetworkStatus: Value(e.posTableNetworkStatus),
                    posTableStatus: const Value('1'),
                  ),
                )
                .toList(),
          );
        } else {
          await db.seedDefaultTablesIfEmpty();
        }
      } catch (_) {
        await db.seedDefaultTablesIfEmpty();
      }
    });
  }

  Future<void> uploadDiningSessionIfOnline(DiningSession session) async {
    await requireOnlineForWeb();
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      if (AppPlatform.requiresNetwork) {
        throw StateError('Please login to save table session on Web POS.');
      }
      return;
    }
    if (!await isDeviceOnline()) {
      if (AppPlatform.requiresNetwork) {
        throw StateError(kOnlineRequiredMessage);
      }
      return;
    }
    try {
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
      final ok = await DiningSessionApi(
        ref.read(apiClientProvider),
      ).insertDiningSession(userId: userId, session: dto);
      if (ok) {
        await ref
            .read(appDatabaseProvider)
            .markDiningSessionSynced(session.sessionId);
      } else if (AppPlatform.requiresNetwork) {
        throw StateError(kWebApiSaveFailedMessage);
      }
    } catch (e) {
      if (AppPlatform.requiresNetwork) {
        if (e is StateError) rethrow;
        throw StateError(kWebApiSaveFailedMessage);
      }
      /* Mobile: keep pending for reconnect / Sync button. */
    }
  }

  Future<BillingSession> openTable(FloorTableView floor) async {
    if (floor.status == FloorTableStatus.blocked ||
        floor.status == FloorTableStatus.reserved) {
      throw StateError('Table is ${floor.statusLabel.toLowerCase()}');
    }

    final db = ref.read(appDatabaseProvider);
    final staff = await StaffStore().read();
    final waiter = staff?.name.trim();
    final session = await db.openOrGetDiningSession(
      floor.billingTableNumber,
      waiterName: (waiter != null && waiter.isNotEmpty) ? waiter : null,
    );
    await uploadDiningSessionIfOnline(session);
    final label =
        floor.joinedLabel ??
        (floor.table.displayName.isEmpty
            ? floor.table.tableNumber
            : floor.table.displayName);
    ref
        .read(billingSessionProvider.notifier)
        .startTableBilling(
          tableNumber: session.primaryTableNumber,
          tableName: label,
          diningSessionId: session.sessionId,
        );
    return ref.read(billingSessionProvider);
  }

  /* Android soft Hold / Save — ensure session exists; cart stays RUNNING. */
  Future<void> softHoldTable(FloorTableView floor) async {
    final session = await ref
        .read(appDatabaseProvider)
        .openOrGetDiningSession(floor.billingTableNumber);
    await uploadDiningSessionIfOnline(session);
  }

  Future<DiningSession> joinTables({
    required String primaryTable,
    required String secondaryTable,
  }) async {
    final session = await ref
        .read(appDatabaseProvider)
        .joinTables(primaryTable: primaryTable, secondaryTable: secondaryTable);
    await uploadDiningSessionIfOnline(session);
    return session;
  }

  Future<void> splitJoined(int sessionId) async {
    final db = ref.read(appDatabaseProvider);
    await db.splitJoinedTables(sessionId);
    final session = await db.getDiningSessionById(sessionId);
    if (session != null) await uploadDiningSessionIfOnline(session);
  }

  Future<void> transferTable({
    required String fromTable,
    required String toTable,
  }) async {
    final db = ref.read(appDatabaseProvider);
    await db.transferTable(fromTable: fromTable, toTable: toTable);
    final session = await db.openOrGetDiningSession(toTable);
    await uploadDiningSessionIfOnline(session);
  }

  Future<void> setSessionStatus({
    required int sessionId,
    required String status,
  }) async {
    final db = ref.read(appDatabaseProvider);
    await db.setDiningSessionStatus(sessionId: sessionId, status: status);
    final session = await db.getDiningSessionById(sessionId);
    if (session != null) await uploadDiningSessionIfOnline(session);
  }

  Future<void> markBillRequested(FloorTableView floor) async {
    final session = await ref
        .read(appDatabaseProvider)
        .openOrGetDiningSession(floor.billingTableNumber);
    await setSessionStatus(
      sessionId: session.sessionId,
      status: 'BILL_REQUESTED',
    );
  }

  Future<void> moveItems({
    required String fromTable,
    required String toTable,
    required List<CartItem> items,
  }) async {
    await ref
        .read(appDatabaseProvider)
        .moveCartItemsToTable(
          fromTable: fromTable,
          toTable: toTable,
          items: items,
        );
    final session = await ref
        .read(appDatabaseProvider)
        .openOrGetDiningSession(toTable);
    await uploadDiningSessionIfOnline(session);
  }

  Future<void> updateSessionMeta({
    required int sessionId,
    String? waiterName,
    int? guestCount,
  }) async {
    final db = ref.read(appDatabaseProvider);
    await db.updateDiningSessionMeta(
      sessionId,
      waiterName: waiterName,
      guestCount: guestCount,
    );
    final session = await db.getDiningSessionById(sessionId);
    if (session != null) await uploadDiningSessionIfOnline(session);
  }

  Future<void> settleUnpaidInvoice({
    required FloorTableView floor,
    required Invoice invoice,
    required String paymentMode,
    double cashAmount = 0,
    double upiAmount = 0,
  }) async {
    final db = ref.read(appDatabaseProvider);
    await db.updateInvoiceTablePaymentMode(
      invoiceNumber: invoice.invoiceNumber,
      tableNumber: invoice.noOfTable,
      paymentMode: paymentMode,
      cashAmount: cashAmount,
      upiAmount: upiAmount,
    );
    final session = floor.openSession;
    if (session != null) {
      await db.closeDiningSession(session.sessionId);
    } else {
      final open = await db.getOpenSessionForTable(floor.billingTableNumber);
      if (open != null) await db.closeDiningSession(open.sessionId);
    }
  }
}

final tablesControllerProvider =
    NotifierProvider<TablesController, AsyncValue<void>>(TablesController.new);
