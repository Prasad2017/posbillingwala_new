import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';

class AppModuleIcon extends StatelessWidget {
  const AppModuleIcon({
    super.key,
    this.icon,
    this.assetPath,
    this.svgPath,
    required this.color,
    this.size = 52,
  }) : assert(icon != null || assetPath != null || svgPath != null);

  final IconData? icon;
  final String? assetPath;
  final String? svgPath;
  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: .92, end: 1),
      duration: const Duration(milliseconds: 240),
      curve: Curves.easeOutBack,
      builder: (context, value, child) =>
          Transform.scale(scale: value, child: child),
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(size * 0.34),
          border: Border.all(color: color.withValues(alpha: 0.12)),
        ),
        clipBehavior: Clip.antiAlias,
        child: svgPath != null
            ? Padding(
                padding: EdgeInsets.all(size * 0.2),
                child: AppSvg(
                  svgPath!,
                  color: color,
                  fit: BoxFit.contain,
                ),
              )
            : assetPath != null
                ? Padding(
                    padding: EdgeInsets.all(size * 0.18),
                    child: Image.asset(
                      assetPath!,
                      fit: BoxFit.contain,
                      errorBuilder: (context, error, stackTrace) => Icon(
                        icon ?? Icons.image_outlined,
                        color: color,
                        size: size * 0.48,
                      ),
                    ),
                  )
                : Icon(icon, color: color, size: size * 0.48),
      ),
    );
  }
}

/* Tiny helper for inline asset images with Poppins-safe fallbacks. */
class AppAssetImage extends StatelessWidget {
  const AppAssetImage(
    this.path, {
    super.key,
    this.width,
    this.height,
    this.fit = BoxFit.contain,
  });

  final String path;
  final double? width;
  final double? height;
  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      path,
      width: width,
      height: height,
      fit: fit,
      errorBuilder: (context, error, stackTrace) => SizedBox(
        width: width,
        height: height,
        child: const Center(
          child: Text(
            '•',
            style: TextStyle(fontFamily: AppFonts.family),
          ),
        ),
      ),
    );
  }
}
