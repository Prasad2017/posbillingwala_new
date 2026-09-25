import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:share_plus/share_plus.dart';

class ShareAppPage extends StatelessWidget {
  const ShareAppPage({super.key});

  static const shareAppPagePlayStoreLink =
      'https://play.google.com/store/apps/details?id=com.pos_billingwala';

  Future<void> shareAppPageShare(BuildContext context) async {
    final box = context.findRenderObject() as RenderBox?;
    final text =
        'Try ${AppConstants.appName} — smart POS billing for shops & restaurants.\n'
        'Download: $shareAppPagePlayStoreLink\n'
        'Learn more: ${AppConstants.website}';
    await SharePlus.instance.share(
      ShareParams(
        text: text,
        subject: AppConstants.appName,
        sharePositionOrigin: box == null
            ? null
            : box.localToGlobal(Offset.zero) & box.size,
      ),
    );
  }

  Future<void> copyLink(BuildContext context) async {
    await Clipboard.setData(
      const ClipboardData(text: shareAppPagePlayStoreLink),
    );
    if (!context.mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Play Store link copied')));
  }

  void showQr(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: Text('Invite with QR Code', style: AppTypography.sectionTitle()),
        content: SizedBox(
          width: 260,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              QrPreview(size: 220),
              const SizedBox(height: 12),
              Text(
                'Scan to download the app instantly.',
                textAlign: TextAlign.center,
                style: AppTypography.bodySmall(),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.paddingOf(context).top;

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Column(
        children: [
          ShareHeader(topInset: topInset, onBack: () => context.pop()),
          Expanded(
            child: ResponsiveScrollShell(
              dashboard: true,
              child: ListView(
                padding: EdgeInsets.fromLTRB(
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  4,
                  AppBreakpoints.pagePaddingFor(context.widthClass),
                  28,
                ),
                children: [
                  const LoveCard(),
                  const SizedBox(height: 14),
                  ShareLinkCard(
                    playStoreLink: shareAppPagePlayStoreLink,
                    onCopy: () => copyLink(context),
                    onShare: () => shareAppPageShare(context),
                  ),
                  const SizedBox(height: 14),
                  QrInviteCard(onShowQr: () => showQr(context)),
                  const SizedBox(height: 14),
                  const WhyShareCard(),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class ShareHeader extends StatelessWidget {
  const ShareHeader({super.key, required this.topInset, required this.onBack});

  final double topInset;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(12, topInset + 8, 16, 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Material(
            color: Colors.white,
            shape: const CircleBorder(),
            elevation: 0,
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: onBack,
              child: Container(
                width: 44,
                height: 44,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: AppColors.border),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.navy.withValues(alpha: .04),
                      blurRadius: 10,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: const Icon(
                  Icons.arrow_back_rounded,
                  color: AppColors.primaryDark,
                  size: 22,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Share App',
                    style: TextStyle(
                      fontFamily: AppFonts.family,
                      color: AppColors.primaryDark,
                      fontWeight: FontWeight.w800,
                      fontSize: 26,
                      height: 1.15,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Invite your friends and grow together',
                    style: AppTypography.bodySmall(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 8),
          const Padding(
            padding: EdgeInsets.only(top: 2),
            child: BrandLogo(width: 72),
          ),
        ],
      ),
    );
  }
}

class LoveCard extends StatelessWidget {
  const LoveCard({super.key});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      accentColor: AppColors.primary,
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Share the app you love 💙',
            style: AppTypography.sectionTitle(color: AppColors.primaryDark),
          ),
          const SizedBox(height: 8),
          Text(
            'Help your friends run their business smarter with ${AppConstants.appName}.',
            style: AppTypography.body(color: AppColors.textSecondary),
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: AppColors.primaryLight,
              borderRadius: BorderRadius.circular(999),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const AppSvg(
                  AppAssets.svgSharePeople,
                  width: 18,
                  height: 18,
                  color: AppColors.primary,
                ),
                const SizedBox(width: 8),
                Text(
                  'More Users, More Growth',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    color: AppColors.primaryDark,
                    fontWeight: FontWeight.w700,
                    fontSize: 13,
                  ),
                ),
                const SizedBox(width: 6),
                Icon(
                  Icons.north_east_rounded,
                  size: 16,
                  color: AppColors.primary.withValues(alpha: .9),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class ShareLinkCard extends StatelessWidget {
  const ShareLinkCard({
    super.key,
    required this.playStoreLink,
    required this.onCopy,
    required this.onShare,
  });

  final String playStoreLink;
  final VoidCallback onCopy;
  final VoidCallback onShare;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      accentColor: AppColors.primary,
      padding: const EdgeInsets.fromLTRB(18, 16, 14, 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Share App Link',
                  style: AppTypography.sectionTitle(),
                ),
              ),
              Material(
                color: AppColors.primaryLight,
                shape: const CircleBorder(),
                child: InkWell(
                  customBorder: const CircleBorder(),
                  onTap: onCopy,
                  child: const SizedBox(
                    width: 42,
                    height: 42,
                    child: Center(
                      child: AppSvg(
                        AppAssets.svgCopy,
                        width: 18,
                        height: 18,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            'Tap the copy icon above to copy the link anytime.',
            style: AppTypography.bodySmall(),
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              color: const Color(0xFFF0F4FA),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.border),
            ),
            child: Text(
              playStoreLink,
              style: AppTypography.bodySmall(color: AppColors.primaryDark),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
            decoration: BoxDecoration(
              color: AppColors.primaryLight,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Text(
              'Know someone who runs a shop? Share ${AppConstants.appName} with them — it\'s 100% free and helps them bill faster! 🚀',
              style: AppTypography.bodySmall(color: AppColors.primaryDark),
            ),
          ),
          const SizedBox(height: 14),
          AppButton(
            label: 'SHARE NOW',
            icon: Icons.share_rounded,
            onPressed: onShare,
          ),
        ],
      ),
    );
  }
}

class QrInviteCard extends StatelessWidget {
  const QrInviteCard({super.key, required this.onShowQr});

  final VoidCallback onShowQr;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      accentColor: AppColors.primary,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const QrPreview(size: 96),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Invite with QR Code',
                      style: AppTypography.sectionTitle(),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Scan to download the app instantly.',
                      style: AppTypography.bodySmall(),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: onShowQr,
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.green,
                side: const BorderSide(color: AppColors.green, width: 1.4),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: Text(
                'SHOW QR CODE',
                style: TextStyle(
                  fontFamily: AppFonts.family,
                  fontWeight: FontWeight.w800,
                  fontSize: 14,
                  letterSpacing: 0.3,
                  color: AppColors.green,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class QrPreview extends StatelessWidget {
  const QrPreview({super.key, required this.size});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      padding: EdgeInsets.all(size * 0.06),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border),
        boxShadow: [
          BoxShadow(
            color: AppColors.navy.withValues(alpha: .05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          QrImageView(
            data: ShareAppPage.shareAppPagePlayStoreLink,
            size: size * 0.88,
            backgroundColor: Colors.white,
            eyeStyle: const QrEyeStyle(
              eyeShape: QrEyeShape.square,
              color: AppColors.primaryDark,
            ),
            dataModuleStyle: const QrDataModuleStyle(
              dataModuleShape: QrDataModuleShape.square,
              color: AppColors.primary,
            ),
          ),
          Container(
            width: size * 0.26,
            height: size * 0.26,
            padding: EdgeInsets.all(size * 0.028),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(size * 0.045),
              boxShadow: [
                BoxShadow(
                  color: AppColors.navy.withValues(alpha: .08),
                  blurRadius: 6,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(size * 0.028),
              child: Image.asset(AppAssets.appLogo, fit: BoxFit.contain),
            ),
          ),
        ],
      ),
    );
  }
}

class WhyShareCard extends StatelessWidget {
  const WhyShareCard({super.key});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      accentColor: AppColors.primary,
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Why share ${AppConstants.appName}?',
            style: AppTypography.sectionTitle(),
          ),
          const SizedBox(height: 16),
          const Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: BenefitItem(
                  svgPath: AppAssets.svgShareBenefitGrow,
                  bgColor: Color(0xFFE8F1FF),
                  title: 'Grow Together',
                  subtitle: 'Help friends grow their business.',
                ),
              ),
              SizedBox(width: 8),
              Expanded(
                child: BenefitItem(
                  svgPath: AppAssets.svgShareBenefitTime,
                  bgColor: Color(0xFFFFF6E5),
                  title: 'Save Time',
                  subtitle: 'Manage business in less time.',
                ),
              ),
              SizedBox(width: 8),
              Expanded(
                child: BenefitItem(
                  svgPath: AppAssets.svgShareBenefitBilling,
                  bgColor: Color(0xFFE8F8EF),
                  title: 'Easy Billing',
                  subtitle: 'Simple, fast & accurate billing.',
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class BenefitItem extends StatelessWidget {
  const BenefitItem({
    super.key,
    required this.svgPath,
    required this.bgColor,
    required this.title,
    required this.subtitle,
  });

  final String svgPath;
  final Color bgColor;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 52,
          height: 52,
          alignment: Alignment.center,
          decoration: BoxDecoration(color: bgColor, shape: BoxShape.circle),
          child: AppSvg(svgPath, width: 28, height: 28),
        ),
        const SizedBox(height: 10),
        Text(
          title,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: AppFonts.family,
            fontWeight: FontWeight.w800,
            fontSize: 12.5,
            color: AppColors.navy,
            height: 1.2,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          subtitle,
          textAlign: TextAlign.center,
          style: AppTypography.caption(),
        ),
      ],
    );
  }
}
