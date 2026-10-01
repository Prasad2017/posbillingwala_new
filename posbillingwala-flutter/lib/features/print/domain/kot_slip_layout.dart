/* Structured KOT slip — preview + thermal raster (invoice-style columns). */
class KotSlipLayout {
  const KotSlipLayout({
    required this.title,
    required this.metaLines,
    required this.colItem,
    required this.colQty,
    required this.items,
  });

  final String title;
  final List<String> metaLines;
  final String colItem;
  final String colQty;
  final List<KotSlipItem> items;
}

class KotSlipItem {
  const KotSlipItem({required this.name, required this.qty});

  final String name;
  final String qty;
}
