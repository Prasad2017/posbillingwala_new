import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:qr/qr.dart';

class UpiQrValidationResult {
  const UpiQrValidationResult._({
    required this.ok,
    this.uri,
    this.svg,
    this.error,
  });

  factory UpiQrValidationResult.success({
    required String uri,
    required String svg,
  }) => UpiQrValidationResult._(ok: true, uri: uri, svg: svg);

  factory UpiQrValidationResult.failure(String error) =>
      UpiQrValidationResult._(ok: false, error: error);

  final bool ok;
  final String? uri;
  final String? svg;
  final String? error;
}

/* Validates UPI config + amount and builds payment URI + SVG QR. */
class UpiQrService {
  const UpiQrService();

  static final _upiIdPattern = RegExp(
    r'^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z][a-zA-Z0-9.\-]{1,64}$',
  );

  bool isValidUpiId(String upiId) {
    final v = upiId.trim();
    if (v.isEmpty || v.length > 120) return false;
    if (v.contains(' ') || v.contains('\n')) return false;
    return _upiIdPattern.hasMatch(v);
  }

  String resolvePayeeName(ShopReceiptProfile shop) {
    final shopName = shop.shopName1.trim();
    if (shopName.isNotEmpty) return shopName;
    final company = shop.companyName.trim();
    if (company.isNotEmpty) return company;
    return 'Merchant';
  }

  String resolveShopName(ShopReceiptProfile shop) => resolvePayeeName(shop);

  UpiQrValidationResult generate({
    required String upiId,
    required String payeeName,
    required double amount,
    String note = '',
  }) {
    if (!isValidUpiId(upiId)) {
      return UpiQrValidationResult.failure('UPI payment is not configured.');
    }
    if (amount.isNaN || amount.isInfinite || amount <= 0) {
      return UpiQrValidationResult.failure('Invalid payment amount.');
    }
    final name = payeeName.trim().isEmpty ? 'Merchant' : payeeName.trim();
    try {
      final uri = buildUpiPayUri(
        upiId: upiId.trim(),
        payeeName: name,
        amount: amount,
        note: note,
      );
      final svg = buildQrSvg(uri);
      return UpiQrValidationResult.success(uri: uri, svg: svg);
    } catch (e) {
      return UpiQrValidationResult.failure('QR generation failed.');
    }
  }

  String buildQrSvg(String data, {int moduleSize = 8, int marginModules = 2}) {
    final qrCode = QrCode.fromData(
      data: data,
      errorCorrectLevel: QrErrorCorrectLevel.M,
    );
    final qrImage = QrImage(qrCode);
    final modules = qrImage.moduleCount;
    final dim = (modules + marginModules * 2) * moduleSize;
    final buffer = StringBuffer();
    buffer.write(
      '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $dim $dim" '
      'width="100%" height="100%" shape-rendering="crispEdges">',
    );
    buffer.write('<rect width="$dim" height="$dim" fill="#ffffff"/>');
    for (var y = 0; y < modules; y++) {
      for (var x = 0; x < modules; x++) {
        if (!qrImage.isDark(y, x)) continue;
        final px = (x + marginModules) * moduleSize;
        final py = (y + marginModules) * moduleSize;
        buffer.write(
          '<rect x="$px" y="$py" width="$moduleSize" height="$moduleSize" fill="#000000"/>',
        );
      }
    }
    buffer.write('</svg>');
    return buffer.toString();
  }
}
