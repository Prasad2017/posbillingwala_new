import 'package:flutter_riverpod/flutter_riverpod.dart';

enum SyncTableStatus { pending, running, complete, error, skipped }

enum SyncScreenMode { upload, fetch }

class SyncTableStep {
  const SyncTableStep({
    required this.id,
    required this.label,
    this.status = SyncTableStatus.pending,
    this.message,
  });

  final String id;
  final String label;
  final SyncTableStatus status;
  final String? message;

  SyncTableStep copyWith({
    SyncTableStatus? status,
    String? message,
  }) {
    return SyncTableStep(
      id: id,
      label: label,
      status: status ?? this.status,
      message: message ?? this.message,
    );
  }
}

class SyncProgressState {
  const SyncProgressState({
    required this.mode,
    required this.steps,
    this.isRunning = false,
    this.isFinished = false,
    this.failed = 0,
    this.headline,
    this.subtitle,
    this.displayStep = 0,
  });

  final SyncScreenMode mode;
  final List<SyncTableStep> steps;
  final bool isRunning;
  final bool isFinished;
  final int failed;
  final String? headline;
  final String? subtitle;

  /// 1-based step shown in "Fetching data... (x/y)".
  final int displayStep;

  bool get allComplete =>
      steps.isNotEmpty &&
      steps.every(
        (s) =>
            s.status == SyncTableStatus.complete ||
            s.status == SyncTableStatus.skipped,
      );

  int get totalCount => steps.length;

  /// 1-based index for Android-style "Fetching data... (3/21)".
  int get currentIndex {
    if (displayStep > 0) return displayStep.clamp(1, totalCount);
    if (isFinished) return totalCount;
    return isRunning ? 1 : 0;
  }

  SyncProgressState copyWith({
    SyncScreenMode? mode,
    List<SyncTableStep>? steps,
    bool? isRunning,
    bool? isFinished,
    int? failed,
    String? headline,
    String? subtitle,
    int? displayStep,
  }) {
    return SyncProgressState(
      mode: mode ?? this.mode,
      steps: steps ?? this.steps,
      isRunning: isRunning ?? this.isRunning,
      isFinished: isFinished ?? this.isFinished,
      failed: failed ?? this.failed,
      headline: headline ?? this.headline,
      subtitle: subtitle ?? this.subtitle,
      displayStep: displayStep ?? this.displayStep,
    );
  }

  static List<SyncTableStep> uploadSteps() => const [
        SyncTableStep(id: 'categories', label: 'Categories'),
        SyncTableStep(id: 'subcategories', label: 'Subcategories'),
        SyncTableStep(id: 'products', label: 'Products'),
        SyncTableStep(id: 'portion_master', label: 'Portion master'),
        SyncTableStep(id: 'portions', label: 'Portions'),
        SyncTableStep(id: 'combos', label: 'Combos'),
        SyncTableStep(id: 'combo_items', label: 'Combo items'),
        SyncTableStep(id: 'printer_settings', label: 'Printer settings'),
        SyncTableStep(id: 'shop_details', label: 'Shop details'),
        SyncTableStep(id: 'invoice_item_deletes', label: 'Invoice item deletes'),
        SyncTableStep(id: 'invoice_items', label: 'Invoice items'),
        SyncTableStep(id: 'invoice_combo_items', label: 'Invoice combo items'),
        SyncTableStep(id: 'invoices', label: 'Invoices'),
        SyncTableStep(id: 'mess_members', label: 'Mess members'),
        SyncTableStep(id: 'mess_payments', label: 'Mess payments'),
        SyncTableStep(id: 'mess_invoices', label: 'Mess invoices'),
        SyncTableStep(id: 'inventory', label: 'Inventory'),
        SyncTableStep(id: 'expenses', label: 'Expenses'),
        SyncTableStep(id: 'dining_areas', label: 'Dining areas'),
        SyncTableStep(id: 'table_types', label: 'Table types'),
        SyncTableStep(id: 'tables', label: 'Tables'),
        SyncTableStep(id: 'mess_tokens', label: 'Mess tokens'),
      ];

  static List<SyncTableStep> fetchSteps() => const [
        SyncTableStep(id: 'categories', label: 'Categories'),
        SyncTableStep(id: 'subcategories', label: 'Subcategories'),
        SyncTableStep(id: 'products', label: 'Products'),
        SyncTableStep(id: 'portion_master', label: 'Portion master'),
        SyncTableStep(id: 'portions', label: 'Portions'),
        SyncTableStep(id: 'combos', label: 'Combos'),
        SyncTableStep(id: 'combo_items', label: 'Combo items'),
        SyncTableStep(id: 'dining_areas', label: 'Dining areas'),
        SyncTableStep(id: 'table_types', label: 'Table types'),
        SyncTableStep(id: 'tables', label: 'Tables'),
        SyncTableStep(id: 'invoice_items', label: 'Invoice items'),
        SyncTableStep(id: 'invoice_combo_items', label: 'Invoice combo items'),
        SyncTableStep(id: 'invoices', label: 'Invoices'),
        SyncTableStep(id: 'inventory', label: 'Inventory'),
        SyncTableStep(id: 'expenses', label: 'Expenses'),
        SyncTableStep(id: 'mess_members', label: 'Mess members'),
        SyncTableStep(id: 'mess_payments', label: 'Mess payments'),
        SyncTableStep(id: 'mess_invoices', label: 'Mess invoices'),
        SyncTableStep(id: 'mess_tokens', label: 'Mess tokens'),
        SyncTableStep(id: 'printer_settings', label: 'Printer settings'),
        SyncTableStep(id: 'shop_details', label: 'Shop details'),
      ];

  factory SyncProgressState.initial(SyncScreenMode mode) {
    final isUpload = mode == SyncScreenMode.upload;
    return SyncProgressState(
      mode: mode,
      steps: isUpload ? uploadSteps() : fetchSteps(),
      headline: isUpload ? 'Preparing upload…' : 'Preparing fetch…',
      subtitle: isUpload
          ? 'Checking offline data for cloud sync'
          : 'Cloud data will replace local data',
    );
  }
}

class SyncProgressController extends Notifier<SyncProgressState> {
  @override
  SyncProgressState build() =>
      SyncProgressState.initial(SyncScreenMode.upload);

  void reset(SyncScreenMode mode) {
    state = SyncProgressState.initial(mode);
  }

  void setHeader(String headline, String subtitle) {
    state = state.copyWith(headline: headline, subtitle: subtitle);
  }

  void markGroup(
    List<String> ids, {
    required SyncTableStatus status,
    String? message,
  }) {
    final idSet = ids.toSet();
    state = state.copyWith(
      steps: [
        for (final step in state.steps)
          if (idSet.contains(step.id))
            step.copyWith(status: status, message: message)
          else
            step,
      ],
    );
  }

  void markRunning(List<String> ids) =>
      markGroup(ids, status: SyncTableStatus.running);

  void markComplete(List<String> ids) =>
      markGroup(ids, status: SyncTableStatus.complete);

  void markError(List<String> ids, [String? message]) =>
      markGroup(ids, status: SyncTableStatus.error, message: message);

  /// Marks [ids] running then complete one-by-one so (x/y) advances smoothly.
  Future<void> completeSequentially(
    List<String> ids, {
    bool error = false,
    String? message,
  }) async {
    var cursor = state.displayStep;
    for (final id in ids) {
      final index = state.steps.indexWhere((s) => s.id == id);
      if (index >= 0) {
        cursor = index + 1;
      } else {
        cursor = (cursor + 1).clamp(1, state.totalCount);
      }
      state = state.copyWith(displayStep: cursor);
      markRunning([id]);
      await Future<void>.delayed(const Duration(milliseconds: 35));
      if (error) {
        markError([id], message);
      } else {
        markComplete([id]);
      }
    }
  }

  void begin(SyncScreenMode mode) {
    state = SyncProgressState.initial(mode).copyWith(
      isRunning: true,
      isFinished: false,
      failed: 0,
      displayStep: 1,
      headline: mode == SyncScreenMode.upload
          ? 'Uploading to cloud…'
          : 'Fetching from cloud…',
      subtitle: mode == SyncScreenMode.upload
          ? 'Sending offline data to the server'
          : 'Downloading latest data from cloud',
    );
  }

  void finish({required int failed, required bool hadPending}) {
    final isUpload = state.mode == SyncScreenMode.upload;
    final ok = failed == 0;
    late final String headline;
    late final String subtitle;
    if (isUpload) {
      if (ok && !hadPending) {
        headline = 'All data is on the cloud';
        subtitle = 'Nothing left to upload.';
      } else if (ok) {
        headline = 'Upload complete';
        subtitle = 'Offline data synchronized with cloud.';
      } else {
        headline = 'Upload finished with issues';
        subtitle = '$failed table group(s) need attention.';
      }
    } else {
      if (ok) {
        headline = 'Fetch complete';
        subtitle = 'Local data replaced with cloud data.';
      } else {
        headline = 'Fetch finished with issues';
        subtitle = '$failed table group(s) need attention.';
      }
    }

    // Mark any leftover pending steps as complete when overall ok.
    final steps = [
      for (final step in state.steps)
        if (ok &&
            (step.status == SyncTableStatus.pending ||
                step.status == SyncTableStatus.running))
          step.copyWith(status: SyncTableStatus.complete)
        else if (!ok && step.status == SyncTableStatus.running)
          step.copyWith(status: SyncTableStatus.error)
        else
          step,
    ];

    state = state.copyWith(
      isRunning: false,
      isFinished: true,
      failed: failed,
      headline: headline,
      subtitle: subtitle,
      steps: steps,
      displayStep: state.totalCount,
    );
  }

  void setBlocked({required String headline, required String subtitle}) {
    state = state.copyWith(
      isRunning: false,
      isFinished: true,
      failed: 1,
      headline: headline,
      subtitle: subtitle,
    );
    setHeader(headline, subtitle);
  }
}

final syncProgressProvider =
    NotifierProvider<SyncProgressController, SyncProgressState>(
  SyncProgressController.new,
);
