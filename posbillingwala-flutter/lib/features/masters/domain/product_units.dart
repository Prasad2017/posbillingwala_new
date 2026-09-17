/* Unit labels + qty step helpers for decimal stock / billing. */
abstract final class ProductUnits {
  ProductUnits._();

  static const list = <String>[
    'Piece',
    'KG',
    'Gram',
    'Plate',
    'Glass',
    'Bowl',
    'Packet',
    'Litre',
    'Dozen',
    'Cup',
    'Box',
  ];

  static String normalize(String? raw) {
    final u = (raw ?? '').trim();
    if (u.isEmpty) return list.first;
    final lower = u.toLowerCase().replaceAll(' ', '');
    switch (lower) {
      case 'pcs':
      case 'pc':
      case 'pic':
      case 'piece':
      case 'pieces':
        return 'Piece';
      case 'kg':
      case 'kgs':
      case 'kilogram':
      case 'kilograms':
        return 'KG';
      case 'gram':
      case 'grams':
      case 'gm':
      case 'gms':
        return 'Gram';
      case 'plate':
        return 'Plate';
      case 'glass':
        return 'Glass';
      case 'bowl':
        return 'Bowl';
      case 'packet':
      case 'pkt':
        return 'Packet';
      case 'litre':
      case 'liter':
      case 'ltr':
      case 'l':
        return 'Litre';
      case 'dozen':
      case 'doz':
        return 'Dozen';
      case 'cup':
        return 'Cup';
      case 'box':
        return 'Box';
      default:
        return list.contains(u) ? u : list.first;
    }
  }

  /* +/- step on POS cart for this unit. */
  static double stepFor(String? unit) {
    switch (normalize(unit)) {
      case 'KG':
      case 'Litre':
        return 0.1;
      case 'Gram':
        return 50;
      default:
        return 1;
    }
  }

  static bool allowsFraction(String? unit) {
    switch (normalize(unit)) {
      case 'KG':
      case 'Gram':
      case 'Litre':
        return true;
      default:
        return false;
    }
  }

  static String formatQty(double qty, {String? unit}) {
    if (allowsFraction(unit)) {
      final t = qty.toStringAsFixed(3);
      return t.replaceFirst(RegExp(r'\.?0+$'), '');
    }
    if (qty == qty.roundToDouble()) return '${qty.round()}';
    return qty.toStringAsFixed(2);
  }
}
