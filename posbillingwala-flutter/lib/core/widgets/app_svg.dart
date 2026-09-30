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
      placeholderBuilder: (_) => SizedBox(width: width, height: height),
    );
  }
}
