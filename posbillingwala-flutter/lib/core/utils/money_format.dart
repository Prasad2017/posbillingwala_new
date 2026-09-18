import 'package:intl/intl.dart';

/* Cached INR formatters — NumberFormat construction is expensive in rebuilds. */
abstract final class MoneyFormat {
  MoneyFormat._();

  static final inr = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
  static final inrSpaced = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');
  static final inrRs = NumberFormat.currency(locale: 'en_IN', symbol: 'Rs. ');
}

/* Empty input when no real value — show hint only, never dummy 0 / 0.00. */
String amountInputText(num? value, {int decimals = 2}) {
  if (value == null) return '';
  final n = value.toDouble();
  if (n == 0) return '';
  if (n == n.roundToDouble()) return n.toInt().toString();
  var fixed = n.toStringAsFixed(decimals);
  fixed = fixed.replaceFirst(RegExp(r'0+$'), '');
  if (fixed.endsWith('.')) fixed = fixed.substring(0, fixed.length - 1);
  return fixed;
}

String numericInputText(num? value) {
  if (value == null) return '';
  final n = value.toDouble();
  if (n == 0) return '';
  return n == n.roundToDouble() ? n.toInt().toString() : n.toString();
}

String textOrEmpty(String? raw) {
  final t = raw?.trim() ?? '';
  if (t.isEmpty || t == '0' || t == '0.0' || t == '0.00' || t == '00') {
    return '';
  }
  return t;
}
