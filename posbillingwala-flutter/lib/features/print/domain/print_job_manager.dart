import 'dart:async';

import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_job_state.dart';

/* Serializes print jobs per printer endpoint.
 * Multiple printers print independently; same printer never gets concurrent writes.
 * Does NOT automatically retry uncertain network failures (duplicate-bill risk). */
class PrintJobManager {
  PrintJobManager._();

  static final PrintJobManager instance = PrintJobManager._();

  final Map<String, Future<void>> _tails = {};
  final Map<String, PrintJobState> _states = {};
  int _seq = 0;

  PrintJobState stateOf(String printerKey) =>
      _states[printerKey] ?? PrintJobState.created;

  Future<T> runExclusive<T>({
    required String printerKey,
    required String jobLabel,
    required Future<T> Function(void Function(PrintJobState) setState) body,
  }) async {
    final jobId = 'job-${++_seq}';
    final previous = _tails[printerKey] ?? Future<void>.value();
    final gate = Completer<void>();
    _tails[printerKey] = previous.then((_) => gate.future);

    await previous;
    final sw = Stopwatch()..start();
    _states[printerKey] = PrintJobState.created;
    AppLogger.info(
      'PrintJob start id=$jobId key=$printerKey label=$jobLabel',
    );
    try {
      final result = await body((s) => _states[printerKey] = s);
      final finalState = _states[printerKey];
      if (finalState == null || !finalState.isTerminal) {
        _states[printerKey] = PrintJobState.completed;
      }
      AppLogger.info(
        'PrintJob end id=$jobId key=$printerKey '
        'state=${_states[printerKey]?.label} ms=${sw.elapsedMilliseconds}',
      );
      return result;
    } catch (e, st) {
      _states[printerKey] = PrintJobState.failed;
      AppLogger.warning(
        'PrintJob failed id=$jobId key=$printerKey ms=${sw.elapsedMilliseconds}',
        e,
        st,
      );
      rethrow;
    } finally {
      gate.complete();
    }
  }
}
