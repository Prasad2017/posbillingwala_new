import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/domain/thermal_ticket.dart';

/* Structured mess slip — shop header uses same lines as invoice bills. */
class MessSlipLayout {
  const MessSlipLayout({
    required this.shopLines,
    required this.bodyLines,
    required this.footerLines,
    this.qrPayload,
  });

  /* Same [ShopReceiptProfile.headerLines] as bill / invoice. */
  final List<String> shopLines;
  /* Title + member details (before QR). */
  final List<String> bodyLines;
  /* Token line + powered-by (after QR). */
  final List<String> footerLines;
  final String? qrPayload;

  String toPlainText({required int width}) {
    final buf = StringBuffer();
    void writeCentered(String line) {
      buf.writeln(MessSlipBuilder.center(line, width));
    }

    for (final line in shopLines) {
      writeCentered(line);
    }
    for (final line in bodyLines) {
      writeCentered(line);
    }
    final qr = qrPayload?.trim() ?? '';
    if (qr.isNotEmpty) {
      buf.writeln(MessSlipBuilder.qrMarker);
    }
    for (final line in footerLines) {
      writeCentered(line);
    }
    return buf.toString();
  }
}

/* Android CouponBluetoothPrint / MessTokenBluetoothPrint / MessMealTokenPrintWorker. */
abstract final class MessSlipBuilder {
  static final _couponDate = DateFormat('dd MMM, yyyy hh:mm a');
  static final _tokenDateTime = DateFormat('yyyy-MM-dd HH:mm:ss');
  static final _mealDate = DateFormat('dd-MMM-yyyy');
  static final _tokenTime = DateFormat('HH:mm');

  /* Inline QR marker — ReceiptRasterizer swaps this for the QR image. */
  static const qrMarker = ThermalTicket.upiQrMarker;

  static String center(String text, int width) {
    if (text.length >= width) return text;
    final pad = (width - text.length) ~/ 2;
    return '${' ' * pad}$text';
  }

  /* Same shop name / address / mobile / GST lines as invoice thermal bill. */
  static List<String> shopHeaderLines(
    ShopReceiptProfile profile, {
    String fallbackTitle = 'Billingwala',
  }) {
    final lines = profile.headerLines();
    if (lines.isEmpty) return [fallbackTitle];
    return lines;
  }

  static void writeShopHeader(
    StringBuffer buf, {
    required ShopReceiptProfile profile,
    required int width,
    String fallbackTitle = 'Billingwala',
  }) {
    for (final line in shopHeaderLines(profile, fallbackTitle: fallbackTitle)) {
      buf.writeln(center(line, width));
    }
  }

  static void writePoweredBy(StringBuffer buf, int width) {
    buf
      ..writeln(center('Powered by POS Billingwala', width))
      ..writeln(center('www.posbillingwala.com', width));
  }

  static List<String> poweredByLines() => const [
    'Powered by POS Billingwala',
    'www.posbillingwala.com',
  ];

  /* Android CouponBluetoothPrint 2" layout (logo via printMessSlip). */
  static MessSlipLayout couponLayout({
    required ShopReceiptProfile profile,
    required String memberName,
    required String messType,
    required int couponNo,
  }) {
    final now = _couponDate.format(DateTime.now());
    final body = <String>[
      'MESS COUPON',
      memberName.trim(),
      if (messType.trim().isNotEmpty) messType.trim(),
      now,
      'MESS COUPON No: $couponNo',
    ];
    return MessSlipLayout(
      shopLines: shopHeaderLines(profile),
      bodyLines: body.where((e) => e.isNotEmpty).toList(),
      footerLines: poweredByLines(),
    );
  }

  static String couponSlip({
    required ShopReceiptProfile profile,
    required String memberName,
    required String messType,
    required int couponNo,
    int width = 32,
  }) {
    return couponLayout(
      profile: profile,
      memberName: memberName,
      messType: messType,
      couponNo: couponNo,
    ).toPlainText(width: width);
  }

  /*
   * Android MessTokenBluetoothPrint layout:
   * shop header → MESS QR TOKEN → name → mobile → meal → date → QR → Token → footer
   */
  static MessSlipLayout qrTokenLayout({
    required ShopReceiptProfile profile,
    required String memberName,
    required String memberMobile,
    required String messType,
    required String tokenCode,
    DateTime? printedAt,
    String? qrPayload,
  }) {
    final when = printedAt ?? DateTime.now();
    final code = tokenCode.trim().toUpperCase();
    final short = code.length > 8 ? code.substring(0, 8) : code;
    final body = <String>[
      'MESS QR TOKEN',
      memberName.trim(),
      memberMobile.trim(),
      if (messType.trim().isNotEmpty) messType.trim(),
      _tokenDateTime.format(when),
    ];
    return MessSlipLayout(
      shopLines: shopHeaderLines(profile),
      bodyLines: body.where((e) => e.isNotEmpty).toList(),
      footerLines: [
        'Token: $short',
        ...poweredByLines(),
      ],
      qrPayload: qrPayload,
    );
  }

  static String qrTokenSlip({
    required ShopReceiptProfile profile,
    required String memberName,
    required String memberMobile,
    required String messType,
    required String tokenCode,
    DateTime? printedAt,
    int width = 32,
  }) {
    return qrTokenLayout(
      profile: profile,
      memberName: memberName,
      memberMobile: memberMobile,
      messType: messType,
      tokenCode: tokenCode,
      printedAt: printedAt,
    ).toPlainText(width: width);
  }

  /* Android MessMealTokenPrintWorker.buildSlipBitmap — centered, no shop header. */
  static MessSlipLayout mealTokenLayout({
    required String tokenNumber,
    required String memberName,
    required String registrationNo,
    required String mealSession,
    required String tokenDate,
    String? createdAt,
  }) {
    final date = tokenDate.trim().isEmpty
        ? _mealDate.format(DateTime.now())
        : tokenDate.trim();
    final created = (createdAt ?? '').trim();
    final time = created.length >= 16
        ? created.substring(11, 16)
        : (created.isNotEmpty ? created : _tokenTime.format(DateTime.now()));
    return MessSlipLayout(
      shopLines: const ['BILLINGWALA'],
      bodyLines: [
        'MESS TOKEN',
        'Token: $tokenNumber',
        'Name: $memberName',
        'Mobile: $registrationNo',
        'Meal: $mealSession',
        'Date: $date',
        'Time: $time',
      ],
      footerLines: poweredByLines(),
    );
  }

  static String mealTokenSlip({
    required String tokenNumber,
    required String memberName,
    required String registrationNo,
    required String mealSession,
    required String tokenDate,
    String? createdAt,
    int width = 32,
  }) {
    return mealTokenLayout(
      tokenNumber: tokenNumber,
      memberName: memberName,
      registrationNo: registrationNo,
      mealSession: mealSession,
      tokenDate: tokenDate,
      createdAt: createdAt,
    ).toPlainText(width: width);
  }

  /* Common mess QR — same shop header as invoice / mess token slip. */
  static MessSlipLayout commonQrLayout({
    required ShopReceiptProfile profile,
    String? messTitle,
    required String qrPayload,
  }) {
    final title = (messTitle ?? '').trim().isEmpty
        ? 'MESS QR'
        : messTitle!.trim().toUpperCase();
    return MessSlipLayout(
      shopLines: shopHeaderLines(profile),
      bodyLines: [title],
      footerLines: poweredByLines(),
      qrPayload: qrPayload,
    );
  }

  static String commonQrSlip({
    required ShopReceiptProfile profile,
    String? messTitle,
    int width = 32,
  }) {
    return commonQrLayout(
      profile: profile,
      messTitle: messTitle,
      qrPayload: '',
    ).toPlainText(width: width);
  }

  /* Android MessTokenQrHelper.resolveMessType (+ 2nd print → Dinner). */
  static String resolveMessType({int existingPrintsToday = 0}) {
    return MessTokenQrHelper.resolveMessType(
      existingPrintsToday: existingPrintsToday,
    );
  }
}
