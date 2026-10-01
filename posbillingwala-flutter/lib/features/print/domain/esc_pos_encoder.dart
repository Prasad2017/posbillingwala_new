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

  /* Advance [lines] blank lines at bill end. Independent of auto-cut.
   * 0 → no advance. Uses ESC d n (works on BT / USB / network ESC/POS). */
  void feed([int lines = 0]) {
    final n = lines.clamp(0, 20);
    if (n <= 0) return;
    raw([0x1B, 0x64, n]); /* ESC d n */
  }

  /* Paper cut only — no paper advance.
   *
   * Ending blank space must come from [feed] with the user feed-line count,
   * whether or not the printer supports a cutter.
   * [feedToCutter] stays 0 unless a caller explicitly needs ESC d before cut.
   *
   * Many GLPrinter / POS-80-style firmwares ignore classic GS V 0/1 alone.
   * Sequence: optional ESC d n → GS V 65/66 (Function B) → GS V 0/1 (Function A).
   */
  void cut({bool full = true, int feedToCutter = 0}) {
    final n = feedToCutter.clamp(0, 20);
    if (n > 0) {
      raw([0x1B, 0x64, n]); /* ESC d n — only when caller asks */
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
