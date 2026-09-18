import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';

/* Keep providers stable so thumbs do not flash on cart rebuilds. */
final Map<String, ImageProvider> productImageProviderCache = {};

/* Resolves productImage (data URL, http(s), or relative media path). */
ImageProvider? productImageProvider(String? value) {
  final raw = value?.trim() ?? '';
  if (raw.isEmpty) return null;

  final cached = productImageProviderCache[raw];
  if (cached != null) return cached;

  ImageProvider? provider;
  if (raw.startsWith('data:image')) {
    final comma = raw.indexOf(',');
    if (comma <= 0 || comma >= raw.length - 1) return null;
    try {
      final bytes = base64Decode(raw.substring(comma + 1));
      if (bytes.isEmpty) return null;
      provider = MemoryImage(bytes);
    } catch (_) {
      return null;
    }
  } else if (raw.startsWith('http://') || raw.startsWith('https://')) {
    provider = NetworkImage(raw);
  } else {
    final media = ApiConstants.mediaUrl(raw);
    if (media == null) return null;
    provider = NetworkImage(media);
  }

  productImageProviderCache[raw] = provider;
  return provider;
}

bool hasProductImage(String? value) => productImageProvider(value) != null;

/* Compact thumbnail — optional placeholder when [value] is empty/invalid. */
class ProductImageThumb extends StatelessWidget {
  const ProductImageThumb({
    super.key,
    required this.value,
    this.size = 48,
    this.radius = 10,
    this.showPlaceholder = false,
  });

  final String? value;
  final double size;
  final double radius;
  final bool showPlaceholder;

  @override
  Widget build(BuildContext context) {
    final provider = productImageProvider(value);
    if (provider == null) {
      if (!showPlaceholder) return const SizedBox.shrink();
      return Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: const Color(0xFFE8EEF7),
          borderRadius: BorderRadius.circular(radius),
        ),
        child: Icon(
          Icons.fastfood_rounded,
          size: size * 0.45,
          color: const Color(0xFF64748B),
        ),
      );
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: Image(
        image: provider,
        width: size,
        height: size,
        fit: BoxFit.cover,
        gaplessPlayback: true,
        filterQuality: FilterQuality.low,
        errorBuilder: (_, _, _) {
          if (!showPlaceholder) return const SizedBox.shrink();
          return Container(
            width: size,
            height: size,
            color: const Color(0xFFE8EEF7),
            alignment: Alignment.center,
            child: Icon(
              Icons.fastfood_rounded,
              size: size * 0.45,
              color: const Color(0xFF64748B),
            ),
          );
        },
      ),
    );
  }
}

Future<String?> encodePickedProductImage(XFile picked) async {
  try {
    final bytes = await picked.readAsBytes();
    if (bytes.isEmpty || bytes.length > 900000) return null;
    final b64 = base64Encode(bytes);
    final name = picked.name.toLowerCase();
    final path = picked.path.toLowerCase();
    final mime = (name.endsWith('.png') || path.endsWith('.png'))
        ? 'image/png'
        : (name.endsWith('.webp') || path.endsWith('.webp'))
        ? 'image/webp'
        : 'image/jpeg';
    return 'data:$mime;base64,$b64';
  } catch (_) {
    return null;
  }
}
