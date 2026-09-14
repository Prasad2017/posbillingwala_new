import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

/* Loads SVG assets from `assets/images/svg/` with optional tint. */
class AppSvg extends StatelessWidget {
  const AppSvg(
    this.assetPath, {
    super.key,
    this.width,
    this.height,
    this.color,
    this.fit = BoxFit.contain,
    this.semanticsLabel,
  });

  final String assetPath;
  final double? width;
  final double? height;
  final Color? color;
  final BoxFit fit;
  final String? semanticsLabel;

  @override
  Widget build(BuildContext context) {
    return SvgPicture.asset(
      assetPath,
      width: width,
      height: height,
      fit: fit,
      semanticsLabel: semanticsLabel,
      colorFilter: color == null
          ? null
          : ColorFilter.mode(color!, BlendMode.srcIn),
      placeholderBuilder: (_) => SizedBox(
        width: width,
        height: height,
      ),
    );
  }
}

/* Circular / rounded icon chip using an SVG glyph. */
class AppSvgIconChip extends StatelessWidget {
  const AppSvgIconChip({
    super.key,
    required this.assetPath,
    required this.color,
    this.size = 44,
    this.iconSize,
    this.onTap,
  });

  final String assetPath;
  final Color color;
  final double size;
  final double? iconSize;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final glyph = iconSize ?? size * 0.46;
    final child = Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(size * 0.32),
        border: Border.all(color: color.withValues(alpha: 0.14)),
      ),
      child: AppSvg(assetPath, width: glyph, height: glyph, color: color),
    );
    if (onTap == null) return child;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(size * 0.32),
        child: child,
      ),
    );
  }
}
