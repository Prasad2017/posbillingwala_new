import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_constants.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgets/brand_logo.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/ads/ad_banner.dart';
import 'package:pos_billingwala_v2/features/ads/ad_config.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

class AboutPage extends StatelessWidget {
  const AboutPage({super.key});

  static const aboutPageTagline =
      'POS BillingWala is your complete billing solution to manage sales, '
      'inventory, customers and reports easily. Simple, Fast and Reliable – '
      'All in One App.';

  static const disclaimer =
      "This Software is provided 'is-as', without any express or implied "
      'warranty. In no event will the authors be held liable for any damages '
      'arising from the use of this software.';

  Future<void> openExternal(BuildContext context, Uri uri) async {
    final ok = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!ok && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not open link')),
      );
    }
  }

  Future<void> rateUs(BuildContext context) {
    return openExternal(context, Uri.parse(AppConstants.playStoreUrl));
  }

  Future<void> openWebsite(BuildContext context) {
    return openExternal(context, Uri.parse(AppConstants.website));
  }

  Future<void> openEmail(BuildContext context) {
    return openExternal(
      context,
      Uri(scheme: 'mailto', path: AppConstants.supportEmail),
    );
  }

  Future<void> openPhone(BuildContext context) => callSupport(context);

  @override
  Widget build(BuildContext context) {
    final phone = formatSupportPhone(AppConstants.supportPhone);

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text('About Us'),
        leadingWidth: 64,
        leading: Padding(
          padding: const EdgeInsets.only(left: 12),
          child: Center(
            child: Material(
              color: Colors.transparent,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: () => context.pop(),
                child: Container(
                  width: 40,
                  height: 40,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: Colors.white.withValues(alpha: .55),
                      width: 1.4,
                    ),
                  ),
                  child: const Icon(
                    Icons.arrow_back_rounded,
                    color: Colors.white,
                    size: 20,
                  ),
                ),
              ),
            ),
          ),
        ),
        actions: [
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert_rounded, color: Colors.white),
            onSelected: (value) {
              if (value == 'rate') rateUs(context);
            },
            itemBuilder: (context) => const [
              PopupMenuItem(value: 'rate', child: Text('Rate us')),
            ],
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            28),
        children: [
          HeroCard(tagline: aboutPageTagline),
          const SizedBox(height: 22),
          const SectionTitle('Get in touch'),
          const SizedBox(height: 12),
          AppCard(
            accentColor: AppColors.primary,
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                ContactTile(
                  iconAsset: AppAssets.svgAboutWeb,
                  label: 'Website',
                  value: AppConstants.websiteDisplay,
                  onTap: () => openWebsite(context),
                ),
                const Divider(height: 1, indent: 72, endIndent: 16),
                ContactTile(
                  iconAsset: AppAssets.svgAboutEmail,
                  label: 'Email',
                  value: AppConstants.supportEmail,
                  onTap: () => openEmail(context),
                ),
                const Divider(height: 1, indent: 72, endIndent: 16),
                ContactTile(
                  iconAsset: AppAssets.svgAboutPhone,
                  label: 'Phone',
                  value: phone,
                  onTap: () => openPhone(context),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          const DisclaimerCard(text: disclaimer),
          const SizedBox(height: 14),
          DeveloperCard(phone: phone, onCall: () => openPhone(context)),
          const SizedBox(height: 14),
          RateUsCard(onRate: () => rateUs(context)),
          const SizedBox(height: 16),
          const Center(child: AdBanner(slot: AdSlot.about)),
        ],
      ),
      ),
    );
  }
}

class SectionTitle extends StatelessWidget {
  const SectionTitle(this.title, {super.key});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: AppTypography.sectionTitle(color: AppColors.primaryDark),
        ),
        const SizedBox(height: 6),
        Container(
          width: 36,
          height: 3.5,
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(99),
          ),
        ),
      ],
    );
  }
}

class HeroCard extends StatelessWidget {
  const HeroCard({super.key, required this.tagline});

  final String tagline;

  static const aboutPageFeatures = <FeatureItem>[
    FeatureItem(
      label: 'Fast Billing',
      icon: Icons.shopping_cart_rounded,
      color: AppColors.primary,
      background: Color(0xFFEFF6FF),
    ),
    FeatureItem(
      label: 'Inventory Management',
      icon: Icons.inventory_2_rounded,
      color: AppColors.green,
      background: Color(0xFFECFDF5),
    ),
    FeatureItem(
      label: 'Customer Management',
      icon: Icons.person_rounded,
      color: AppColors.orange,
      background: Color(0xFFFFF7ED),
    ),
    FeatureItem(
      label: 'Powerful Reports',
      icon: Icons.grid_view_rounded,
      color: AppColors.purple,
      background: Color(0xFFF5F3FF),
    ),
    FeatureItem(
      label: 'Multi Language Print Support',
      icon: Icons.print_rounded,
      color: AppColors.red,
      background: Color(0xFFFDF2F8),
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return AppCard(
      accentColor: AppColors.primary,
      padding: const EdgeInsets.fromLTRB(18, 22, 18, 18),
      child: Column(
        children: [
          const BrandLogo(width: 148),
          const SizedBox(height: 14),
          Text(
            AppConstants.appName,
            textAlign: TextAlign.center,
            style: AppTypography.screenTitle(color: AppColors.primaryDark),
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 5),
            decoration: BoxDecoration(
              color: AppColors.primary,
              borderRadius: BorderRadius.circular(99),
            ),
            child: Text(
              AppConstants.appVersionBadge,
              style: const TextStyle(
                fontFamily: AppFonts.family,
                color: Colors.white,
                fontWeight: FontWeight.w700,
                fontSize: 12,
                letterSpacing: 0.2,
              ),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            'Thank You For Choosing Us!',
            textAlign: TextAlign.center,
            style: AppTypography.cardTitle(color: AppColors.primary),
          ),
          const SizedBox(height: 16),
          const DiamondDivider(),
          const SizedBox(height: 14),
          Text(
            tagline,
            textAlign: TextAlign.center,
            style: AppTypography.bodySmall(color: AppColors.textSecondary),
          ),
          const SizedBox(height: 18),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final feature in aboutPageFeatures)
                Expanded(child: FeatureTile(item: feature)),
            ],
          ),
        ],
      ),
    );
  }
}

class FeatureItem {
  const FeatureItem({
    required this.label,
    required this.icon,
    required this.color,
    required this.background,
  });

  final String label;
  final IconData icon;
  final Color color;
  final Color background;
}

class FeatureTile extends StatelessWidget {
  const FeatureTile({super.key, required this.item});

  final FeatureItem item;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 2),
      child: Column(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: item.background,
              borderRadius: BorderRadius.circular(14),
            ),
            alignment: Alignment.center,
            child: Icon(item.icon, color: item.color, size: 22),
          ),
          const SizedBox(height: 6),
          Text(
            item.label,
            textAlign: TextAlign.center,
            maxLines: 3,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontSize: 9.5,
              height: 1.2,
              fontWeight: FontWeight.w600,
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}

class DiamondDivider extends StatelessWidget {
  const DiamondDivider({super.key});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(child: Divider(color: AppColors.border.withValues(alpha: .9))),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          child: Transform.rotate(
            angle: 0.785398,
            child: Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                color: AppColors.yellow,
                borderRadius: BorderRadius.circular(2),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.yellow.withValues(alpha: .45),
                    blurRadius: 4,
                  ),
                ],
              ),
            ),
          ),
        ),
        Expanded(child: Divider(color: AppColors.border.withValues(alpha: .9))),
      ],
    );
  }
}

class ContactTile extends StatelessWidget {
  const ContactTile({super.key, 
    required this.iconAsset,
    required this.label,
    required this.value,
    required this.onTap,
  });

  final String iconAsset;
  final String label;
  final String value;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 14, 12, 14),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: const BoxDecoration(
                color: AppColors.primaryLight,
                shape: BoxShape.circle,
              ),
              alignment: Alignment.center,
              child: AppSvg(iconAsset, width: 22, height: 22),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: AppTypography.bodySmall(
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    value,
                    style: AppTypography.cardTitle(color: AppColors.primary),
                  ),
                ],
              ),
            ),
            Icon(
              Icons.chevron_right_rounded,
              color: AppColors.textSecondary.withValues(alpha: .7),
            ),
          ],
        ),
      ),
    );
  }
}

class DisclaimerCard extends StatelessWidget {
  const DisclaimerCard({super.key, required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 16, 14),
      decoration: BoxDecoration(
        color: const Color(0xFFE8F0FE),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFD6E4FF)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: .7),
              shape: BoxShape.circle,
            ),
            alignment: Alignment.center,
            child: const Icon(
              Icons.verified_user_rounded,
              color: AppColors.primary,
              size: 22,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: AppTypography.bodySmall(color: AppColors.textSecondary),
            ),
          ),
        ],
      ),
    );
  }
}

class DeveloperCard extends StatelessWidget {
  const DeveloperCard({super.key, required this.phone, required this.onCall});

  final String phone;
  final VoidCallback onCall;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      accentColor: AppColors.primary,
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: const BoxDecoration(
              color: Color(0xFFE8ECF8),
              shape: BoxShape.circle,
            ),
            alignment: Alignment.center,
            child: const Icon(
              Icons.apartment_rounded,
              color: AppColors.primary,
              size: 24,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Developed by',
                  style: AppTypography.bodySmall(
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  AppConstants.developerName,
                  style: AppTypography.cardTitle(color: AppColors.primaryDark),
                ),
                const SizedBox(height: 8),
                InkWell(
                  onTap: onCall,
                  borderRadius: BorderRadius.circular(8),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.headset_mic_rounded,
                        size: 16,
                        color: AppColors.primary,
                      ),
                      const SizedBox(width: 6),
                      Flexible(
                        child: Text(
                          'Technical Support: $phone',
                          style: AppTypography.bodySmall(
                            color: AppColors.primary,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class RateUsCard extends StatelessWidget {
  const RateUsCard({super.key, required this.onRate});

  final VoidCallback onRate;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      accentColor: AppColors.primary,
      onTap: onRate,
      padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Love the app? Rate us on Google Play',
                  style: AppTypography.cardTitle(color: AppColors.primaryDark),
                ),
                const SizedBox(height: 8),
                Row(
                  children: List.generate(
                    5,
                    (index) => const Padding(
                      padding: EdgeInsets.only(right: 2),
                      child: Icon(
                        Icons.star_rounded,
                        color: AppColors.yellow,
                        size: 20,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          const GooglePlayBadge(),
        ],
      ),
    );
  }
}

class GooglePlayBadge extends StatelessWidget {
  const GooglePlayBadge({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 112,
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF111111),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          const AppSvg(
            AppAssets.svgPlayStore,
            width: 18,
            height: 18,
            color: Colors.white,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: const [
                Text(
                  'GET IT ON',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    color: Colors.white,
                    fontSize: 7.5,
                    fontWeight: FontWeight.w500,
                    height: 1,
                    letterSpacing: 0.4,
                  ),
                ),
                SizedBox(height: 2),
                Text(
                  'Google Play',
                  style: TextStyle(
                    fontFamily: AppFonts.family,
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    height: 1.1,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
