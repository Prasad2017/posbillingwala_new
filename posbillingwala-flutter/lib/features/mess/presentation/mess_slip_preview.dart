import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/receipt_rasterizer.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/features/print/presentation/bill_print_preview_page.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

/* On-screen thermal slip — same shop header + raster as bill print. */
class MessSlipPreview extends ConsumerStatefulWidget {
  const MessSlipPreview({
    super.key,
    required this.layout,
    this.showStatusLine = true,
  });

  final MessSlipLayout layout;
  final bool showStatusLine;

  @override
  ConsumerState<MessSlipPreview> createState() => MessSlipPreviewState();
}

class MessSlipPreviewState extends ConsumerState<MessSlipPreview> {
  Uint8List? png;
  Object? error;
  bool loading = true;
  String? _cacheKey;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _scheduleRender();
  }

  @override
  void didUpdateWidget(MessSlipPreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.layout != widget.layout) {
      _scheduleRender();
    }
  }

  void _scheduleRender() {
    final settings = ref.read(printerSettingsProvider);
    final shop = ref.read(shopReceiptProfileProvider);
    final paper = settings.paperSizeFor(isKot: false);
    final key =
        '${widget.layout.shopLines.join("|")}|'
        '${widget.layout.bodyLines.join("|")}|'
        '${widget.layout.footerLines.join("|")}|'
        '${widget.layout.qrPayload}|${paper.name}|'
        '${settings.logoUse}|${shop.logoLocalPath}|${settings.charsPerLine}';
    if (key == _cacheKey && png != null) return;
    _cacheKey = key;
    _render(settings, shop, paper);
  }

  Future<void> _render(
    PrinterSettings settings,
    ShopReceiptProfile shop,
    PrinterPaperSize paper,
  ) async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final printSettings = settings.copyWith(paperSize: paper);
      final logoPath = printSettings.logoUse ? shop.logoLocalPath : null;
      final bytes = await const ReceiptRasterizer().renderMessLayoutPng(
        widget.layout,
        settings: printSettings,
        logoPath: logoPath,
        useAssetLogoFallback: false,
      );
      if (!mounted) return;
      setState(() {
        png = bytes;
        loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        error = e;
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(printerSettingsProvider, (_, _) => _scheduleRender());
    ref.listen(shopReceiptProfileProvider, (_, _) => _scheduleRender());

    final settings = ref.watch(printerSettingsProvider);
    final shop = ref.watch(shopReceiptProfileProvider);
    final strings = AppStrings.of(ref);
    final paper = settings.paperSizeFor(isKot: false);
    final is3 = paper == PrinterPaperSize.inch3;
    final widthMm = is3 ? 72.0 : 48.0;
    final displayW = widthMm * 3.78;

    final details = [
      'Paper ${paper.dbValue}',
      if (settings.logoUse)
        shop.logoLocalPath.trim().isNotEmpty
            ? 'Logo ON'
            : 'Logo ON (no shop logo file)'
      else
        'Logo OFF',
      '${settings.charsPerLine} chars',
    ].join(' · ');

    Widget previewBody;
    if (loading && png == null) {
      previewBody = const Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: Center(child: CircularProgressIndicator()),
      );
    } else if (error != null && png == null) {
      previewBody = Padding(
        padding: const EdgeInsets.all(16),
        child: Text(
          'Preview failed: $error',
          textAlign: TextAlign.center,
          style: const TextStyle(color: AppColors.danger),
        ),
      );
    } else {
      previewBody = SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Image.memory(
          png!,
          width: displayW,
          fit: BoxFit.fitWidth,
          filterQuality: FilterQuality.none,
          gaplessPlayback: true,
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (widget.showStatusLine) ...[
          Text(
            details,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: AppColors.primary,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
        ],
        PreviewCard(
          title: is3 ? strings.paper3Inch : strings.paper2Inch,
          child: previewBody,
        ),
      ],
    );
  }
}

MessSlipLayout messCouponPreviewLayout({
  required ShopReceiptProfile profile,
  required String memberName,
  required String messType,
  required int couponNo,
}) {
  return MessSlipBuilder.couponLayout(
    profile: profile,
    memberName: memberName,
    messType: messType,
    couponNo: couponNo,
  );
}

MessSlipLayout messQrTokenPreviewLayout({
  required ShopReceiptProfile profile,
  required String memberName,
  required String memberMobile,
  required String messType,
  required String tokenCode,
  String? qrPayload,
}) {
  return MessSlipBuilder.qrTokenLayout(
    profile: profile,
    memberName: memberName,
    memberMobile: memberMobile,
    messType: messType,
    tokenCode: tokenCode,
    qrPayload: qrPayload,
  );
}
