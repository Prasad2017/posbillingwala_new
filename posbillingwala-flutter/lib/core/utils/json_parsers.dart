import 'package:intl/intl.dart';

double parseMoney(Object? value) {
  if (value == null) return 0;
  if (value is num) return value.toDouble();
  return double.tryParse(value.toString().trim()) ?? 0;
}

/* Alias used by cloud invoice DTOs (same rules as [parseMoney]). */
double parseCloudMoney(Object? value) => parseMoney(value);

int? parseInt(Object? value) {
  if (value == null) return null;
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value.toString().trim());
}

String? parseString(Object? value) {
  if (value == null) return null;
  return value.toString();
}

DateTime? parseInvoiceDate(Object? value) {
  if (value == null) return null;
  if (value is DateTime) return value;
  final text = value.toString().trim();
  if (text.isEmpty) return null;
  final direct = DateTime.tryParse(text.replaceFirst(' ', 'T'));
  if (direct != null) return direct;
  for (final pattern in [
    'yyyy-MM-dd HH:mm:ss',
    'yyyy-MM-dd',
    'dd-MM-yyyy HH:mm:ss',
    'dd/MM/yyyy HH:mm:ss',
    'dd-MM-yyyy',
    'dd/MM/yyyy',
  ]) {
    try {
      return DateFormat(pattern).parse(text);
    } catch (_) {}
  }
  return null;
}
