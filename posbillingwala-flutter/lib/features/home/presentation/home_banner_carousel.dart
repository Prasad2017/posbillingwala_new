import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/home/data/home_banner_api.dart';
import 'package:pos_billingwala_v2/features/home/domain/home_banner_providers.dart';

/* Admin-uploaded home banners (online only). Multiple → auto-scroll + dots. */
class HomeBannerCarousel extends ConsumerStatefulWidget {
  const HomeBannerCarousel({super.key});

  @override
  ConsumerState<HomeBannerCarousel> createState() => HomeBannerCarouselState();
}

class HomeBannerCarouselState extends ConsumerState<HomeBannerCarousel> {
  final controller = PageController();
  Timer? timer;
  int page = 0;
  int count = 0;

  @override
  void dispose() {
    timer?.cancel();
    controller.dispose();
    super.dispose();
  }

  void syncTimer(int nextCount) {
    if (nextCount == count && (timer != null) == (nextCount > 1)) return;
    count = nextCount;
    timer?.cancel();
    timer = null;
    if (nextCount <= 1) return;
    timer = Timer.periodic(const Duration(seconds: 4), (_) {
      if (!mounted || !controller.hasClients || count <= 1) return;
      final next = (page + 1) % count;
      controller.animateToPage(
        next,
        duration: const Duration(milliseconds: 420),
        curve: Curves.easeOutCubic,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final online = ref.watch(deviceOnlineProvider).value ?? true;
    if (!online) {
      syncTimer(0);
      return const SizedBox.shrink();
    }

    final banners = ref.watch(homeBannersProvider);
    return banners.when(
      data: (items) {
        if (items.isEmpty) {
          syncTimer(0);
          return const SizedBox.shrink();
        }
        syncTimer(items.length);
        return Column(
          children: [
            const SizedBox(height: 20),
            SizedBox(
              height: 132,
              child: PageView.builder(
                controller: controller,
                itemCount: items.length,
                onPageChanged: (index) => setState(() => page = index),
                itemBuilder: (context, index) =>
                    _BannerSlide(banner: items[index]),
              ),
            ),
            if (items.length > 1) ...[
              const SizedBox(height: 10),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  for (var i = 0; i < items.length; i++)
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 220),
                      curve: Curves.easeOut,
                      width: i == page ? 18 : 7,
                      height: 7,
                      margin: const EdgeInsets.symmetric(horizontal: 3),
                      decoration: BoxDecoration(
                        color: i == page
                            ? AppColors.primary
                            : AppColors.primary.withValues(alpha: 0.28),
                        borderRadius: BorderRadius.circular(99),
                      ),
                    ),
                ],
              ),
            ],
          ],
        );
      },
      loading: () => const SizedBox.shrink(),
      error: (_, _) => const SizedBox.shrink(),
    );
  }
}

class _BannerSlide extends StatelessWidget {
  const _BannerSlide({required this.banner});

  final HomeBanner banner;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 1),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(22),
        child: Image.network(
          banner.imageUrl,
          width: double.infinity,
          height: 132,
          fit: BoxFit.cover,
          errorBuilder: (_, _, _) => const SizedBox.shrink(),
        ),
      ),
    );
  }
}
