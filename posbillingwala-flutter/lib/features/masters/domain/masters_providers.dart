import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/masters/data/masters_api.dart';
import 'package:pos_billingwala_v2/features/masters/data/masters_repository.dart';

final mastersRepositoryProvider = Provider<MastersRepository>((ref) {
  return MastersRepository(
    api: MastersApi(ref.watch(apiClientProvider)),
    db: ref.watch(appDatabaseProvider),
  );
});

final categoriesProvider = StreamProvider<List<ProductCategory>>((ref) {
  return ref.watch(mastersRepositoryProvider).watchCategories();
});

final subcategoriesProvider = StreamProvider<List<ProductSubcategory>>((ref) {
  return ref.watch(mastersRepositoryProvider).watchSubcategories();
});

final foodTypesProvider = StreamProvider<List<FoodType>>((ref) {
  return ref.watch(mastersRepositoryProvider).watchFoodTypes();
});

final diningAreasProvider = StreamProvider<List<DiningArea>>((ref) {
  return ref.watch(mastersRepositoryProvider).watchDiningAreas();
});

final tableTypesProvider = StreamProvider<List<TableType>>((ref) {
  return ref.watch(mastersRepositoryProvider).watchTableTypes();
});

final portionMastersProvider = StreamProvider<List<PortionMaster>>((ref) {
  return ref.watch(mastersRepositoryProvider).watchPortionMasters();
});

final selectedCategoryIdProvider =
    NotifierProvider<SelectedCategoryId, int?>(SelectedCategoryId.new);

class SelectedCategoryId extends Notifier<int?> {
  @override
  int? build() => null;

  void select(int? id) => state = id;
}

final productsProvider = StreamProvider<List<Product>>((ref) {
  final categoryId = ref.watch(selectedCategoryIdProvider);
  return ref
      .watch(mastersRepositoryProvider)
      .watchProducts(categoryId: categoryId);
});

/* Unfiltered product stream — product pickers / combo forms. */
final allProductsProvider = StreamProvider<List<Product>>((ref) {
  return ref.watch(mastersRepositoryProvider).watchProducts();
});

final catalogCategoryCountProvider = StreamProvider<int>((ref) {
  return ref.watch(appDatabaseProvider).watchCountActiveCategories();
});

final catalogSubcategoryCountProvider = StreamProvider<int>((ref) {
  return ref.watch(appDatabaseProvider).watchCountActiveSubcategories();
});

final catalogProductCountProvider = StreamProvider<int>((ref) {
  return ref.watch(appDatabaseProvider).watchCountActiveProducts();
});

final catalogComboCountProvider = StreamProvider<int>((ref) {
  return ref.watch(appDatabaseProvider).watchCountActiveCombos();
});

final catalogCountsProvider = Provider<
    ({int categories, int products, int combos, int subcategories})>((ref) {
  int countOf(AsyncValue<int> value) => value.maybeWhen(
        data: (n) => n,
        orElse: () => 0,
      );
  return (
    categories: countOf(ref.watch(catalogCategoryCountProvider)),
    products: countOf(ref.watch(catalogProductCountProvider)),
    combos: countOf(ref.watch(catalogComboCountProvider)),
    subcategories: countOf(ref.watch(catalogSubcategoryCountProvider)),
  );
});

final combosListProvider = StreamProvider<List<Combo>>((ref) {
  return ref.watch(appDatabaseProvider).watchActiveCombos();
});

class MastersSyncController extends Notifier<AsyncValue<MastersSyncResult?>> {
  @override
  AsyncValue<MastersSyncResult?> build() => const AsyncData(null);

  Future<void> syncNow() async {
    final session = ref.read(authControllerProvider).session;
    final ownerId = session?.catalogOwnerId;
    final licenceId = session?.licenceUserId;
    if (ownerId == null || ownerId.isEmpty) {
      state = AsyncError(
        'Please login before syncing masters',
        StackTrace.current,
      );
      return;
    }

    state = const AsyncLoading();
    state = await AsyncValue.guard(() {
      return ref.read(mastersRepositoryProvider).syncFromCloud(
            ownerId: ownerId,
            licenceUserId: licenceId ?? ownerId,
          );
    });
  }

  Future<int> uploadPending() async {
    final session = ref.read(authControllerProvider).session;
    final ownerId = session?.catalogOwnerId;
    final licenceId = session?.licenceUserId;
    if (ownerId == null || ownerId.isEmpty) {
      state = AsyncError(
        'Please login before uploading masters',
        StackTrace.current,
      );
      return 0;
    }
    state = const AsyncLoading();
    try {
      final count = await ref.read(mastersRepositoryProvider).uploadPendingMasters(
            ownerId: ownerId,
            licenceUserId: licenceId,
          );
      state = AsyncData(
        MastersSyncResult(
          foodTypeCount: 0,
          categoryCount: 0,
          productCount: 0,
          portionCount: 0,
          uploadedPending: count,
        ),
      );
      return count;
    } catch (e, st) {
      state = AsyncError(e, st);
      return 0;
    }
  }

  Future<void> createCategory(
    String name, {
    int? foodTypeId,
    String? foodTypeCode,
  }) async {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    await ref.read(mastersRepositoryProvider).createCategory(
          userId: userId,
          categoryName: name,
          foodTypeId: foodTypeId,
          foodTypeCode: foodTypeCode,
        );
  }

  Future<int> createProduct({
    required String name,
    required double price,
    double mrp = 0,
    int? categoryId,
    String? categoryName,
    String? productCode,
    String? productImage,
    String openPrice = '0',
    String priceIncludesGst = '0',
    String? productUnit,
    double productCgst = 0,
    double productSgst = 0,
    int? subcategoryId,
  }) async {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).createProduct(
          userId: userId,
          productName: name,
          productPrice: price,
          productMrp: mrp,
          categoryId: categoryId,
          categoryName: categoryName,
          productCode: productCode,
          productImage: productImage,
          openPrice: openPrice,
          priceIncludesGst: priceIncludesGst,
          productUnit: productUnit,
          productCgst: productCgst,
          productSgst: productSgst,
          subcategoryId: subcategoryId,
        );
  }

  Future<void> updateCategory({
    required int categoryId,
    required String name,
    int? foodTypeId,
    String? foodTypeCode,
  }) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).updateCategory(
          userId: userId,
          categoryId: categoryId,
          categoryName: name,
          foodTypeId: foodTypeId,
          foodTypeCode: foodTypeCode,
        );
  }

  Future<void> deleteCategory(int categoryId) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).deleteCategory(
          userId: userId,
          categoryId: categoryId,
        );
  }

  Future<void> updateProduct({
    required int productId,
    required String name,
    required double price,
    double mrp = 0,
    int? categoryId,
    String? categoryName,
    String? productCode,
    String? productImage,
    bool clearProductImage = false,
    String openPrice = '0',
    String priceIncludesGst = '0',
    String? productUnit,
    double productCgst = 0,
    double productSgst = 0,
    int? subcategoryId,
  }) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).updateProduct(
          userId: userId,
          productId: productId,
          productName: name,
          productPrice: price,
          productMrp: mrp,
          categoryId: categoryId,
          categoryName: categoryName,
          productCode: productCode,
          productImage: productImage,
          clearProductImage: clearProductImage,
          openPrice: openPrice,
          priceIncludesGst: priceIncludesGst,
          productUnit: productUnit,
          productCgst: productCgst,
          productSgst: productSgst,
          subcategoryId: subcategoryId,
        );
  }

  Future<void> deleteProduct(int productId) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).deleteProduct(
          userId: userId,
          productId: productId,
        );
  }

  Future<int> createPortion({
    required int productId,
    required String portionName,
    required double portionPrice,
    int portionSortOrder = 1,
    int? portionMasterId,
  }) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).createPortion(
          userId: userId,
          productId: productId,
          portionName: portionName,
          portionPrice: portionPrice,
          portionSortOrder: portionSortOrder,
          portionMasterId: portionMasterId,
        );
  }

  Future<void> deletePortion(int portionId) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).deletePortion(
          userId: userId,
          portionId: portionId,
        );
  }

  Future<void> createCombo({
    required String name,
    required double price,
    String? comboCode,
    double comboCgst = 0,
    double comboSgst = 0,
    bool activeOnPos = true,
    List<({int productId, int quantity})> items = const [],
  }) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).createCombo(
          userId: userId,
          comboName: name,
          comboPrice: price,
          comboCode: comboCode,
          comboCgst: comboCgst,
          comboSgst: comboSgst,
          activeOnPos: activeOnPos,
          items: items,
        );
  }

  Future<void> updateCombo({
    required int comboId,
    required String name,
    required double price,
    String? comboCode,
    double comboCgst = 0,
    double comboSgst = 0,
    bool? activeOnPos,
    List<({int productId, int quantity})> items = const [],
  }) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).updateCombo(
          userId: userId,
          comboId: comboId,
          comboName: name,
          comboPrice: price,
          comboCode: comboCode,
          comboCgst: comboCgst,
          comboSgst: comboSgst,
          activeOnPos: activeOnPos,
          items: items,
        );
  }

  Future<void> deleteCombo(int comboId) {
    final userId =
        ref.read(authControllerProvider).session?.catalogOwnerId ?? '';
    return ref.read(mastersRepositoryProvider).deleteCombo(
          userId: userId,
          comboId: comboId,
        );
  }
}

final mastersSyncControllerProvider =
    NotifierProvider<MastersSyncController, AsyncValue<MastersSyncResult?>>(
  MastersSyncController.new,
);
