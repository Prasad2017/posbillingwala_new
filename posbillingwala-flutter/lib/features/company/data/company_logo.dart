import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/* Encode local shop logo for `companys.companyLogo` (data-URL). */
Future<String?> encodeShopLogoFile(String? path) async {
  final raw = path?.trim() ?? '';
  if (raw.isEmpty) return null;
  if (raw.startsWith('data:image')) return raw;
  try {
    final file = File(raw);
    if (!await file.exists()) return null;
    final bytes = await file.readAsBytes();
    /* Cap ~400KB raw (~550KB base64) so PHP POST / MySQL packet stay safe. */
    if (bytes.isEmpty || bytes.length > 400000) return null;
    return 'data:image/jpeg;base64,${base64Encode(bytes)}';
  } catch (_) {
    return null;
  }
}

/* Persist cloud `companyLogo` data-URL to Documents/shop_logo.jpg. */
Future<String?> materializeShopLogo(String? companyLogo) async {
  final raw = companyLogo?.trim() ?? '';
  if (raw.isEmpty || !raw.startsWith('data:image')) return null;
  final comma = raw.indexOf(',');
  if (comma <= 0 || comma >= raw.length - 1) return null;
  try {
    final bytes = base64Decode(raw.substring(comma + 1));
    if (bytes.isEmpty) return null;
    final docs = await getApplicationDocumentsDirectory();
    final dest = File('${docs.path}/shop_logo.jpg');
    await dest.writeAsBytes(bytes, flush: true);
    return dest.path;
  } catch (_) {
    return null;
  }
}
