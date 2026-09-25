import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/home/data/home_banner_api.dart';

/* Online-only — empty list when offline (no cache). */
final homeBannersProvider = FutureProvider.autoDispose<List<HomeBanner>>((
  ref,
) async {
  final online = await ref.watch(deviceOnlineProvider.future);
  if (!online) return const [];

  final userId = ref.watch(authControllerProvider).session?.licenceUserId;
  if (userId == null || userId.isEmpty) return const [];
  return HomeBannerApi(ref.read(apiClientProvider)).fetch(userId: userId);
});
