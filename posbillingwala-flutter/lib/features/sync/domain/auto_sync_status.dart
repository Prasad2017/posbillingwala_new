import 'package:flutter_riverpod/flutter_riverpod.dart';

class AutoSyncStatus {
  const AutoSyncStatus({
    this.online = true,
    this.syncing = false,
    this.pendingCount = 0,
    this.lastSuccessAt,
    this.lastError,
  });

  final bool online;
  final bool syncing;
  final int pendingCount;
  final DateTime? lastSuccessAt;
  final String? lastError;

  bool get chipHealthy =>
      online && !syncing && pendingCount == 0 && lastError == null;

  String get chipLabel {
    if (!online) {
      return pendingCount > 0 ? 'Offline · $pendingCount pending' : 'Offline';
    }
    if (syncing) return 'Syncing…';
    if (pendingCount > 0) return '$pendingCount pending';
    if (lastError != null) return 'Sync failed';
    return 'Synced';
  }

  AutoSyncStatus copyWith({
    bool? online,
    bool? syncing,
    int? pendingCount,
    DateTime? lastSuccessAt,
    String? lastError,
    bool clearError = false,
    bool clearSuccess = false,
  }) {
    return AutoSyncStatus(
      online: online ?? this.online,
      syncing: syncing ?? this.syncing,
      pendingCount: pendingCount ?? this.pendingCount,
      lastSuccessAt: clearSuccess
          ? null
          : (lastSuccessAt ?? this.lastSuccessAt),
      lastError: clearError ? null : (lastError ?? this.lastError),
    );
  }
}

class AutoSyncStatusController extends Notifier<AutoSyncStatus> {
  @override
  AutoSyncStatus build() => const AutoSyncStatus();

  void setOnline(bool online) {
    if (state.online == online) return;
    state = state.copyWith(online: online);
  }

  void setSyncing(bool syncing) {
    state = state.copyWith(
      syncing: syncing,
      clearError: syncing,
    );
  }

  void setPendingCount(int count) {
    if (state.pendingCount == count) return;
    state = state.copyWith(pendingCount: count);
  }

  void setSuccess({required int pendingCount}) {
    state = state.copyWith(
      syncing: false,
      pendingCount: pendingCount,
      lastSuccessAt: DateTime.now(),
      clearError: true,
    );
  }

  void setError(String message, {int? pendingCount}) {
    state = state.copyWith(
      syncing: false,
      pendingCount: pendingCount,
      lastError: message,
    );
  }
}

final autoSyncStatusProvider =
    NotifierProvider<AutoSyncStatusController, AutoSyncStatus>(
  AutoSyncStatusController.new,
);
