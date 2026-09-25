import 'dart:async';
import 'dart:math';

import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/utils/json_parsers.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/mess/data/mess_api.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_dtos.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_meal_token_print_worker.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_payer_mode.dart';

/* Matches Android MessTokenQrHelper payload. */
class MessTokenQrHelper {
  static const prefix = 'POSBILL|v1|';
  static const memberTypeMember = 'member';
  static const memberTypeWalkIn = 'walk_in';

  static String generateTokenCode() {
    final rand = Random.secure();
    final bytes = List<int>.generate(16, (_) => rand.nextInt(256));
    return bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }

  /* Android MessTokenQrHelper.resolveMessType (+ 2nd coupon/token → Dinner). */
  static String resolveMessType({int existingPrintsToday = 0}) {
    if (existingPrintsToday == 1) return 'Dinner';
    final hour = DateTime.now().hour;
    if (hour >= 18) return 'Dinner';
    if (hour >= 7) return 'Lunch';
    return 'Meal';
  }

  static String buildPayload({
    required String tokenCode,
    required String userId,
    required String memberType,
  }) {
    return '$prefix$tokenCode|$userId|$memberType';
  }

  /* Returns [tokenCode, userId, memberType] or null. */
  static List<String>? parsePayload(String raw) {
    if (!raw.startsWith(prefix)) return null;
    final parts = raw.split('|');
    if (parts.length < 5) return null;
    return [parts[2], parts[3], parts[4]];
  }
}

final messMembersProvider = StreamProvider<List<MessMember>>((ref) {
  return ref.watch(appDatabaseProvider).watchMessMembers();
});

final messInstitutePayProvider = FutureProvider<bool>((ref) async {
  return MessPayerMode.isInstitutePay();
});

final messPaymentsProvider =
    StreamProvider.family<List<MessMemberPayment>, String?>((ref, memberId) {
      return ref
          .watch(appDatabaseProvider)
          .watchMessMemberPayments(memberId: memberId);
    });

final todayMessTokensProvider = StreamProvider<List<MessToken>>((ref) {
  return ref.watch(appDatabaseProvider).watchTodayMessTokens();
});

final messCommonQrProvider =
    NotifierProvider<MessCommonQrController, AsyncValue<MessCommonQrDto?>>(
      MessCommonQrController.new,
    );

class MessCommonQrController extends Notifier<AsyncValue<MessCommonQrDto?>> {
  @override
  AsyncValue<MessCommonQrDto?> build() => const AsyncData(null);

  Future<void> load() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      state = AsyncError('Please login first', StackTrace.current);
      return;
    }
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final api = MessApi(ref.read(apiClientProvider));
      return api.fetchCommonQr(userId);
    });
  }

  Future<void> generate() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      state = AsyncError('Please login first', StackTrace.current);
      return;
    }
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final device = await DeviceIdentityService().resolve();
      final api = MessApi(ref.read(apiClientProvider));
      return api.generateCommonQr(
        userId,
        deviceId: device.deviceId,
        deviceName: device.deviceName,
      );
    });
  }

  Future<void> regenerate() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      state = AsyncError('Please login first', StackTrace.current);
      return;
    }
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final device = await DeviceIdentityService().resolve();
      final api = MessApi(ref.read(apiClientProvider));
      return api.regenerateCommonQr(
        userId,
        deviceId: device.deviceId,
        deviceName: device.deviceName,
      );
    });
  }

  Future<void> setStatus(String status) async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      state = AsyncError('Please login first', StackTrace.current);
      return;
    }
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final device = await DeviceIdentityService().resolve();
      final api = MessApi(ref.read(apiClientProvider));
      final ok = await api.setCommonQrStatus(
        userId,
        status: status,
        deviceId: device.deviceId,
        deviceName: device.deviceName,
      );
      if (!ok) {
        throw Exception('Failed to update QR status');
      }
      return api.fetchCommonQr(userId);
    });
  }
}

class MessController extends Notifier<AsyncValue<void>> {
  @override
  AsyncValue<void> build() => const AsyncData(null);

  Future<void> syncMembers() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      state = AsyncError('Please login first', StackTrace.current);
      return;
    }

    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final api = MessApi(ref.read(apiClientProvider));
      final members = await api.fetchMembers(userId);
      await ref
          .read(appDatabaseProvider)
          .replaceMessMembers(
            members
                .where((e) => e.memberId > 0 && e.memberName.trim().isNotEmpty)
                .map(
                  (e) => MessMembersCompanion.insert(
                    memberId: Value(e.memberId),
                    memberName: Value(e.memberName),
                    memberMobileNumber: Value(e.memberMobileNumber),
                    memberAltenetMobileNumber: Value(
                      e.memberAltenetMobileNumber,
                    ),
                    memberAddress: Value(e.memberAddress),
                    registrationNo: Value(e.registrationNo),
                    memberType: Value(e.memberType),
                    rollNo: Value(e.rollNo),
                    college: Value(e.college),
                    studentYear: Value(e.studentYear),
                    company: Value(e.company),
                    memberStatus: Value(e.memberStatus),
                    memberNetworkStatus: Value(e.memberNetworkStatus),
                    memberSyncStatus: const Value('1'),
                  ),
                )
                .toList(),
          );

      /* Upload pending paper coupons, then refresh cloud list. */
      final db = ref.read(appDatabaseProvider);
      for (final coupon in await db.getPendingMessInvoices()) {
        try {
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
          if (ok) await db.markMessInvoiceSynced(coupon.invoiceId);
        } catch (_) {}
      }
      try {
        final cloudCoupons = await api.fetchMessInvoices(userId);
        if (cloudCoupons.isNotEmpty) {
          await db.replaceMessInvoices(
            cloudCoupons
                .map(
                  (e) => MessInvoicesCompanion.insert(
                    invoiceId: e.invoiceId > 0
                        ? Value(e.invoiceId)
                        : const Value.absent(),
                    memberId: Value(e.memberId),
                    memberName: Value(e.memberName),
                    messType: Value(e.messType),
                    messInvoiceDate:
                        parseInvoiceDate(e.messInvoiceDate) ?? DateTime.now(),
                    messInvoiceNetworkStatus:
                        e.messInvoiceNetworkStatus?.trim().isNotEmpty == true
                        ? e.messInvoiceNetworkStatus!
                        : 'mi_${e.invoiceId}',
                    messInvoiceStatus: Value(
                      e.messInvoiceStatus.isEmpty ||
                              e.messInvoiceStatus.toLowerCase() == 'active'
                          ? '1'
                          : e.messInvoiceStatus,
                    ),
                  ),
                )
                .toList(),
          );
        }
      } catch (_) {}

      try {
        final cloudPayments = await api.fetchMemberPayments(userId);
        await db.replaceMessMemberPayments(
          cloudPayments
              .where(
                (e) =>
                    e.memberId.trim().isNotEmpty &&
                    (e.paymentNetworkStatus?.trim().isNotEmpty == true ||
                        e.paymentId > 0),
              )
              .map(
                (e) => MessMemberPaymentsCompanion.insert(
                  memberId: e.memberId,
                  memberName: Value(e.memberName),
                  paymentMessAmount: Value(e.paymentMessAmount),
                  paymentPaidAmount: Value(e.paymentPaidAmount),
                  messTotalDays: Value(e.messTotalDays),
                  paymentDate: e.paymentDate.isNotEmpty
                      ? e.paymentDate
                      : DateFormat('yyyy-MM').format(DateTime.now()),
                  paymentNetworkStatus:
                      e.paymentNetworkStatus?.trim().isNotEmpty == true
                      ? e.paymentNetworkStatus!.trim()
                      : 'pay_${e.paymentId}',
                  paymentStatus: Value(
                    e.paymentStatus.isEmpty ? '1' : e.paymentStatus,
                  ),
                  paymentSyncStatus: const Value('1'),
                ),
              )
              .toList(),
        );
      } catch (_) {}

      try {
        final cloudTokens = await api.fetchMessTokens(userId);
        await db.replaceMessTokens(
          cloudTokens
              .where((e) => e.tokenCode.trim().isNotEmpty)
              .map(
                (e) => MessTokensCompanion.insert(
                  tokenId: e.tokenId > 0
                      ? Value(e.tokenId)
                      : const Value.absent(),
                  tokenCode: e.tokenCode,
                  memberId: Value(e.memberId),
                  memberName: Value(e.memberName),
                  memberMobile: Value(e.memberMobile),
                  memberType: Value(e.memberType),
                  messType: Value(e.messType),
                  tokenAmount: Value(e.tokenAmount),
                  tokenDate: e.tokenDate ?? DateTime.now(),
                  verifiedDate: Value(e.verifiedDate),
                  tokenState: Value(e.tokenState),
                  tokenNetworkStatus: Value(
                    e.tokenNetworkStatus?.trim().isNotEmpty == true
                        ? e.tokenNetworkStatus
                        : 'tok_${e.tokenCode}',
                  ),
                  tokenStatus: Value(
                    e.tokenStatus.isEmpty ? '1' : e.tokenStatus,
                  ),
                  tokenSyncStatus: const Value('1'),
                  verifyNetworkStatus: Value(e.verifyNetworkStatus),
                  verifyStatus: Value(e.verifyStatus),
                ),
              )
              .toList(),
        );
      } catch (_) {}

      /* Shop payer mode cache (WithTable MessPayerMode + mess_shop_setting_get). */
      try {
        final mode = await api.fetchShopPayerMode(userId);
        if (mode != null) {
          await MessPayerMode.setLocal(mode);
        }
      } catch (_) {}

      /* Recover unprinted meal tokens for this device into local print queue. */
      try {
        await recoverPendingMealTokens();
      } catch (_) {}
    });
  }

  /* Pulls `mess_meal_token_pending` into Drift print queue (WithTable recover). */
  Future<int> recoverPendingMealTokens() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return 0;
    final device = await DeviceIdentityService().resolve();
    final api = MessApi(ref.read(apiClientProvider));
    final pending = await api.fetchPendingMealTokens(
      userId: userId,
      deviceId: device.deviceId,
    );
    final db = ref.read(appDatabaseProvider);
    var count = 0;
    for (final token in pending) {
      final id = token.tokenId.trim();
      if (id.isEmpty) continue;
      await db.enqueueMessMealToken(
        serverPublicId: id,
        tokenNumber: token.tokenNumber,
        registrationNo: token.registrationNo.trim().isNotEmpty
            ? token.registrationNo
            : token.memberMobile,
        mealSession: token.mealSession,
        tokenDate: token.date,
        memberName: token.memberName,
        createdAt: token.createdAt,
        printStatus: token.printStatus.trim().isEmpty
            ? 'PRINT_PENDING'
            : token.printStatus,
      );
      count++;
    }
    if (count > 0) {
      unawaited(ref.read(messMealTokenPrintWorkerProvider).kick());
    }
    return count;
  }

  Future<void> deleteLocalMember(int memberId) async {
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      throw StateError(kOnlineRequiredMessage);
    }
    final db = ref.read(appDatabaseProvider);
    await db.deleteLocalMessMember(memberId);
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId != null && userId.isNotEmpty) {
      try {
        final member = await db.getMessMember(memberId);
        if (member != null) {
          final ok = await MessApi(ref.read(apiClientProvider)).insertMessMember(
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
              memberStatus: '2',
              memberNetworkStatus: member.memberNetworkStatus,
            ),
          );
          if (ok) {
            await db.markMessMemberSynced(memberId);
          } else if (AppPlatform.requiresNetwork) {
            throw StateError(kWebApiSaveFailedMessage);
          }
        }
      } catch (e) {
        if (AppPlatform.requiresNetwork) {
          if (e is StateError) rethrow;
          throw StateError(kWebApiSaveFailedMessage);
        }
      }
    } else if (AppPlatform.requiresNetwork) {
      throw StateError('Please login to save on Web POS.');
    }
  }

  Future<void> setShopPayerMode(bool institutePay) async {
    final mode = institutePay
        ? MessPayerMode.modeInstitute
        : MessPayerMode.modeUser;
    await MessPayerMode.setLocal(mode);
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    if (!await ensureOnline()) return;
    try {
      await MessApi(
        ref.read(apiClientProvider),
      ).saveShopPayerMode(userId: userId, payerMode: mode);
    } catch (_) {}
  }

  Future<int> addLocalMember({
    required String name,
    String? mobile,
    String? altMobile,
    String? address,
    String? registrationNo,
    String memberType = 'student',
    String? rollNo,
    String? college,
    String? studentYear,
    String? company,
  }) async {
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      throw StateError(kOnlineRequiredMessage);
    }
    final db = ref.read(appDatabaseProvider);
    final id = await db.upsertLocalMessMember(
      memberName: name,
      mobile: mobile,
      altMobile: altMobile,
      address: address,
      registrationNo: registrationNo,
      memberType: memberType,
      rollNo: rollNo,
      college: college,
      studentYear: studentYear,
      company: company,
    );
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId != null && userId.isNotEmpty) {
      try {
        final member = await db.getMessMember(id);
        if (member != null) {
          final ok = await MessApi(ref.read(apiClientProvider))
              .insertMessMember(
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
            await db.markMessMemberSynced(id);
          } else if (AppPlatform.requiresNetwork) {
            throw StateError(kWebApiSaveFailedMessage);
          }
        }
      } catch (e) {
        if (AppPlatform.requiresNetwork) {
          if (e is StateError) rethrow;
          throw StateError(kWebApiSaveFailedMessage);
        }
        /* Keep local pending row for later full sync. */
      }
    } else if (AppPlatform.requiresNetwork) {
      throw StateError('Please login to save on Web POS.');
    }
    return id;
  }

  Future<void> updateLocalMember({
    required int memberId,
    required String name,
    String? mobile,
    String? altMobile,
    String? address,
    String? registrationNo,
    String memberType = 'student',
    String? rollNo,
    String? college,
    String? studentYear,
    String? company,
  }) async {
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      throw StateError(kOnlineRequiredMessage);
    }
    final db = ref.read(appDatabaseProvider);
    await db.updateLocalMessMember(
      memberId: memberId,
      memberName: name,
      mobile: mobile,
      altMobile: altMobile,
      address: address,
      registrationNo: registrationNo,
      memberType: memberType,
      rollNo: rollNo,
      college: college,
      studentYear: studentYear,
      company: company,
    );
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId != null && userId.isNotEmpty) {
      try {
        final member = await db.getMessMember(memberId);
        if (member != null) {
          final ok = await MessApi(ref.read(apiClientProvider))
              .insertMessMember(
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
            await db.markMessMemberSynced(memberId);
          } else if (AppPlatform.requiresNetwork) {
            throw StateError(kWebApiSaveFailedMessage);
          }
        }
      } catch (e) {
        if (AppPlatform.requiresNetwork) {
          if (e is StateError) rethrow;
          throw StateError(kWebApiSaveFailedMessage);
        }
      }
    } else if (AppPlatform.requiresNetwork) {
      throw StateError('Please login to save on Web POS.');
    }
  }

  /* Prepare token without DB write — Android saves only after print success. */
  ({String tokenCode, String payload, String messType}) prepareMemberToken(
    MessMember member, {
    String? messType,
  }) {
    final userId = ref.read(authControllerProvider).session?.userId ?? '0';
    final code = MessTokenQrHelper.generateTokenCode();
    final type =
        messType ?? MessTokenQrHelper.resolveMessType();
    final payload = MessTokenQrHelper.buildPayload(
      tokenCode: code,
      userId: userId,
      memberType: MessTokenQrHelper.memberTypeMember,
    );
    return (tokenCode: code, payload: payload, messType: type);
  }

  /* Persist token + twin mess invoice after successful print (Android twin). */
  Future<MessToken> commitMemberToken({
    required MessMember member,
    required String tokenCode,
    required String messType,
  }) async {
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      throw StateError(kOnlineRequiredMessage);
    }
    final userId = ref.read(authControllerProvider).session?.userId ?? '0';
    final db = ref.read(appDatabaseProvider);
    final token = await db.issueMessToken(
      tokenCode: tokenCode,
      memberId: '${member.memberId}',
      memberName: member.memberName,
      memberMobile: member.memberMobileNumber,
      memberType: MessTokenQrHelper.memberTypeMember,
      messType: messType,
    );
    /* Android also saves mess_invoice for daily One/Two Time accounting. */
    final invoiceId = await db.issueMessCoupon(
      memberId: '${member.memberId}',
      memberName: member.memberName,
      messType: messType,
    );
    if (userId != '0' && userId.isNotEmpty) {
      try {
        final ok = await MessApi(ref.read(apiClientProvider)).insertMessToken(
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
        } else if (AppPlatform.requiresNetwork) {
          throw StateError(kWebApiSaveFailedMessage);
        }
      } catch (e) {
        if (AppPlatform.requiresNetwork) {
          if (e is StateError) rethrow;
          throw StateError(kWebApiSaveFailedMessage);
        }
      }
      try {
        final row = await db.getMessInvoiceById(invoiceId);
        if (row != null) {
          final ok = await MessApi(ref.read(apiClientProvider)).insertMessInvoice(
            userId: userId,
            memberName: row.memberName,
            messType: row.messType,
            messInvoiceDate: DateFormat(
              'yyyy-MM-dd HH:mm:ss',
            ).format(row.messInvoiceDate),
            messInvoiceNetworkStatus: row.messInvoiceNetworkStatus,
            messInvoiceStatus: '0',
          );
          if (ok) await db.markMessInvoiceSynced(invoiceId);
        }
      } catch (_) {}
    } else if (AppPlatform.requiresNetwork) {
      throw StateError('Please login to save on Web POS.');
    }
    return token;
  }

  Future<({MessToken token, String payload})> issueMemberToken(
    MessMember member, {
    String messType = 'Lunch',
  }) async {
    final prep = prepareMemberToken(member, messType: messType);
    final token = await commitMemberToken(
      member: member,
      tokenCode: prep.tokenCode,
      messType: prep.messType,
    );
    return (token: token, payload: prep.payload);
  }

  Future<({MessToken token, String payload})> issueWalkInToken({
    String name = 'Walk-in',
    String? mobile,
    String messType = 'Lunch',
    double amount = 0,
  }) async {
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      throw StateError(kOnlineRequiredMessage);
    }
    final userId = ref.read(authControllerProvider).session?.userId ?? '0';
    final code = MessTokenQrHelper.generateTokenCode();
    final db = ref.read(appDatabaseProvider);
    final token = await db.issueMessToken(
      tokenCode: code,
      memberName: name.trim().isEmpty ? 'Walk-in' : name.trim(),
      memberMobile: mobile?.trim().isEmpty == true ? null : mobile?.trim(),
      memberType: MessTokenQrHelper.memberTypeWalkIn,
      messType: messType,
      tokenAmount: amount,
    );
    if (userId != '0' && userId.isNotEmpty) {
      try {
        final ok = await MessApi(ref.read(apiClientProvider)).insertMessToken(
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
        } else if (AppPlatform.requiresNetwork) {
          throw StateError(kWebApiSaveFailedMessage);
        }
      } catch (e) {
        if (AppPlatform.requiresNetwork) {
          if (e is StateError) rethrow;
          throw StateError(kWebApiSaveFailedMessage);
        }
      }
    } else if (AppPlatform.requiresNetwork) {
      throw StateError('Please login to save on Web POS.');
    }
    final payload = MessTokenQrHelper.buildPayload(
      tokenCode: code,
      userId: userId,
      memberType: MessTokenQrHelper.memberTypeWalkIn,
    );
    return (token: token, payload: payload);
  }

  Future<MessToken?> verifyRaw(String rawOrCode) async {
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      throw StateError(kOnlineRequiredMessage);
    }
    final parsed = MessTokenQrHelper.parsePayload(rawOrCode.trim());
    final code = parsed?.first ?? rawOrCode.trim();
    if (code.isEmpty) return null;
    final db = ref.read(appDatabaseProvider);
    final userId = ref.read(authControllerProvider).session?.userId;

    /* Web: verify on server first so status is authoritative. */
    if (AppPlatform.requiresNetwork) {
      if (userId == null || userId.isEmpty) {
        throw StateError('Please login to verify tokens on Web POS.');
      }
      final ok = await MessApi(
        ref.read(apiClientProvider),
      ).verifyMessToken(userId: userId, tokenCode: code);
      if (!ok) return null;
    }

    final token = await db.verifyMessToken(code);
    if (token != null &&
        userId != null &&
        userId.isNotEmpty &&
        !AppPlatform.requiresNetwork) {
      try {
        final ok = await MessApi(
          ref.read(apiClientProvider),
        ).verifyMessToken(userId: userId, tokenCode: token.tokenCode);
        if (ok) await db.markMessTokenVerifySynced(token.tokenId);
      } catch (_) {}
    }
    return token;
  }
}

final messControllerProvider =
    NotifierProvider<MessController, AsyncValue<void>>(MessController.new);
