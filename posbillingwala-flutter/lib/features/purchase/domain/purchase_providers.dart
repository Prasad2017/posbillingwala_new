import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/enterprise/data/enterprise_api.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/data/warehouse_stock_store.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/domain/enterprise_runtime.dart';
import 'package:pos_billingwala_v2/features/inventory/domain/inventory_providers.dart';
import 'package:pos_billingwala_v2/features/purchase/data/purchase_local_store.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_document.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/vendor.dart';

final vendorsProvider = FutureProvider<List<Vendor>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];
  final local = await PurchaseLocalStore.loadVendors(session);
  if (await isDeviceOnline()) {
    try {
      final remote = await ref
          .read(enterpriseApiProvider)
          .fetchVendors(session.licenceUserId);
      if (remote.isNotEmpty) {
        await PurchaseLocalStore.saveVendors(session, remote);
        return remote;
      }
    } catch (_) {
      /* Keep local cache when pull fails. */
    }
  }
  return local;
});

final activeVendorsProvider = Provider<List<Vendor>>((ref) {
  final all = ref.watch(vendorsProvider).maybeWhen(
        data: (v) => v,
        orElse: () => const <Vendor>[],
      );
  return all.where((e) => e.isActive).toList();
});

final purchaseDocumentsProvider =
    FutureProvider<List<PurchaseDocument>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];
  final local = await PurchaseLocalStore.loadDocuments(session);
  if (await isDeviceOnline()) {
    try {
      final remote = await ref
          .read(enterpriseApiProvider)
          .fetchPurchaseDocuments(session.licenceUserId);
      if (remote.isNotEmpty) {
        await PurchaseLocalStore.saveDocuments(session, remote);
        return remote;
      }
    } catch (_) {}
  }
  return local;
});

final purchaseOrdersProvider = Provider<List<PurchaseDocument>>((ref) {
  final docs = ref.watch(purchaseDocumentsProvider).maybeWhen(
        data: (v) => v,
        orElse: () => const <PurchaseDocument>[],
      );
  return docs
      .where(
        (d) =>
            d.type == PurchaseDocType.order ||
            d.type == PurchaseDocType.request,
      )
      .toList();
});

final purchaseGrnsProvider = Provider<List<PurchaseDocument>>((ref) {
  final docs = ref.watch(purchaseDocumentsProvider).maybeWhen(
        data: (v) => v,
        orElse: () => const <PurchaseDocument>[],
      );
  return docs.where((d) => d.type == PurchaseDocType.grn).toList();
});

class PurchaseController extends Notifier<AsyncValue<String?>> {
  @override
  AsyncValue<String?> build() => const AsyncData(null);

  void _invalidate() {
    ref.invalidate(vendorsProvider);
    ref.invalidate(purchaseDocumentsProvider);
  }

  Future<Vendor> saveVendor({
    String? id,
    required String name,
    String gstin = '',
    String contactName = '',
    String mobile = '',
    String email = '',
    String address = '',
    String paymentTerms = 'Net 30',
    double creditLimit = 0,
    double openingBalance = 0,
    String bankDetails = '',
    String status = 'ACTIVE',
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) throw StateError('Not signed in');
    final trimmed = name.trim();
    if (trimmed.isEmpty) throw StateError('Vendor name is required');

    final existingId = id?.trim();
    final vendor = Vendor(
      id: (existingId == null || existingId.isEmpty)
          ? PurchaseLocalStore.newVendorId()
          : existingId,
      name: trimmed,
      gstin: gstin.trim(),
      contactName: contactName.trim(),
      mobile: mobile.trim(),
      email: email.trim(),
      address: address.trim(),
      paymentTerms: paymentTerms.trim().isEmpty ? 'Net 30' : paymentTerms.trim(),
      creditLimit: creditLimit,
      openingBalance: openingBalance,
      bankDetails: bankDetails.trim(),
      status: status,
    );
    final saved = await PurchaseLocalStore.upsertVendor(session, vendor);
    _invalidate();
    if (await isDeviceOnline()) {
      try {
        await ref.read(enterpriseApiProvider).saveVendor(
              userId: session.licenceUserId,
              vendor: saved,
            );
      } catch (_) {}
    }
    return saved;
  }

  Future<void> deactivateVendor(String vendorId) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) return;
    await PurchaseLocalStore.deactivateVendor(session, vendorId);
    _invalidate();
  }

  Future<PurchaseDocument> savePurchaseOrder({
    String? id,
    required String vendorId,
    required String vendorName,
    required List<PurchaseLine> lines,
    String notes = '',
    String referenceNo = '',
    PurchaseDocType type = PurchaseDocType.order,
    PurchaseDocStatus status = PurchaseDocStatus.ordered,
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) throw StateError('Not signed in');
    if (vendorId.trim().isEmpty) throw StateError('Select a vendor');
    if (lines.isEmpty) throw StateError('Add at least one product line');

    final existingId = id?.trim();
    final isNew = existingId == null || existingId.isEmpty;
    final docId = isNew ? PurchaseLocalStore.newDocId(type) : existingId;
    final docNo = isNew
        ? await PurchaseLocalStore.nextDocNo(session, type)
        : (await PurchaseLocalStore.getDocument(session, docId))?.docNo ??
              await PurchaseLocalStore.nextDocNo(session, type);

    final doc = PurchaseDocument(
      id: docId,
      docNo: docNo,
      type: type,
      status: status,
      vendorId: vendorId,
      vendorName: vendorName,
      lines: lines,
      notes: notes.trim(),
      referenceNo: referenceNo.trim(),
      createdBy: session.userName?.trim() ?? session.displayName,
      createdAt: isNew
          ? DateTime.now()
          : (await PurchaseLocalStore.getDocument(session, docId))?.createdAt,
    );
    final saved = await PurchaseLocalStore.upsertDocument(session, doc);
    _invalidate();
    if (await isDeviceOnline()) {
      try {
        await ref.read(enterpriseApiProvider).savePurchaseDocument(
              userId: session.licenceUserId,
              doc: saved,
            );
      } catch (_) {}
    }
    /* Auto-create approval queue entry for submitted/ordered POs. */
    if (type == PurchaseDocType.order &&
        (status == PurchaseDocStatus.ordered ||
            status == PurchaseDocStatus.submitted)) {
      try {
        await EnterpriseRuntime.createPurchaseApproval(
          ref,
          docNo: saved.docNo,
          vendorName: saved.vendorName,
          amount: saved.subTotal,
          requestedBy: saved.createdBy,
        );
      } catch (_) {}
    }
    return saved;
  }

  /* Receive against a PO: create GRN + push stock via inventory ledger. */
  Future<PurchaseDocument> receiveGrn({
    required String purchaseOrderId,
    required List<PurchaseLine> receiveLines,
    String notes = '',
    String referenceNo = '',
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) throw StateError('Not signed in');

    final po = await PurchaseLocalStore.getDocument(session, purchaseOrderId);
    if (po == null) throw StateError('Purchase order not found');
    if (!po.canReceive) {
      throw StateError('This order cannot be received');
    }
    final approved =
        await EnterpriseRuntime.isPurchaseApproved(ref, po.docNo);
    if (!approved) {
      throw StateError('Purchase order pending approval — cannot receive');
    }

    final positive = receiveLines.where((l) => l.quantity > 0).toList();
    if (positive.isEmpty) throw StateError('Enter quantities to receive');

    final inventory = ref.read(inventoryControllerProvider.notifier);
    for (final line in positive) {
      await inventory.addPurchase(
        productId: line.productId,
        productName: line.productName,
        quantity: line.quantity,
        note: 'GRN from ${po.docNo}',
        unitCost: line.unitCost,
      );
      /* Credit default warehouse ledger. */
      await WarehouseStockStore.adjust(
        session,
        warehouse: WarehouseStockStore.defaultWarehouse,
        productId: line.productId,
        delta: line.quantity,
      );
      /* Create batch lot for FEFO tracking when note has batch/expiry. */
      await EnterpriseRuntime.createLotFromGrn(
        ref,
        productName: line.productName,
        productId: line.productId,
        qty: line.quantity,
        batchNo: line.note,
        warehouse: WarehouseStockStore.defaultWarehouse,
      );
    }

    /* Update PO received qty / status. */
    final updatedPoLines = <PurchaseLine>[];
    for (final existing in po.lines) {
      double add = 0;
      for (final r in positive) {
        if (r.productId == existing.productId) {
          add += r.quantity;
        }
      }
      updatedPoLines.add(
        existing.copyWith(receivedQty: existing.receivedQty + add),
      );
    }
    final allDone = updatedPoLines.every((l) => l.receivedQty >= l.quantity);
    final anyReceived = updatedPoLines.any((l) => l.receivedQty > 0);
    final poStatus = allDone
        ? PurchaseDocStatus.received
        : (anyReceived
              ? PurchaseDocStatus.partial
              : po.status);

    await PurchaseLocalStore.upsertDocument(
      session,
      po.copyWith(
        lines: updatedPoLines,
        status: poStatus,
        updatedAt: DateTime.now(),
      ),
    );

    final grn = PurchaseDocument(
      id: PurchaseLocalStore.newDocId(PurchaseDocType.grn),
      docNo: await PurchaseLocalStore.nextDocNo(session, PurchaseDocType.grn),
      type: PurchaseDocType.grn,
      status: PurchaseDocStatus.received,
      vendorId: po.vendorId,
      vendorName: po.vendorName,
      lines: positive,
      notes: notes.trim(),
      referenceNo: referenceNo.trim().isEmpty ? po.docNo : referenceNo.trim(),
      parentDocId: po.id,
      createdBy: session.userName?.trim() ?? session.displayName,
      createdAt: DateTime.now(),
      receivedAt: DateTime.now(),
    );
    final saved = await PurchaseLocalStore.upsertDocument(session, grn);
    if (await isDeviceOnline()) {
      try {
        final api = ref.read(enterpriseApiProvider);
        await api.savePurchaseDocument(
          userId: session.licenceUserId,
          doc: po.copyWith(lines: updatedPoLines, status: poStatus),
        );
        await api.savePurchaseDocument(
          userId: session.licenceUserId,
          doc: saved,
        );
      } catch (_) {}
    }
    _invalidate();
    return saved;
  }
}

final purchaseControllerProvider =
    NotifierProvider<PurchaseController, AsyncValue<String?>>(
      PurchaseController.new,
    );
