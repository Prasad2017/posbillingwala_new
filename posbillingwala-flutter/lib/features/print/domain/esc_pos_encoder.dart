import 'dart:convert';
import 'dart:typed_data';

/* Minimal ESC/POS builder for thermal printers (paper width via charsPerLine). */
class EscPosEncoder {
  EscPosEncoder({this.charsPerLine = 32});

  /* Font A columns: 32≈58mm, 33≈60mm, 46≈78mm, 48≈80mm. */
  final int charsPerLine;
  final BytesBuilder escPosEncoderBytes = BytesBuilder();

  List<int> get bytes => escPosEncoderBytes.toBytes();

  void raw(List<int> data) => escPosEncoderBytes.add(data);

  void init() {
    raw(const [0x1B, 0x40]); /* ESC @ */
  }

  void alignLeft() => raw(const [0x1B, 0x61, 0x00]);

  void alignCenter() => raw(const [0x1B, 0x61, 0x01]);

  void alignRight() => raw(const [0x1B, 0x61, 0x02]);

  void bold(bool on) => raw([0x1B, 0x45, on ? 0x01 : 0x00]);

  void text(String value, {bool boldStyle = false, bool center = false}) {
    if (center) {
      alignCenter();
    } else {
      alignLeft();
    }
    bold(boldStyle);
    final encoded = latin1.encode(sanitize(value));
    raw(encoded);
    raw(const [0x0A]);
    bold(false);
    alignLeft();
  }

  void line(String left, String right) {
    final space = charsPerLine - left.length - right.length;
    final gap = space > 1 ? ' ' * space : ' ';
    text('$left$gap$right');
  }

  void separator([String char = '-']) {
    text(char * charsPerLine);
  }

  /* Advance [lines] only — caller must pass the user feed-line count. */
  void feed([int lines = 0]) {
    final n = lines.clamp(0, 20);
    for (var i = 0; i < n; i++) {
      raw(const [0x0A]);
    }
  }

  /* Paper cut for production thermal printers (incl. cheap BT clones).
   *
   * Many GLPrinter / POS-80-style firmwares ignore classic GS V 0/1 alone.
   * Sequence: ESC d n (advance [feedToCutter] lines) → GS V 65/66 (Function B) →
   * GS V 0/1 (Function A fallback). Printers without a cutter ignore these.
   *
   * [feedToCutter] must be the user-configured feed lines — never a hardcoded count.
   */
  void cut({bool full = true, int feedToCutter = 0}) {
    final n = feedToCutter.clamp(0, 20);
    if (n > 0) {
      raw([0x1B, 0x64, n]); /* ESC d n — feed n lines to cutter */
    }
    /* Function B — Epson + most Chinese auto-cutters (m=65 full, 66 partial). */
    raw([0x1D, 0x56, full ? 0x41 : 0x42, 0x00]);
    /* Function A — older / alternate firmware. */
    raw([0x1D, 0x56, full ? 0x00 : 0x01]);
  }

  String sanitize(String value) {
    return value
        .replaceAll('₹', 'Rs.')
        .replaceAll('—', '-')
        .replaceAll('•', '*')
        .replaceAll(RegExp(r'[^\x20-\x7E\n]'), '?');
  }
}
