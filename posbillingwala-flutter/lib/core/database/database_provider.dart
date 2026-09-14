import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';

final appDatabaseProvider = Provider<AppDatabase>((ref) {
  final db = AppDatabase();
  PrinterSettingsStore.dbOverlay = (prefs) async {
    final row = await db.getLocalCompanyPrinterSettings();
    return db.mergePrinterSettings(prefs, row);
  };
  PrinterSettingsStore.dbPersist = db.upsertLocalCompanyPrinterFromSettings;
  ref.onDispose(() {
    PrinterSettingsStore.dbOverlay = null;
    PrinterSettingsStore.dbPersist = null;
    db.close();
  });
  return db;
});
