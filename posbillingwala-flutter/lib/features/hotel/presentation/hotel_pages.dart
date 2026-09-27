import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/enterprise/data/enterprise_api.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class HotelRoom {
  const HotelRoom({
    required this.id,
    required this.roomNumber,
    this.floor = '',
    this.roomTypeName = '',
    this.roomStatus = 'AVAILABLE',
    this.notes = '',
  });

  final String id;
  final String roomNumber;
  final String floor;
  final String roomTypeName;
  final String roomStatus;
  final String notes;

  Color get statusColor => switch (roomStatus.toUpperCase()) {
    'OCCUPIED' || 'CHECKED_IN' => AppColors.tableRunning,
    'RESERVED' => AppColors.tableReserved,
    'CLEANING' => AppColors.tableBillRequested,
    'BILLING' => AppColors.tablePayment,
    _ => AppColors.tableAvailable,
  };

  Map<String, dynamic> toJson() => {
    'id': id,
    'clientId': id,
    'roomNumber': roomNumber,
    'floor': floor,
    'roomTypeName': roomTypeName,
    'roomStatus': roomStatus,
    'notes': notes,
  };

  factory HotelRoom.fromJson(Map<String, dynamic> json) => HotelRoom(
    id: '${json['id'] ?? json['clientId'] ?? ''}',
    roomNumber: '${json['roomNumber'] ?? ''}',
    floor: '${json['floor'] ?? ''}',
    roomTypeName: '${json['roomTypeName'] ?? ''}',
    roomStatus: '${json['roomStatus'] ?? 'AVAILABLE'}',
    notes: '${json['notes'] ?? ''}',
  );
}

class HotelBooking {
  const HotelBooking({
    required this.id,
    required this.bookingNo,
    required this.guestName,
    this.guestMobile = '',
    this.roomClientId = '',
    this.roomNumber = '',
    this.bookingStatus = 'RESERVED',
    this.advancePaid = 0,
    this.roomCharges = 0,
    this.restaurantCharges = 0,
    this.otherCharges = 0,
    this.folioNotes = '',
  });

  final String id;
  final String bookingNo;
  final String guestName;
  final String guestMobile;
  final String roomClientId;
  final String roomNumber;
  final String bookingStatus;
  final double advancePaid;
  final double roomCharges;
  final double restaurantCharges;
  final double otherCharges;
  final String folioNotes;

  double get folioTotal =>
      roomCharges + restaurantCharges + otherCharges - advancePaid;

  Map<String, dynamic> toJson() => {
    'id': id,
    'clientId': id,
    'bookingNo': bookingNo,
    'guestName': guestName,
    'guestMobile': guestMobile,
    'roomClientId': roomClientId,
    'roomNumber': roomNumber,
    'bookingStatus': bookingStatus,
    'advancePaid': advancePaid,
    'roomCharges': roomCharges,
    'restaurantCharges': restaurantCharges,
    'otherCharges': otherCharges,
    'extraCharges': restaurantCharges + otherCharges,
    'folioNotes': folioNotes,
    'notes': folioNotes,
  };

  factory HotelBooking.fromJson(Map<String, dynamic> json) {
    double n(Object? v) =>
        v is num ? v.toDouble() : double.tryParse('$v') ?? 0;
    return HotelBooking(
      id: '${json['id'] ?? json['clientId'] ?? ''}',
      bookingNo: '${json['bookingNo'] ?? ''}',
      guestName: '${json['guestName'] ?? ''}',
      guestMobile: '${json['guestMobile'] ?? ''}',
      roomClientId: '${json['roomClientId'] ?? ''}',
      roomNumber: '${json['roomNumber'] ?? ''}',
      bookingStatus: '${json['bookingStatus'] ?? 'RESERVED'}',
      advancePaid: n(json['advancePaid']),
      roomCharges: n(json['roomCharges']),
      restaurantCharges: n(json['restaurantCharges']),
      otherCharges: n(json['otherCharges']),
      folioNotes: '${json['folioNotes'] ?? ''}',
    );
  }

  HotelBooking copyWith({
    String? bookingStatus,
    double? advancePaid,
    double? roomCharges,
    double? restaurantCharges,
    double? otherCharges,
    String? folioNotes,
  }) {
    return HotelBooking(
      id: id,
      bookingNo: bookingNo,
      guestName: guestName,
      guestMobile: guestMobile,
      roomClientId: roomClientId,
      roomNumber: roomNumber,
      bookingStatus: bookingStatus ?? this.bookingStatus,
      advancePaid: advancePaid ?? this.advancePaid,
      roomCharges: roomCharges ?? this.roomCharges,
      restaurantCharges: restaurantCharges ?? this.restaurantCharges,
      otherCharges: otherCharges ?? this.otherCharges,
      folioNotes: folioNotes ?? this.folioNotes,
    );
  }
}

abstract final class HotelLocalStore {
  static String _roomsKey(UserSession s) =>
      'hotel_rooms_${BranchScope.effectiveOrganizationId(s)}_${BranchScope.effectiveBranchId(s)}';
  static String _bookingsKey(UserSession s) =>
      'hotel_bookings_${BranchScope.effectiveOrganizationId(s)}_${BranchScope.effectiveBranchId(s)}';

  static Future<List<HotelRoom>> loadRooms(UserSession session) async {
    final rows = await CloudScreenCache.loadMapList(_roomsKey(session));
    return rows.map(HotelRoom.fromJson).toList()
      ..sort((a, b) => a.roomNumber.compareTo(b.roomNumber));
  }

  static Future<void> saveRooms(UserSession session, List<HotelRoom> list) =>
      CloudScreenCache.saveJson(
        _roomsKey(session),
        list.map((e) => e.toJson()).toList(),
      );

  static Future<HotelRoom> upsertRoom(UserSession session, HotelRoom room) async {
    final list = await loadRooms(session);
    final i = list.indexWhere((e) => e.id == room.id);
    if (i >= 0) {
      list[i] = room;
    } else {
      list.add(room);
    }
    await saveRooms(session, list);
    return room;
  }

  static Future<List<HotelBooking>> loadBookings(UserSession session) async {
    final rows = await CloudScreenCache.loadMapList(_bookingsKey(session));
    return rows.map(HotelBooking.fromJson).toList();
  }

  static Future<void> saveBookings(
    UserSession session,
    List<HotelBooking> list,
  ) => CloudScreenCache.saveJson(
    _bookingsKey(session),
    list.map((e) => e.toJson()).toList(),
  );

  static Future<HotelBooking> upsertBooking(
    UserSession session,
    HotelBooking booking,
  ) async {
    final list = await loadBookings(session);
    final i = list.indexWhere((e) => e.id == booking.id);
    if (i >= 0) {
      list[i] = booking;
    } else {
      list.add(booking);
    }
    await saveBookings(session, list);
    return booking;
  }
}

final hotelRoomsProvider = FutureProvider<List<HotelRoom>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];
  final local = await HotelLocalStore.loadRooms(session);
  if (await isDeviceOnline()) {
    try {
      final remote = await ref
          .read(enterpriseApiProvider)
          .fetchHotelRooms(session.licenceUserId);
      if (remote.isNotEmpty) {
        final mapped = remote.map(HotelRoom.fromJson).toList();
        await HotelLocalStore.saveRooms(session, mapped);
        return mapped;
      }
    } catch (_) {}
  }
  return local;
});

final hotelBookingsProvider = FutureProvider<List<HotelBooking>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];
  final local = await HotelLocalStore.loadBookings(session);
  if (await isDeviceOnline()) {
    try {
      final remote = await ref
          .read(enterpriseApiProvider)
          .fetchHotelBookings(session.licenceUserId);
      if (remote.isNotEmpty) {
        final mapped = remote.map(HotelBooking.fromJson).toList();
        await HotelLocalStore.saveBookings(session, mapped);
        return mapped;
      }
    } catch (_) {}
  }
  return local;
});

class HotelController extends Notifier<void> {
  @override
  void build() {}

  Future<HotelRoom> saveRoom({
    String? id,
    required String roomNumber,
    String floor = '',
    String roomTypeName = '',
    String roomStatus = 'AVAILABLE',
    String notes = '',
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) throw StateError('Not signed in');
    final room = HotelRoom(
      id: (id == null || id.isEmpty)
          ? 'room_${DateTime.now().millisecondsSinceEpoch}'
          : id,
      roomNumber: roomNumber.trim(),
      floor: floor.trim(),
      roomTypeName: roomTypeName.trim(),
      roomStatus: roomStatus,
      notes: notes.trim(),
    );
    final saved = await HotelLocalStore.upsertRoom(session, room);
    ref.invalidate(hotelRoomsProvider);
    if (await isDeviceOnline()) {
      try {
        await ref.read(enterpriseApiProvider).saveHotelRoom(
              userId: session.licenceUserId,
              room: saved.toJson(),
            );
      } catch (_) {}
    }
    return saved;
  }

  Future<HotelBooking> saveBooking({
    String? id,
    required String guestName,
    String guestMobile = '',
    String roomClientId = '',
    String roomNumber = '',
    String bookingStatus = 'RESERVED',
    double advancePaid = 0,
    double roomCharges = 0,
    double restaurantCharges = 0,
    double otherCharges = 0,
    String folioNotes = '',
    String? bookingNo,
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) throw StateError('Not signed in');
    final existingId = id;
    String resolvedNo = bookingNo ?? '';
    if (existingId != null && existingId.isNotEmpty) {
      final list = await HotelLocalStore.loadBookings(session);
      final prev = list.where((e) => e.id == existingId).firstOrNull;
      if (prev != null) {
        resolvedNo = prev.bookingNo;
        if (restaurantCharges == 0 && prev.restaurantCharges > 0) {
          restaurantCharges = prev.restaurantCharges;
        }
        if (otherCharges == 0 && prev.otherCharges > 0) {
          otherCharges = prev.otherCharges;
        }
        if (roomCharges == 0 && prev.roomCharges > 0) {
          roomCharges = prev.roomCharges;
        }
        if (advancePaid == 0 && prev.advancePaid > 0) {
          advancePaid = prev.advancePaid;
        }
        if (folioNotes.isEmpty && prev.folioNotes.isNotEmpty) {
          folioNotes = prev.folioNotes;
        }
      }
    }
    if (resolvedNo.isEmpty) {
      resolvedNo = 'BK-${DateTime.now().millisecondsSinceEpoch % 1000000}';
    }
    final booking = HotelBooking(
      id: (existingId == null || existingId.isEmpty)
          ? 'bk_${DateTime.now().millisecondsSinceEpoch}'
          : existingId,
      bookingNo: resolvedNo,
      guestName: guestName.trim(),
      guestMobile: guestMobile.trim(),
      roomClientId: roomClientId,
      roomNumber: roomNumber,
      bookingStatus: bookingStatus,
      advancePaid: advancePaid,
      roomCharges: roomCharges,
      restaurantCharges: restaurantCharges,
      otherCharges: otherCharges,
      folioNotes: folioNotes,
    );
    final saved = await HotelLocalStore.upsertBooking(session, booking);
    if (roomClientId.isNotEmpty) {
      final rooms = await HotelLocalStore.loadRooms(session);
      for (final r in rooms) {
        if (r.id == roomClientId) {
          final status = switch (bookingStatus) {
            'CHECKED_IN' => 'OCCUPIED',
            'CHECKED_OUT' || 'CANCELLED' => 'AVAILABLE',
            _ => 'RESERVED',
          };
          await HotelLocalStore.upsertRoom(
            session,
            HotelRoom(
              id: r.id,
              roomNumber: r.roomNumber,
              floor: r.floor,
              roomTypeName: r.roomTypeName,
              roomStatus: status,
              notes: r.notes,
            ),
          );
          break;
        }
      }
    }
    ref.invalidate(hotelBookingsProvider);
    ref.invalidate(hotelRoomsProvider);
    if (await isDeviceOnline()) {
      try {
        await ref.read(enterpriseApiProvider).saveHotelBooking(
              userId: session.licenceUserId,
              booking: saved.toJson(),
            );
      } catch (_) {}
    }
    return saved;
  }
}

final hotelControllerProvider = NotifierProvider<HotelController, void>(
  HotelController.new,
);

class HotelRoomsPage extends ConsumerWidget {
  const HotelRoomsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(hotelRoomsProvider);
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Rooms'),
        actions: [
          IconButton(
            onPressed: () => _addRoom(context, ref),
            icon: const Icon(Icons.add_rounded),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/hotel/bookings'),
        icon: const Icon(Icons.event_available_rounded),
        label: const Text('Bookings'),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (rooms) {
          if (rooms.isEmpty) {
            return Center(
              child: TextButton(
                onPressed: () => _addRoom(context, ref),
                child: const Text('Add first room'),
              ),
            );
          }
          return ResponsiveScrollShell(
            child: GridView.builder(
              padding: EdgeInsets.all(
                AppBreakpoints.pagePaddingFor(context.widthClass),
              ),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: AppBreakpoints.tableColumnsFor(
                  context.widthClass,
                ),
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: 1.2,
              ),
              itemCount: rooms.length,
              itemBuilder: (context, i) {
                final room = rooms[i];
                return Material(
                  color: room.statusColor,
                  borderRadius: BorderRadius.circular(16),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () => _quickBook(context, ref, room),
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            room.roomNumber,
                            style: const TextStyle(
                              fontFamily: AppFonts.family,
                              color: Colors.white,
                              fontWeight: FontWeight.w800,
                              fontSize: 20,
                            ),
                          ),
                          const Spacer(),
                          Text(
                            room.roomStatus,
                            style: TextStyle(
                              fontFamily: AppFonts.family,
                              color: Colors.white.withValues(alpha: .9),
                              fontWeight: FontWeight.w600,
                              fontSize: 12,
                            ),
                          ),
                          if (room.roomTypeName.isNotEmpty)
                            Text(
                              room.roomTypeName,
                              style: TextStyle(
                                fontFamily: AppFonts.family,
                                color: Colors.white.withValues(alpha: .75),
                                fontSize: 11,
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }

  Future<void> _addRoom(BuildContext context, WidgetRef ref) async {
    final number = TextEditingController();
    final type = TextEditingController();
    final floor = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Add room'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: number,
              decoration: const InputDecoration(labelText: 'Room number *'),
            ),
            TextField(
              controller: type,
              decoration: const InputDecoration(labelText: 'Room type'),
            ),
            TextField(
              controller: floor,
              decoration: const InputDecoration(labelText: 'Floor'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Save'),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    try {
      await ref.read(hotelControllerProvider.notifier).saveRoom(
            roomNumber: number.text,
            roomTypeName: type.text,
            floor: floor.text,
          );
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  Future<void> _quickBook(
    BuildContext context,
    WidgetRef ref,
    HotelRoom room,
  ) async {
    final guest = TextEditingController();
    final mobile = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Book ${room.roomNumber}'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: guest,
              decoration: const InputDecoration(labelText: 'Guest name *'),
            ),
            TextField(
              controller: mobile,
              decoration: const InputDecoration(labelText: 'Mobile'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Check-in'),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    try {
      await ref.read(hotelControllerProvider.notifier).saveBooking(
            guestName: guest.text,
            guestMobile: mobile.text,
            roomClientId: room.id,
            roomNumber: room.roomNumber,
            bookingStatus: 'CHECKED_IN',
          );
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }
}

class HotelBookingsPage extends ConsumerWidget {
  const HotelBookingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(hotelBookingsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Bookings')),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (list) {
          if (list.isEmpty) {
            return const Center(child: Text('No bookings yet'));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: list.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (context, i) {
              final b = list[i];
              return ListTile(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: AppColors.border),
                ),
                title: Text(
                  '${b.bookingNo} · ${b.guestName}',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                subtitle: Text(
                  'Room ${b.roomNumber.isEmpty ? '—' : b.roomNumber} · ${b.bookingStatus}\n'
                  'Folio ₹${b.folioTotal.toStringAsFixed(0)} '
                  '(room ${b.roomCharges.toStringAsFixed(0)} + F&B ${b.restaurantCharges.toStringAsFixed(0)} '
                  '+ other ${b.otherCharges.toStringAsFixed(0)} − adv ${b.advancePaid.toStringAsFixed(0)})',
                ),
                isThreeLine: true,
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (b.bookingStatus == 'CHECKED_IN')
                      IconButton(
                        tooltip: 'Add folio charge',
                        icon: const Icon(Icons.receipt_long_rounded),
                        onPressed: () async {
                          final amt = TextEditingController();
                          var kind = 'restaurant';
                          final ok = await showDialog<bool>(
                            context: context,
                            builder: (ctx) => StatefulBuilder(
                              builder: (ctx, setLocal) => AlertDialog(
                              title: const Text('Add folio charge'),
                              content: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  DropdownButtonFormField<String>(
                                      initialValue: kind,
                                      items: const [
                                        DropdownMenuItem(
                                          value: 'restaurant',
                                          child: Text('Restaurant / F&B'),
                                        ),
                                        DropdownMenuItem(
                                          value: 'room',
                                          child: Text('Room'),
                                        ),
                                        DropdownMenuItem(
                                          value: 'other',
                                          child: Text('Other'),
                                        ),
                                        DropdownMenuItem(
                                          value: 'advance',
                                          child: Text('Advance paid'),
                                        ),
                                      ],
                                      onChanged: (n) => setLocal(() {
                                        kind = n ?? 'restaurant';
                                      }),
                                    ),
                                  TextField(
                                    controller: amt,
                                    keyboardType:
                                        const TextInputType.numberWithOptions(
                                      decimal: true,
                                    ),
                                    decoration: const InputDecoration(
                                      labelText: 'Amount',
                                    ),
                                  ),
                                ],
                              ),
                              actions: [
                                TextButton(
                                  onPressed: () => Navigator.pop(ctx, false),
                                  child: const Text('Cancel'),
                                ),
                                FilledButton(
                                  onPressed: () => Navigator.pop(ctx, true),
                                  child: const Text('Add'),
                                ),
                              ],
                            ),
                            ),
                          );
                          if (ok != true) return;
                          final value = double.tryParse(amt.text) ?? 0;
                          if (value <= 0) return;
                          var room = b.roomCharges;
                          var rest = b.restaurantCharges;
                          var other = b.otherCharges;
                          var adv = b.advancePaid;
                          switch (kind) {
                            case 'room':
                              room += value;
                            case 'other':
                              other += value;
                            case 'advance':
                              adv += value;
                            default:
                              rest += value;
                          }
                          await ref
                              .read(hotelControllerProvider.notifier)
                              .saveBooking(
                                id: b.id,
                                guestName: b.guestName,
                                guestMobile: b.guestMobile,
                                roomClientId: b.roomClientId,
                                roomNumber: b.roomNumber,
                                bookingStatus: b.bookingStatus,
                                roomCharges: room,
                                restaurantCharges: rest,
                                otherCharges: other,
                                advancePaid: adv,
                                bookingNo: b.bookingNo,
                              );
                        },
                      ),
                    if (b.bookingStatus == 'CHECKED_IN')
                      TextButton(
                        onPressed: () async {
                          await ref
                              .read(hotelControllerProvider.notifier)
                              .saveBooking(
                                id: b.id,
                                guestName: b.guestName,
                                guestMobile: b.guestMobile,
                                roomClientId: b.roomClientId,
                                roomNumber: b.roomNumber,
                                bookingStatus: 'CHECKED_OUT',
                                roomCharges: b.roomCharges,
                                restaurantCharges: b.restaurantCharges,
                                otherCharges: b.otherCharges,
                                advancePaid: b.advancePaid,
                                bookingNo: b.bookingNo,
                              );
                        },
                        child: const Text('Checkout'),
                      ),
                  ],
                ),
              );
            },
          );
        },
      ),
    );
  }
}

class HotelRoomTypesPage extends StatelessWidget {
  const HotelRoomTypesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Room Types')),
      body: const Center(
        child: Text('Add room types from Rooms → set type on each room.'),
      ),
    );
  }
}

class HotelRoomMasterPage extends StatelessWidget {
  const HotelRoomMasterPage({super.key});

  @override
  Widget build(BuildContext context) => const HotelRoomsPage();
}
