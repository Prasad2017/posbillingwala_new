import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/theme/billingwala_theme.dart';

/* Soft multi-blob aurora wash — sits behind every route. */
class AuroraBackground extends StatelessWidget {
  const AuroraBackground({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = BillingwalaTheme.of(context);
    final blobs = theme.auroraBlobColors;

    return Stack(
      fit: StackFit.expand,
      children: [
        DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: theme.auroraColors,
              stops: const [0.0, 0.35, 0.7, 1.0],
            ),
          ),
        ),
        IgnorePointer(
          child: CustomPaint(
            painter: _AuroraBlobPainter(blobs),
            size: Size.infinite,
          ),
        ),
        child,
      ],
    );
  }
}

class _AuroraBlobPainter extends CustomPainter {
  _AuroraBlobPainter(this.colors);

  final List<Color> colors;

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty || colors.isEmpty) return;

    final paints = [
      _blob(colors[0 % colors.length], 0.42),
      _blob(colors[1 % colors.length], 0.38),
      _blob(colors[2 % colors.length], 0.34),
      _blob(colors[3 % colors.length], 0.30),
    ];

    final centers = [
      Offset(size.width * 0.12, size.height * 0.08),
      Offset(size.width * 0.92, size.height * 0.18),
      Offset(size.width * 0.78, size.height * 0.72),
      Offset(size.width * 0.08, size.height * 0.78),
    ];

    final radii = [
      size.shortestSide * 0.55,
      size.shortestSide * 0.48,
      size.shortestSide * 0.52,
      size.shortestSide * 0.45,
    ];

    for (var i = 0; i < paints.length; i++) {
      canvas.drawCircle(centers[i], radii[i], paints[i]);
    }
  }

  Paint _blob(Color color, double opacity) {
    return Paint()
      ..color = color.withValues(alpha: opacity)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 72);
  }

  @override
  bool shouldRepaint(covariant _AuroraBlobPainter oldDelegate) {
    return oldDelegate.colors != colors;
  }
}
