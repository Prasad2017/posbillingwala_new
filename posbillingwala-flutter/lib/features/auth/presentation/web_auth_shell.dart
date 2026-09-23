import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/brand_logo.dart';
import 'package:url_launcher/url_launcher.dart';

/* Desktop auth chrome: navy brand panel + elevated form card. Mobile callers skip this. */
class WebAuthShell extends StatelessWidget {
  const WebAuthShell({
    super.key,
    required this.child,
    this.maxContentWidth = 460,
    this.topBar,
  });

  final Widget child;
  final double maxContentWidth;
  final Widget? topBar;

  static bool get enabled => AppPlatform.useDesktopShell;

  @override
  Widget build(BuildContext context) {
    final wide = context.widthClass.index >= AppWidthClass.largeTablet.index;
    return Scaffold(
      backgroundColor: const Color(0xFFEEF2F7),
      body: Row(
        children: [
          if (wide) const Expanded(flex: 11, child: WebAuthBrandPanel()),
          Expanded(
            flex: 9,
            child: SafeArea(
              child: Column(
                children: [
                  if (topBar != null)
                    Padding(
                      padding: const EdgeInsets.fromLTRB(28, 16, 28, 0),
                      child: Align(
                        alignment: Alignment.centerRight,
                        child: topBar!,
                      ),
                    ),
                  Expanded(
                    child: Center(
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 28,
                          vertical: 28,
                        ),
                        child: ConstrainedBox(
                          constraints: BoxConstraints(
                            maxWidth: maxContentWidth,
                          ),
                          child: child,
                        ),
                      ),
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
}

class WebAuthBrandPanel extends StatelessWidget {
  const WebAuthBrandPanel({super.key});

  static const _features = <_AuthFeature>[
    _AuthFeature(
      label: 'Fast Bill',
      icon: Icons.receipt_long_rounded,
      color: Color(0xFFFF8A1F),
    ),
    _AuthFeature(
      label: 'Table',
      icon: Icons.table_restaurant_rounded,
      color: Color(0xFF22C55E),
    ),
    _AuthFeature(
      label: 'Dine In',
      icon: Icons.restaurant_rounded,
      color: Color(0xFF3B82F6),
    ),
    _AuthFeature(
      label: 'Many More',
      icon: Icons.apps_rounded,
      color: Color(0xFF8B5CF6),
    ),
  ];

  Future<void> _open(Uri uri) async {
    await launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xFF062B73),
            Color(0xFF0756C9),
            Color(0xFF168BFF),
            Color(0xFFFF9D1A),
            Color(0xFFFF7800),
          ],
          stops: [0.0, 0.28, 0.52, 0.78, 1.0],
        ),
      ),
      child: Stack(
        children: [
          Positioned(
            top: -80,
            right: -40,
            child: _GlowOrb(
              size: 300,
              color: AppColors.orange.withValues(alpha: .28),
            ),
          ),
          Positioned(
            bottom: -50,
            left: -40,
            child: _GlowOrb(
              size: 260,
              color: AppColors.primaryBright.withValues(alpha: .22),
            ),
          ),
          Positioned(
            top: 160,
            left: 40,
            child: _GlowOrb(
              size: 160,
              color: Colors.white.withValues(alpha: .10),
            ),
          ),
          Positioned(
            bottom: 120,
            right: 20,
            child: _GlowOrb(
              size: 180,
              color: AppColors.yellow.withValues(alpha: .14),
            ),
          ),
          SafeArea(
            child: LayoutBuilder(
              builder: (context, constraints) {
                final compact = constraints.maxHeight < 760;
                return SingleChildScrollView(
                  padding: EdgeInsets.fromLTRB(
                    36,
                    compact ? 28 : 36,
                    36,
                    28,
                  ),
                  child: ConstrainedBox(
                    constraints: BoxConstraints(
                      minHeight: constraints.maxHeight - 56,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 12,
                                vertical: 8,
                              ),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(16),
                                boxShadow: [
                                  BoxShadow(
                                    color: Colors.black.withValues(alpha: .12),
                                    blurRadius: 16,
                                    offset: const Offset(0, 6),
                                  ),
                                ],
                              ),
                              child: Image.asset(
                                AppAssets.appLogo,
                                width: constraints.maxWidth > 560 ? 160 : 130,
                                fit: BoxFit.contain,
                                filterQuality: FilterQuality.high,
                                errorBuilder: (_, _, _) => BrandLogo(
                                  width: constraints.maxWidth > 560
                                      ? 160
                                      : 130,
                                ),
                              ),
                            ),
                            const SizedBox(width: 18),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'Simple Fast Powerful',
                                    style: TextStyle(
                                      fontFamily: AppFonts.family,
                                      fontStyle: FontStyle.italic,
                                      color: AppColors.yellow,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 13,
                                      letterSpacing: 0.2,
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  Text.rich(
                                    TextSpan(
                                      style: TextStyle(
                                        fontFamily: AppFonts.family,
                                        color: Colors.white,
                                        fontWeight: FontWeight.w800,
                                        fontSize: compact ? 24 : 30,
                                        height: 1.15,
                                        letterSpacing: -0.5,
                                      ),
                                      children: const [
                                        TextSpan(text: 'Manage Your '),
                                        TextSpan(
                                          text: 'Business',
                                          style: TextStyle(
                                            color: AppColors.orange,
                                          ),
                                        ),
                                        TextSpan(text: ' Easily'),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    'Bill, manage catalog, and view reports from the browser. '
                                    'Keep the Android or iOS app for offline billing.',
                                    style: TextStyle(
                                      fontFamily: AppFonts.family,
                                      color: Colors.white.withValues(
                                        alpha: .78,
                                      ),
                                      fontWeight: FontWeight.w400,
                                      fontSize: 13.5,
                                      height: 1.45,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        SizedBox(height: compact ? 22 : 28),
                        Wrap(
                          spacing: 10,
                          runSpacing: 10,
                          children: [
                            for (final feature in _features)
                              _FeatureChip(feature: feature),
                          ],
                        ),
                        SizedBox(height: compact ? 24 : 32),
                        const _DeviceMockupCluster(),
                        SizedBox(height: compact ? 22 : 28),
                        Wrap(
                          spacing: 12,
                          runSpacing: 10,
                          children: [
                            _StoreBadge(
                              onTap: () => _open(
                                Uri.parse(AppConstants.playStoreUrl),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Icon(
                                    Icons.play_arrow_rounded,
                                    color: Colors.white,
                                    size: 28,
                                  ),
                                  const SizedBox(width: 8),
                                  Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Text(
                                        'GET IT ON',
                                        style: TextStyle(
                                          fontFamily: AppFonts.family,
                                          color: Colors.white.withValues(
                                            alpha: .85,
                                          ),
                                          fontSize: 9,
                                          fontWeight: FontWeight.w500,
                                          letterSpacing: 0.6,
                                        ),
                                      ),
                                      const Text(
                                        'Google Play',
                                        style: TextStyle(
                                          fontFamily: AppFonts.family,
                                          color: Colors.white,
                                          fontSize: 15,
                                          fontWeight: FontWeight.w700,
                                          height: 1.1,
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                            _StoreBadge(
                              onTap: () => _open(
                                Uri.parse(AppConstants.website),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Icon(
                                    Icons.apple,
                                    color: Colors.white,
                                    size: 26,
                                  ),
                                  const SizedBox(width: 8),
                                  Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Text(
                                        'Download on the',
                                        style: TextStyle(
                                          fontFamily: AppFonts.family,
                                          color: Colors.white.withValues(
                                            alpha: .85,
                                          ),
                                          fontSize: 9,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                      const Text(
                                        'App Store',
                                        style: TextStyle(
                                          fontFamily: AppFonts.family,
                                          color: Colors.white,
                                          fontSize: 15,
                                          fontWeight: FontWeight.w700,
                                          height: 1.1,
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 28),
                        Text(
                          AppConstants.websiteDisplay,
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            color: Colors.white.withValues(alpha: .65),
                            fontWeight: FontWeight.w500,
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _AuthFeature {
  const _AuthFeature({
    required this.label,
    required this.icon,
    required this.color,
  });

  final String label;
  final IconData icon;
  final Color color;
}

class _FeatureChip extends StatelessWidget {
  const _FeatureChip({required this.feature});

  final _AuthFeature feature;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 148,
      padding: const EdgeInsets.fromLTRB(10, 10, 12, 10),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: .10),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: .14)),
      ),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: feature.color,
              borderRadius: BorderRadius.circular(10),
              boxShadow: [
                BoxShadow(
                  color: feature.color.withValues(alpha: .35),
                  blurRadius: 8,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: Icon(feature.icon, color: Colors.white, size: 18),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              feature.label,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                color: Colors.white,
                fontWeight: FontWeight.w600,
                fontSize: 12,
                height: 1.25,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _StoreBadge extends StatelessWidget {
  const _StoreBadge({required this.onTap, required this.child});

  final VoidCallback onTap;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.black.withValues(alpha: .55),
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: Colors.white.withValues(alpha: .28)),
          ),
          child: child,
        ),
      ),
    );
  }
}

class _GlowOrb extends StatelessWidget {
  const _GlowOrb({required this.size, required this.color});

  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(shape: BoxShape.circle, color: color),
    );
  }
}

/* Stylized device cluster — monitor + tablet + receipt printer. */
class _DeviceMockupCluster extends StatelessWidget {
  const _DeviceMockupCluster();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 168,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned(
            left: 0,
            bottom: 8,
            child: Transform.rotate(
              angle: -0.08,
              child: const _TabletMockup(),
            ),
          ),
          const Positioned(left: 72, bottom: 0, child: _MonitorMockup()),
          Positioned(
            right: 8,
            bottom: 4,
            child: Transform.rotate(
              angle: 0.06,
              child: const _PrinterMockup(),
            ),
          ),
        ],
      ),
    );
  }
}

class _MonitorMockup extends StatelessWidget {
  const _MonitorMockup();

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 210,
          height: 128,
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: const Color(0xFF1A2332),
            borderRadius: BorderRadius.circular(10),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: .35),
                blurRadius: 18,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFFF5F8FC),
              borderRadius: BorderRadius.circular(6),
            ),
            child: Row(
              children: [
                Container(
                  width: 36,
                  decoration: const BoxDecoration(
                    color: Color(0xFF0B3A7A),
                    borderRadius: BorderRadius.only(
                      topLeft: Radius.circular(6),
                      bottomLeft: Radius.circular(6),
                    ),
                  ),
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Column(
                    children: [
                      for (var i = 0; i < 4; i++) ...[
                        if (i > 0) const SizedBox(height: 6),
                        Container(
                          width: 18,
                          height: 4,
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(
                              alpha: i == 0 ? .9 : .35,
                            ),
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.all(8),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Row(
                          children: [
                            for (final color in [
                              const Color(0xFF3B82F6),
                              const Color(0xFF22C55E),
                              const Color(0xFFF59E0B),
                            ]) ...[
                              Expanded(
                                child: Container(
                                  height: 28,
                                  margin: const EdgeInsets.only(right: 4),
                                  decoration: BoxDecoration(
                                    color: color.withValues(alpha: .18),
                                    borderRadius: BorderRadius.circular(5),
                                    border: Border.all(
                                      color: color.withValues(alpha: .35),
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                        const SizedBox(height: 8),
                        Expanded(
                          child: Row(
                            children: [
                              Expanded(
                                flex: 3,
                                child: Container(
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFE8EEF7),
                                    borderRadius: BorderRadius.circular(5),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 6),
                              Expanded(
                                flex: 2,
                                child: Container(
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFDCE8F8),
                                    borderRadius: BorderRadius.circular(5),
                                  ),
                                  child: Center(
                                    child: Icon(
                                      Icons.pie_chart_rounded,
                                      size: 28,
                                      color: AppColors.primary.withValues(
                                        alpha: .55,
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        Container(
          width: 48,
          height: 8,
          color: const Color(0xFF2A3444),
        ),
        Container(
          width: 72,
          height: 5,
          decoration: BoxDecoration(
            color: const Color(0xFF1A2332),
            borderRadius: BorderRadius.circular(2),
          ),
        ),
      ],
    );
  }
}

class _TabletMockup extends StatelessWidget {
  const _TabletMockup();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 92,
      height: 118,
      padding: const EdgeInsets.all(5),
      decoration: BoxDecoration(
        color: const Color(0xFF1A2332),
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: .3),
            blurRadius: 12,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
        ),
        padding: const EdgeInsets.all(6),
        child: Column(
          children: [
            Container(
              height: 10,
              decoration: BoxDecoration(
                color: AppColors.primarySoft,
                borderRadius: BorderRadius.circular(3),
              ),
            ),
            const SizedBox(height: 6),
            Expanded(
              child: GridView.count(
                crossAxisCount: 2,
                mainAxisSpacing: 4,
                crossAxisSpacing: 4,
                physics: const NeverScrollableScrollPhysics(),
                children: [
                  for (final color in [
                    const Color(0xFFFFE4C4),
                    const Color(0xFFDCFCE7),
                    const Color(0xFFE0E7FF),
                    const Color(0xFFFCE7F3),
                  ])
                    Container(
                      decoration: BoxDecoration(
                        color: color,
                        borderRadius: BorderRadius.circular(4),
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PrinterMockup extends StatelessWidget {
  const _PrinterMockup();

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 78,
          padding: const EdgeInsets.fromLTRB(8, 10, 8, 8),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(4),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: .25),
                blurRadius: 10,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'BILLINGWALA',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontSize: 7,
                  fontWeight: FontWeight.w800,
                  color: AppColors.navy,
                  letterSpacing: 0.4,
                ),
              ),
              const SizedBox(height: 4),
              for (var i = 0; i < 4; i++) ...[
                Container(
                  height: 3,
                  margin: EdgeInsets.only(
                    bottom: 3,
                    right: i.isEven ? 8 : 0,
                  ),
                  color: const Color(0xFFE5E7EB),
                ),
              ],
              const SizedBox(height: 2),
              Container(
                height: 22,
                decoration: BoxDecoration(
                  color: const Color(0xFFF3F4F6),
                  borderRadius: BorderRadius.circular(2),
                ),
                child: const Icon(
                  Icons.qr_code_2_rounded,
                  size: 16,
                  color: Color(0xFF6B7280),
                ),
              ),
            ],
          ),
        ),
        Container(
          width: 88,
          height: 28,
          decoration: BoxDecoration(
            color: const Color(0xFF374151),
            borderRadius: BorderRadius.circular(6),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: .3),
                blurRadius: 8,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Center(
            child: Container(
              width: 18,
              height: 6,
              decoration: BoxDecoration(
                color: const Color(0xFF9CA3AF),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
