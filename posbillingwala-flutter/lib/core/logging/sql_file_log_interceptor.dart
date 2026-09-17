import 'package:drift/drift.dart';
import 'package:pos_billingwala_v2/core/constants/app_config.dart';
import 'package:pos_billingwala_v2/core/logging/file_log_store.dart';

/* Persists Drift SQL to Documents/Pos Billingwala/Logs/db_*.log */
class SqlFileLogInterceptor extends QueryInterceptor {
  @override
  Future<void> runCustom(
    QueryExecutor executor,
    String statement,
    List<Object?> args,
  ) {
    return _wrap(
      kind: 'CUSTOM',
      sql: statement,
      args: args,
      run: () => executor.runCustom(statement, args),
    );
  }

  @override
  Future<int> runInsert(
    QueryExecutor executor,
    String statement,
    List<Object?> args,
  ) {
    return _wrap(
      kind: 'INSERT',
      sql: statement,
      args: args,
      run: () => executor.runInsert(statement, args),
      summarize: (rows) => 'insertedId=$rows',
    );
  }

  @override
  Future<int> runDelete(
    QueryExecutor executor,
    String statement,
    List<Object?> args,
  ) {
    return _wrap(
      kind: 'DELETE',
      sql: statement,
      args: args,
      run: () => executor.runDelete(statement, args),
      summarize: (rows) => 'affected=$rows',
    );
  }

  @override
  Future<int> runUpdate(
    QueryExecutor executor,
    String statement,
    List<Object?> args,
  ) {
    return _wrap(
      kind: 'UPDATE',
      sql: statement,
      args: args,
      run: () => executor.runUpdate(statement, args),
      summarize: (rows) => 'affected=$rows',
    );
  }

  @override
  Future<List<Map<String, Object?>>> runSelect(
    QueryExecutor executor,
    String statement,
    List<Object?> args,
  ) {
    return _wrap(
      kind: 'SELECT',
      sql: statement,
      args: args,
      run: () => executor.runSelect(statement, args),
      summarize: (rows) => 'rows=${rows.length}',
    );
  }

  @override
  Future<void> runBatched(
    QueryExecutor executor,
    BatchedStatements statements,
  ) {
    return _wrap(
      kind: 'BATCH',
      sql: statements.statements.join('\n'),
      args: statements.arguments
          .map((a) => {'statementIndex': a.statementIndex, 'args': a.arguments})
          .toList(),
      run: () => executor.runBatched(statements),
      summarize: (_) => 'statements=${statements.statements.length}',
    );
  }

  Future<T> _wrap<T>({
    required String kind,
    required String sql,
    required List<Object?> args,
    required Future<T> Function() run,
    String Function(T value)? summarize,
  }) async {
    if (!AppConfig.enableLogging || _shouldSkip(sql)) return run();
    try {
      final result = await run();
      FileLogStore.logDbQuery(
        kind: kind,
        sql: sql,
        args: args,
        resultSummary: summarize?.call(result),
      );
      return result;
    } catch (e) {
      FileLogStore.logDbQuery(
        kind: kind,
        sql: sql,
        args: args,
        error: e.toString(),
      );
      rethrow;
    }
  }

  static bool _shouldSkip(String sql) {
    final s = sql.trim().toLowerCase();
    return s.contains('sqlite_master') ||
        s.contains('sqlite_schema') ||
        s.startsWith('pragma ');
  }
}
