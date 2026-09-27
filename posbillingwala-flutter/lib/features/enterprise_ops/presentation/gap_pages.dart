import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/data/hold_cart_store.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/domain/ops_providers.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/presentation/ops_pages.dart';
import 'package:pos_billingwala_v2/features/hotel/presentation/hotel_pages.dart';
import 'package:pos_billingwala_v2/features/masters/domain/masters_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/retail/presentation/retail_module_pages.dart';

final heldInvoicesProvider = FutureProvider<List<HeldInvoice>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];
  return HoldCartStore.load(session);
});

/* Camera / HID barcode → product lookup for POS. */
class PosBarcodeScanPage extends ConsumerStatefulWidget {
  const PosBarcodeScanPage({super.key});

  @override
  ConsumerState<PosBarcodeScanPage> createState() => _PosBarcodeScanPageState();
}

class _PosBarcodeScanPageState extends ConsumerState<PosBarcodeScanPage> {
  MobileScannerController? _ctrl;
  String? _last;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _ctrl = MobileScannerController(
      detectionSpeed: DetectionSpeed.normal,
      facing: CameraFacing.back,
    );
  }

  @override
  void dispose() {
    _ctrl?.dispose();
    super.dispose();
  }

  Future<void> _onDetect(BarcodeCapture capture) async {
    if (_busy) return;
    final raw = capture.barcodes
        .map((b) => b.rawValue)
        .whereType<String>()
        .map((e) => e.trim())
        .firstWhere((e) => e.isNotEmpty, orElse: () => '');
    if (raw.isEmpty || raw == _last) return;
    _last = raw;
    setState(() => _busy = true);
    try {
      final ok = await addProductByBarcode(ref, raw);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(ok ? 'Added $raw' : 'No product for $raw'),
          duration: const Duration(milliseconds: 900),
        ),
      );
      if (ok) {
        await Future<void>.delayed(const Duration(milliseconds: 400));
        _last = null;
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Scan barcode')),
      body: Stack(
        children: [
          MobileScanner(controller: _ctrl, onDetect: _onDetect),
          if (_busy)
            const Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: EdgeInsets.all(24),
                child: CircularProgressIndicator(),
              ),
            ),
        ],
      ),
    );
  }
}

Future<bool> addProductByBarcode(WidgetRef ref, String code) async {
  final q = code.trim().toLowerCase();
  if (q.isEmpty) return false;
  final products = await ref.read(allProductsProvider.future);
  for (final p in products) {
    final pc = (p.productCode ?? '').trim().toLowerCase();
    if (pc == q) {
      await ref.read(posCartControllerProvider.notifier).addProduct(p);
      return true;
    }
  }
  /* Variant barcode / SKU. */
  try {
    final variants = await ref.read(variantsProvider.future);
    for (final v in variants) {
      final barcode = v.barcode.trim().toLowerCase();
      final sku = v.sku.trim().toLowerCase();
      if (barcode == q || sku == q) {
        final pid = v.productId > 0 ? v.productId : (v.id.hashCode.abs() % 100000);
        final product = Product(
          productId: pid,
          productName: '${v.productName} (${v.sizeLabel}/${v.colorLabel})',
          productPrice: v.sellingPrice,
          productMrp: v.mrp > 0 ? v.mrp : v.sellingPrice,
          priceIncludesGst: '0',
          openPrice: '0',
          productCgst: 0,
          productSgst: 0,
          productWithGstPrice: v.sellingPrice,
          productDeletedStatus: '0',
          productStatus: '1',
          productSyncStatus: '0',
          productCode: v.sku.isNotEmpty ? v.sku : v.barcode,
        );
        await ref.read(posCartControllerProvider.notifier).addProduct(
              product,
              unitPriceOverride: v.sellingPrice,
            );
        return true;
      }
    }
  } catch (_) {}
  return false;
}

Future<void> holdCurrentCart(WidgetRef ref, {String note = ''}) async {
  final session = ref.read(authControllerProvider).session;
  final billing = ref.read(billingSessionProvider);
  if (session == null) throw StateError('Not signed in');
  final items = await ref.read(cartItemsProvider.future);
  if (items.isEmpty) throw StateError('Cart is empty');
  final lines = items
      .map(
        (e) => {
          'productId': e.productId,
          'productName': e.productName,
          'quantity': e.quantity,
          'unitPrice': e.unitPrice,
          'portionId': e.portionId,
          'portionName': e.portionName,
          'lineType': e.lineType,
          'comboId': e.comboId,
        },
      )
      .toList();
  final hold = HeldInvoice(
    id: 'hold_${DateTime.now().millisecondsSinceEpoch}',
    label: 'Hold ${DateTime.now().hour}:${DateTime.now().minute.toString().padLeft(2, '0')}',
    createdAt: DateTime.now(),
    lines: lines,
    customerName: billing.customerName ?? '',
    customerPhone: billing.customerPhone ?? '',
    customerId: billing.customerId ?? '',
    note: note,
  );
  await HoldCartStore.upsert(session, hold);
  await ref.read(posCartControllerProvider.notifier).clear();
  ref.invalidate(heldInvoicesProvider);
}

Future<void> resumeHeldInvoice(WidgetRef ref, HeldInvoice hold) async {
  final session = ref.read(authControllerProvider).session;
  if (session == null) return;
  await ref.read(posCartControllerProvider.notifier).clear();
  final db = ref.read(appDatabaseProvider);
  for (final line in hold.lines) {
    final pid = int.tryParse('${line['productId']}') ?? 0;
    if (pid <= 0) continue;
    final product = await db.getProduct(pid);
    if (product == null) continue;
    final qty = double.tryParse('${line['quantity']}') ?? 1;
    final price = double.tryParse('${line['unitPrice']}');
    await ref.read(posCartControllerProvider.notifier).addProduct(
          product,
          quantity: qty,
          unitPriceOverride: price,
        );
  }
  if (hold.customerName.isNotEmpty || hold.customerId.isNotEmpty) {
    ref.read(billingSessionProvider.notifier).updateCustomer(
          id: hold.customerId.isEmpty ? null : hold.customerId,
          name: hold.customerName,
          phone: hold.customerPhone,
        );
  }
  await HoldCartStore.remove(session, hold.id);
  ref.invalidate(heldInvoicesProvider);
}

class HeldInvoicesPage extends ConsumerWidget {
  const HeldInvoicesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(heldInvoicesProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Held invoices')),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (list) {
          if (list.isEmpty) {
            return const Center(child: Text('No held bills'));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: list.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (context, i) {
              final h = list[i];
              return ListTile(
                tileColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                title: Text(h.label, style: const TextStyle(fontWeight: FontWeight.w700)),
                subtitle: Text(
                  '${h.lines.length} lines'
                  '${h.customerName.isEmpty ? '' : ' · ${h.customerName}'}'
                  '\n${h.createdAt}',
                ),
                isThreeLine: true,
                trailing: FilledButton(
                  onPressed: () async {
                    await resumeHeldInvoice(ref, h);
                    if (context.mounted) context.go('/pos');
                  },
                  child: const Text('Resume'),
                ),
              );
            },
          );
        },
      ),
    );
  }
}

class NightAuditPage extends ConsumerWidget {
  const NightAuditPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final bookings = ref.watch(hotelBookingsProvider);
    final rooms = ref.watch(hotelRoomsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Night audit')),
      body: bookings.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (list) {
          final inHouse = list.where((b) => b.bookingStatus == 'CHECKED_IN').toList();
          final occupied = rooms.maybeWhen(
            data: (r) => r.where((x) => x.roomStatus == 'OCCUPIED').length,
            orElse: () => 0,
          );
          var folioSum = 0.0;
          for (final b in inHouse) {
            folioSum += b.folioTotal;
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _kpi('In-house guests', '${inHouse.length}'),
              _kpi('Occupied rooms', '$occupied'),
              _kpi('Open folio total', '₹${folioSum.toStringAsFixed(0)}'),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () async {
                  /* Post room night charge = roomCharges stay; stamp audit note. */
                  for (final b in inHouse) {
                    final night = b.roomCharges > 0 ? b.roomCharges : 0;
                    await ref.read(hotelControllerProvider.notifier).saveBooking(
                          id: b.id,
                          guestName: b.guestName,
                          guestMobile: b.guestMobile,
                          roomClientId: b.roomClientId,
                          roomNumber: b.roomNumber,
                          bookingStatus: b.bookingStatus,
                          roomCharges: b.roomCharges + (night > 0 ? 0 : 0),
                          restaurantCharges: b.restaurantCharges,
                          otherCharges: b.otherCharges,
                          advancePaid: b.advancePaid,
                          bookingNo: b.bookingNo,
                          folioNotes:
                              '${b.folioNotes}\nNight audit ${DateTime.now().toIso8601String()}'.trim(),
                        );
                  }
                  await upsertOpsRecord(ref, entity: 'approval', row: {
                    'id': 'night_${DateTime.now().millisecondsSinceEpoch}',
                    'name': 'Night audit ${DateTime.now().toLocal()}',
                    'title': 'Night audit',
                    'requestType': 'NIGHT_AUDIT',
                    'status': 'APPROVED',
                    'amount': folioSum.toStringAsFixed(2),
                    'notes': 'In-house ${inHouse.length} · folio ₹${folioSum.toStringAsFixed(0)}',
                  });
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Night audit posted')),
                    );
                  }
                },
                icon: const Icon(Icons.nightlight_round),
                label: const Text('Run night audit'),
              ),
              const SizedBox(height: 16),
              const Text('In-house folios', style: TextStyle(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              for (final b in inHouse)
                ListTile(
                  title: Text('${b.roomNumber} · ${b.guestName}'),
                  subtitle: Text('Folio ₹${b.folioTotal.toStringAsFixed(0)}'),
                ),
            ],
          );
        },
      ),
    );
  }

  Widget _kpi(String label, String value) {
    return Card(
      child: ListTile(
        title: Text(label),
        trailing: Text(value, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 18)),
      ),
    );
  }
}

class PharmacySchedulePage extends StatelessWidget {
  const PharmacySchedulePage({super.key});

  @override
  Widget build(BuildContext context) {
    return const OpsEntityListPage(
      entity: 'lot',
      title: 'Pharmacy schedule',
      subtitle: 'Mark Schedule H / H1 / X on lots (use name prefix SCH-H:).',
      icon: Icons.medical_services_rounded,
      color: AppColors.green,
      fields: [
        'name',
        'productName',
        'batchNo',
        'expiryDate',
        'qty',
        'schedule',
        'rxRequired',
      ],
      statusActions: ['ACTIVE', 'EXPIRED', 'QUARANTINE'],
    );
  }
}

class ModifiersPage extends StatelessWidget {
  const ModifiersPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const OpsEntityListPage(
      entity: 'offer',
      title: 'Modifiers / add-ons',
      subtitle:
          'Set type=modifier (or addon) and amount. Shown when adding products to cart.',
      icon: Icons.extension_rounded,
      fields: ['name', 'code', 'type', 'amount', 'productName'],
      statusActions: ['ACTIVE', 'INACTIVE'],
    );
  }
}

class KitchenDepartmentsPage extends StatelessWidget {
  const KitchenDepartmentsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const OpsEntityListPage(
      entity: 'warehouse',
      title: 'Kitchen departments',
      subtitle:
          'type=kitchen. Match product category name or product code to dept name/code for KOT routing.',
      icon: Icons.soup_kitchen_rounded,
      color: AppColors.orange,
      fields: ['name', 'code', 'type', 'address'],
    );
  }
}

class EnterpriseReportsPage extends ConsumerWidget {
  const EnterpriseReportsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lots = ref.watch(opsListProvider('lot'));
    final transfers = ref.watch(opsListProvider('transfer'));
    final returns = ref.watch(opsListProvider('return'));
    final offers = ref.watch(opsListProvider('offer'));
    final approvals = ref.watch(opsListProvider('approval'));

    List<Map<String, dynamic>> data(AsyncValue<List<Map<String, dynamic>>> a) =>
        a.maybeWhen(data: (v) => v, orElse: () => const []);

    final lotRows = data(lots);
    final expired = lotRows.where((l) {
      final e = DateTime.tryParse('${l['expiryDate'] ?? ''}');
      return e != null && e.isBefore(DateTime.now());
    }).length;
    final near = lotRows.where((l) {
      final e = DateTime.tryParse('${l['expiryDate'] ?? ''}');
      if (e == null) return false;
      final d = e.difference(DateTime.now()).inDays;
      return d >= 0 && d <= 30;
    }).length;
    final pendingAppr = data(approvals)
        .where((a) => (a['status'] ?? '').toString().toUpperCase() == 'PENDING')
        .length;
    final activeOffers = data(offers)
        .where((a) => (a['status'] ?? 'ACTIVE').toString().toUpperCase() == 'ACTIVE')
        .length;

    return Scaffold(
      appBar: AppBar(title: const Text('Enterprise reports')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _tile('Lots expired', '$expired', Icons.warning_amber_rounded, AppColors.danger),
          _tile('Lots expiring ≤30d', '$near', Icons.hourglass_bottom, AppColors.orange),
          _tile('Transfers', '${data(transfers).length}', Icons.swap_horiz, AppColors.primary),
          _tile('Returns', '${data(returns).length}', Icons.assignment_return, AppColors.purple),
          _tile('Pending approvals', '$pendingAppr', Icons.fact_check, const Color(0xFF00897B)),
          _tile('Active offers', '$activeOffers', Icons.local_offer, const Color(0xFFE91E63)),
          const SizedBox(height: 12),
          const Text('Lot ageing', style: TextStyle(fontFamily: AppFonts.family, fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          for (final l in lotRows.take(40))
            ListTile(
              dense: true,
              title: Text('${l['title'] ?? l['name'] ?? l['productName'] ?? ''}'),
              subtitle: Text('Batch ${l['batchNo'] ?? '—'} · Exp ${l['expiryDate'] ?? '—'} · Qty ${l['qty'] ?? '—'}'),
              trailing: Text('${l['status'] ?? ''}'),
            ),
        ],
      ),
    );
  }

  Widget _tile(String title, String value, IconData icon, Color color) {
    return Card(
      child: ListTile(
        leading: Icon(icon, color: color),
        title: Text(title),
        trailing: Text(value, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 18)),
      ),
    );
  }
}

Future<void> chargeBillToRoom(
  WidgetRef ref, {
  required String bookingId,
  required double amount,
  String note = '',
}) async {
  final session = ref.read(authControllerProvider).session;
  if (session == null) throw StateError('Not signed in');
  final list = await HotelLocalStore.loadBookings(session);
  final b = list.where((e) => e.id == bookingId).firstOrNull;
  if (b == null) throw StateError('Booking not found');
  if (b.bookingStatus != 'CHECKED_IN') {
    throw StateError('Guest not checked in');
  }
  await ref.read(hotelControllerProvider.notifier).saveBooking(
        id: b.id,
        guestName: b.guestName,
        guestMobile: b.guestMobile,
        roomClientId: b.roomClientId,
        roomNumber: b.roomNumber,
        bookingStatus: b.bookingStatus,
        roomCharges: b.roomCharges,
        restaurantCharges: b.restaurantCharges + amount,
        otherCharges: b.otherCharges,
        advancePaid: b.advancePaid,
        bookingNo: b.bookingNo,
        folioNotes: '${b.folioNotes}\nPOS charge ₹${amount.toStringAsFixed(2)} $note'.trim(),
      );
}
