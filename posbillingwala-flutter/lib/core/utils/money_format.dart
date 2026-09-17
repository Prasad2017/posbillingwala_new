import 'package:intl/intl.dart';

/* Cached INR formatters — NumberFormat construction is expensive in rebuilds. */
abstract final class MoneyFormat {
  MoneyFormat._();

  static final inr = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
  static final inrSpaced = NumberFormat.currency(locale: 'en_IN', symbol: '₹ ');
  static final inrRs = NumberFormat.currency(locale: 'en_IN', symbol: 'Rs. ');
}
