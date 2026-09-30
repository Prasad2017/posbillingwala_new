/* Production cut modes — never assume one ESC/POS cut works on every model. */
enum PrinterCutType {
  /* Prefer library / profile default (full cut for generic ESC/POS). */
  defaultCut,
  full,
  partial,
}

extension PrinterCutTypeX on PrinterCutType {
  String get storageValue {
    switch (this) {
      case PrinterCutType.full:
        return 'full';
      case PrinterCutType.partial:
        return 'partial';
      case PrinterCutType.defaultCut:
        return 'default';
    }
  }

  String get label {
    switch (this) {
      case PrinterCutType.full:
        return 'Full cut';
      case PrinterCutType.partial:
        return 'Partial cut';
      case PrinterCutType.defaultCut:
        return 'Default';
    }
  }

  static PrinterCutType fromStorage(String? raw) {
    switch ((raw ?? '').trim().toLowerCase()) {
      case 'full':
        return PrinterCutType.full;
      case 'partial':
        return PrinterCutType.partial;
      default:
        return PrinterCutType.defaultCut;
    }
  }

  /* GS V m — full=0, partial=1. Default uses full (most common with cutter). */
  bool get useFullCut => this != PrinterCutType.partial;
}
