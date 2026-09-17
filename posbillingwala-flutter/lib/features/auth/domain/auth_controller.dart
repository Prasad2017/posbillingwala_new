import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/data/auth_api.dart';
import 'package:pos_billingwala_v2/features/auth/data/auth_repository.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/data/session_store.dart';
import 'package:pos_billingwala_v2/features/auth/domain/license_validator.dart';
import 'package:pos_billingwala_v2/features/auth/domain/login_response.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/features/notifications/domain/fcm_service.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_api.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_store.dart';

enum AuthStatus {
  unknown,
  unauthenticated,
  needsMpin,
  needsStaffLogin,
  authenticated,
}

class AuthState {
  const AuthState({
    required this.status,
    this.session,
    this.errorMessage,
    this.busy = false,
  });

  final AuthStatus status;
  final UserSession? session;
  final String? errorMessage;
  final bool busy;

  AuthState copyWith({
    AuthStatus? status,
    UserSession? session,
    String? errorMessage,
    bool? busy,
    bool clearError = false,
    bool clearSession = false,
  }) {
    return AuthState(
      status: status ?? this.status,
      session: clearSession ? null : (session ?? this.session),
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      busy: busy ?? this.busy,
    );
  }
}

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient());

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    api: AuthApi(ref.watch(apiClientProvider)),
    sessionStore: SessionStore(),
    deviceIdentityService: DeviceIdentityService(),
  );
});

class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() => const AuthState(status: AuthStatus.unknown);

  AuthRepository get repo => ref.read(authRepositoryProvider);

  Future<void> bindBranchScope(UserSession session) async {
    final device = await DeviceIdentityService().resolve();
    await ref.read(appDatabaseProvider).applyLicenceScope(
          session: session,
          deviceId: device.deviceId,
        );
  }

  Future<void> bootstrap() async {
    state = state.copyWith(status: AuthStatus.unknown, clearError: true);
    final session = await repo.readStoredSession();
    if (session == null) {
      state = const AuthState(status: AuthStatus.unauthenticated);
      return;
    }
    /* Bind Drift branch filters before MPIN so cold-start queries stay scoped. */
    await bindBranchScope(session);
    if (session.authToken != null && session.authToken!.isNotEmpty) {
      ref.read(apiClientProvider).setAuthToken(session.authToken);
    }
    /* Owner unlock (PB-PIN) on every cold start — not staff login. */
    state = AuthState(status: AuthStatus.needsMpin, session: session);
  }

  Future<DeviceConflictAction> Function(String message)? conflictHandler;

  void setDeviceConflictHandler(
    Future<DeviceConflictAction> Function(String message)? handler,
  ) {
    conflictHandler = handler;
  }

  Future<bool> loginWithLicence(String licenceKey) async {
    state = state.copyWith(busy: true, clearError: true);
    try {
      if (!await ensureOnline()) {
        state = state.copyWith(
          busy: false,
          errorMessage: kOnlineRequiredMessage,
          status: AuthStatus.unauthenticated,
        );
        return false;
      }
      AppLogger.info('loginWithLicence start keyLen=${licenceKey.trim().length}');
      final session = await repo.loginWithLicence(
        licenceKey: licenceKey,
        onDeviceConflict: conflictHandler,
      );
      ref.read(apiClientProvider).setAuthToken(session.authToken);
      await bindBranchScope(session);
      /* Licence bind done — next screen is owner PB-PIN login. */
      state = AuthState(status: AuthStatus.needsMpin, session: session);
      FcmService(apiClient: ref.read(apiClientProvider))
          .registerForUser(session.userId);
      return true;
    } on AuthException catch (e) {
      AppLogger.error('loginWithLicence AuthException', e);
      state = state.copyWith(
        busy: false,
        errorMessage: e.message,
        status: AuthStatus.unauthenticated,
      );
      return false;
    } catch (e) {
      AppLogger.error('loginWithLicence failed', e);
      state = state.copyWith(
        busy: false,
        errorMessage: 'Something went wrong.\n$e',
        status: AuthStatus.unauthenticated,
      );
      return false;
    }
  }

  Future<bool> loginWithMpin(String mpin) async {
    state = state.copyWith(busy: true, clearError: true);
    try {
      if (!await ensureOnline()) {
        state = state.copyWith(
          busy: false,
          errorMessage: kOnlineRequiredMessage,
          status: AuthStatus.needsMpin,
        );
        return false;
      }
      final session = await repo.loginWithMpin(
        mpin: mpin,
        onDeviceConflict: conflictHandler,
      );
      await StaffStore().clear();
      ref.read(apiClientProvider).setStaffId(null);
      ref.read(apiClientProvider).setAuthToken(session.authToken);
      await bindBranchScope(session);
      state = AuthState(
        status: AuthStatus.authenticated,
        session: session,
      );
      FcmService(apiClient: ref.read(apiClientProvider))
          .registerForUser(session.userId);
      return true;
    } on AuthException catch (e) {
      state = state.copyWith(
        busy: false,
        errorMessage: e.message,
        status: AuthStatus.needsMpin,
      );
      return false;
    } catch (e) {
      state = state.copyWith(
        busy: false,
        errorMessage: 'Something went wrong.\n$e',
        status: AuthStatus.needsMpin,
      );
      return false;
    }
  }

  /* Soft logout: keep licence on device and require owner PB-PIN unlock. */
  Future<void> logout() async {
    final session = state.session;
    final userId = session?.userId;
    if (userId != null && userId.isNotEmpty) {
      await FcmService(apiClient: ref.read(apiClientProvider))
          .clearForUser(userId);
    }
    await StaffStore().clear();
    ref.read(apiClientProvider).setStaffId(null);

    await repo.lockSession();
    ref.read(apiClientProvider).setAuthToken(null);

    final locked = session ?? await repo.readStoredSession();
    if (locked == null) {
      state = const AuthState(status: AuthStatus.unauthenticated);
      return;
    }
    state = AuthState(status: AuthStatus.needsMpin, session: locked);
  }

  Future<bool> loginWithStaff({
    required String mobileNumber,
    required String pin,
  }) async {
    final existing = state.session;
    state = state.copyWith(busy: true, clearError: true);
    try {
      if (!await ensureOnline()) {
        state = state.copyWith(
          busy: false,
          errorMessage: kOnlineRequiredMessage,
          status: existing == null
              ? AuthStatus.unauthenticated
              : AuthStatus.needsMpin,
        );
        return false;
      }
      final device = await DeviceIdentityService().resolve();
      final result = await StaffApi(ref.read(apiClientProvider)).login(
        userId: existing?.licenceUserId,
        mobileNumber: mobileNumber,
        pin: pin,
        deviceId: device.deviceId,
        deviceName: device.deviceName,
      );

      var session = existing;
      if (result.coldLogin || session == null) {
        final login = LoginResponse.fromJson(result.raw);
        if (!login.isSuccess ||
            (login.licenceId?.isEmpty ?? true) ||
            (login.licenceKey?.isEmpty ?? true)) {
          throw Exception(login.message ?? 'Staff login failed');
        }
        session = UserSession.fromLogin(login);
        await SessionStore().saveSession(session);
        await LicenseValidator.saveFromLogin(
          licensePayload: login.licensePayload,
          licenseSignature: login.licenseSignature,
          issuedAt: login.issuedAt,
          offlineGraceUntil: login.offlineGraceUntil,
          trialConsumed: login.trialConsumed,
          organizationId: login.organizationId,
          branchId: login.branchId,
          branchLabel: login.branchLabel,
          deviceId: device.deviceId,
        );
        ref.read(apiClientProvider).setAuthToken(session.authToken);
        await bindBranchScope(session);
        FcmService(apiClient: ref.read(apiClientProvider))
            .registerForUser(session.userId);
      }

      await StaffStore().save(
        staff: result.staff,
        permissionVersion: result.permissionVersion,
        sessionId: result.sessionId,
      );
      ref.read(apiClientProvider).setStaffId(result.staff.id);
      state = AuthState(
        status: AuthStatus.authenticated,
        session: session,
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        busy: false,
        errorMessage: e.toString().replaceFirst('Exception: ', ''),
        status: existing == null
            ? AuthStatus.unauthenticated
            : AuthStatus.needsMpin,
      );
      return false;
    }
  }

  /* Hard logout: wipe local licence/session and return to licence login. */
  Future<void> clearLicence() async {
    final session = state.session;
    final userId = session?.userId;
    if (userId != null && userId.isNotEmpty) {
      await FcmService(apiClient: ref.read(apiClientProvider))
          .clearForUser(userId);
    }
    await repo.clearSession(licenceKey: session?.licenceKey);
    await StaffStore().clear();
    ref.read(apiClientProvider).setAuthToken(null);
    ref.read(apiClientProvider).setStaffId(null);
    state = const AuthState(status: AuthStatus.unauthenticated);
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }

  Future<bool> updateAppPin(String newPin) async {
    final session = state.session;
    if (session == null) return false;
    final pin = newPin.trim();
    if (pin.length != 4 || int.tryParse(pin) == null) return false;

    if (AppPlatform.requiresNetwork) {
      await requireOnlineForWeb();
      final device = await DeviceIdentityService().resolve();
      final response = await AuthApi(ref.read(apiClientProvider)).updateMpin(
        mpin: pin,
        licenceKey: session.licenceKey,
        deviceId: device.deviceId,
        deviceName: device.deviceName,
      );
      if (!response.isSuccess) return false;
    } else if (await isDeviceOnline()) {
      try {
        final device = await DeviceIdentityService().resolve();
        await AuthApi(ref.read(apiClientProvider)).updateMpin(
          mpin: pin,
          licenceKey: session.licenceKey,
          deviceId: device.deviceId,
          deviceName: device.deviceName,
        );
      } catch (_) {
        /* Mobile can keep local PIN if cloud update fails. */
      }
    }

    await SessionStore().updateAppPin(pin);
    final updated = session.copyWith(appPin: pin);
    await SessionStore().saveSession(updated);
    state = state.copyWith(session: updated);
    return true;
  }
}

final authControllerProvider =
    NotifierProvider<AuthController, AuthState>(AuthController.new);
