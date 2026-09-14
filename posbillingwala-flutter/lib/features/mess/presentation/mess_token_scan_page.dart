import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/l10n/app_strings.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';

/* Matches `activity_mess_token_scan.xml`: intro + Start QR scanner, then camera. */
class MessTokenScanPage extends ConsumerStatefulWidget {
  const MessTokenScanPage({super.key});

  @override
  ConsumerState<MessTokenScanPage> createState() => MessTokenScanPageState();
}

class MessTokenScanPageState extends ConsumerState<MessTokenScanPage> {
  MobileScannerController? messTokenScanPageController;
  bool scanning = false;
  bool busy = false;
  String? last;
  String? resultText;

  @override
  void dispose() {
    messTokenScanPageController?.dispose();
    super.dispose();
  }

  Future<void> startScanner() async {
    messTokenScanPageController?.dispose();
    messTokenScanPageController = MobileScannerController(
      detectionSpeed: DetectionSpeed.normal,
      facing: CameraFacing.back,
    );
    setState(() {
      scanning = true;
      resultText = null;
      last = null;
    });
  }

  void stopScanner() {
    messTokenScanPageController?.dispose();
    messTokenScanPageController = null;
    setState(() => scanning = false);
  }

  Future<void> messTokenScanPageOnDetect(BarcodeCapture capture) async {
    if (busy) return;
    final raw = capture.barcodes
        .map((b) => b.rawValue)
        .whereType<String>()
        .firstWhere((e) => e.trim().isNotEmpty, orElse: () => '');
    if (raw.isEmpty || raw == last) return;
    last = raw;
    setState(() => busy = true);
    try {
      final token =
          await ref.read(messControllerProvider.notifier).verifyRaw(raw);
      if (!mounted) return;
      if (token == null) {
        throw StateError('Token not found');
      }
      final label = 'Verified: ${token.memberName ?? token.tokenCode}';
      setState(() => resultText = label);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(label)));
      Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      setState(() => resultText = '$e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e')),
      );
      await Future<void>.delayed(const Duration(seconds: 2));
      last = null;
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (scanning && messTokenScanPageController != null) {
      return Scaffold(
        backgroundColor: Colors.black,
        body: Stack(
          fit: StackFit.expand,
          children: [
            MobileScanner(controller: messTokenScanPageController!, onDetect: messTokenScanPageOnDetect),
            const ScannerOverlay(),
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Material(
                          color: Colors.black54,
                          shape: const CircleBorder(),
                          child: IconButton(
                            tooltip: 'Close',
                            onPressed: stopScanner,
                            icon: const AppSvg(
                              AppAssets.svgClose,
                              width: 20,
                              height: 20,
                              color: Colors.white,
                            ),
                          ),
                        ),
                        const Spacer(),
                        Material(
                          color: Colors.black54,
                          shape: const CircleBorder(),
                          child: IconButton(
                            tooltip: 'Toggle torch',
                            onPressed: () => messTokenScanPageController?.toggleTorch(),
                            icon: const Icon(
                              Icons.flashlight_on_rounded,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const Spacer(),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.black.withValues(alpha: .65),
                        borderRadius: BorderRadius.circular(18),
                      ),
                      child: busy
                          ? const Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                ThreeDotsLoader(color: Colors.white),
                                SizedBox(width: 12),
                                Text(
                                  'Verifying…',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ],
                            )
                          : Text(
                              'Align the QR inside the frame',
                              textAlign: TextAlign.center,
                              style: AppTypography.body(color: Colors.white),
                            ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: Text(AppStrings.of(ref).scanMessToken)),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Scan the QR code on a mess token to verify',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 15),
            ),
            const SizedBox(height: 24),
            AppButton(
              label: 'Start QR scanner',
              onPressed: startScanner,
            ),
            if (resultText != null) ...[
              const SizedBox(height: 24),
              Text(
                resultText!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class ScannerOverlay extends StatelessWidget {
  const ScannerOverlay({super.key});

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: CornerFramePainter(),
      child: const SizedBox.expand(),
    );
  }
}

class CornerFramePainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final cut = Rect.fromCenter(
      center: Offset(size.width / 2, size.height * 0.42),
      width: size.width * 0.72,
      height: size.width * 0.72,
    );
    final overlay = Paint()..color = Colors.black.withValues(alpha: 0.45);
    final path = Path()
      ..addRect(Offset.zero & size)
      ..addRRect(RRect.fromRectAndRadius(cut, const Radius.circular(22)))
      ..fillType = PathFillType.evenOdd;
    canvas.drawPath(path, overlay);

    final stroke = Paint()
      ..color = AppColors.primary
      ..strokeWidth = 4
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;
    const len = 28.0;
    canvas.drawLine(cut.topLeft, cut.topLeft + const Offset(len, 0), stroke);
    canvas.drawLine(cut.topLeft, cut.topLeft + const Offset(0, len), stroke);
    canvas.drawLine(cut.topRight, cut.topRight + const Offset(-len, 0), stroke);
    canvas.drawLine(cut.topRight, cut.topRight + const Offset(0, len), stroke);
    canvas.drawLine(
        cut.bottomLeft, cut.bottomLeft + const Offset(len, 0), stroke);
    canvas.drawLine(
        cut.bottomLeft, cut.bottomLeft + const Offset(0, -len), stroke);
    canvas.drawLine(
        cut.bottomRight, cut.bottomRight + const Offset(-len, 0), stroke);
    canvas.drawLine(
        cut.bottomRight, cut.bottomRight + const Offset(0, -len), stroke);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
