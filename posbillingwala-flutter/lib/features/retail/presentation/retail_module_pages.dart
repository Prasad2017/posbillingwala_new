import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/branch_scope.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/user_session.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/features/enterprise/data/enterprise_api.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/sync/domain/cloud_screen_cache.dart';

class ProductVariant {
  const ProductVariant({
    required this.id,
    required this.productName,
    this.productId = 0,
    this.brand = '',
    this.sizeLabel = '',
    this.colorLabel = '',
    this.sku = '',
    this.barcode = '',
    this.fabric = '',
    this.stockQty = 0,
    this.sellingPrice = 0,
    this.mrp = 0,
  });

  final String id;
  final int productId;
  final String productName;
  final String brand;
  final String sizeLabel;
  final String colorLabel;
  final String sku;
  final String barcode;
  final String fabric;
  final double stockQty;
  final double sellingPrice;
  final double mrp;

  Map<String, dynamic> toJson() => {
    'id': id,
    'clientId': id,
    'productId': productId,
    'productName': productName,
    'brand': brand,
    'sizeLabel': sizeLabel,
    'colorLabel': colorLabel,
    'sku': sku,
    'barcode': barcode,
    'fabric': fabric,
    'stockQty': stockQty,
    'sellingPrice': sellingPrice,
    'mrp': mrp,
    'status': 'ACTIVE',
  };

  factory ProductVariant.fromJson(Map<String, dynamic> json) {
    double n(Object? v) =>
        v is num ? v.toDouble() : double.tryParse('$v') ?? 0;
    return ProductVariant(
      id: '${json['id'] ?? json['clientId'] ?? ''}',
      productId: int.tryParse('${json['productId']}') ?? 0,
      productName: '${json['productName'] ?? ''}',
      brand: '${json['brand'] ?? ''}',
      sizeLabel: '${json['sizeLabel'] ?? ''}',
      colorLabel: '${json['colorLabel'] ?? ''}',
      sku: '${json['sku'] ?? ''}',
      barcode: '${json['barcode'] ?? ''}',
      fabric: '${json['fabric'] ?? ''}',
      stockQty: n(json['stockQty']),
      sellingPrice: n(json['sellingPrice']),
      mrp: n(json['mrp']),
    );
  }
}

abstract final class VariantLocalStore {
  static String _key(UserSession s) =>
      'product_variants_${BranchScope.effectiveOrganizationId(s)}_${BranchScope.effectiveBranchId(s)}';

  static Future<List<ProductVariant>> load(UserSession session) async {
    final rows = await CloudScreenCache.loadMapList(_key(session));
    return rows.map(ProductVariant.fromJson).toList();
  }

  static Future<void> save(
    UserSession session,
    List<ProductVariant> list,
  ) => CloudScreenCache.saveJson(
    _key(session),
    list.map((e) => e.toJson()).toList(),
  );

  static Future<ProductVariant> upsert(
    UserSession session,
    ProductVariant v,
  ) async {
    final list = await load(session);
    final i = list.indexWhere((e) => e.id == v.id);
    if (i >= 0) {
      list[i] = v;
    } else {
      list.add(v);
    }
    await save(session, list);
    return v;
  }
}

final variantsProvider = FutureProvider<List<ProductVariant>>((ref) async {
  final session = ref.watch(authControllerProvider).session;
  if (session == null) return const [];
  final local = await VariantLocalStore.load(session);
  if (await isDeviceOnline()) {
    try {
      final remote = await ref
          .read(enterpriseApiProvider)
          .fetchProductVariants(session.licenceUserId);
      if (remote.isNotEmpty) {
        final mapped = remote.map(ProductVariant.fromJson).toList();
        await VariantLocalStore.save(session, mapped);
        return mapped;
      }
    } catch (_) {}
  }
  return local;
});

class VariantController extends Notifier<void> {
  @override
  void build() {}

  Future<ProductVariant> save({
    String? id,
    required String productName,
    String brand = '',
    String sizeLabel = '',
    String colorLabel = '',
    String sku = '',
    String barcode = '',
    String fabric = '',
    double stockQty = 0,
    double sellingPrice = 0,
    double mrp = 0,
  }) async {
    final session = ref.read(authControllerProvider).session;
    if (session == null) throw StateError('Not signed in');
    final variant = ProductVariant(
      id: (id == null || id.isEmpty)
          ? 'var_${DateTime.now().millisecondsSinceEpoch}'
          : id,
      productName: productName.trim(),
      brand: brand.trim(),
      sizeLabel: sizeLabel.trim(),
      colorLabel: colorLabel.trim(),
      sku: sku.trim(),
      barcode: barcode.trim(),
      fabric: fabric.trim(),
      stockQty: stockQty,
      sellingPrice: sellingPrice,
      mrp: mrp,
    );
    final saved = await VariantLocalStore.upsert(session, variant);
    ref.invalidate(variantsProvider);
    if (await isDeviceOnline()) {
      try {
        await ref.read(enterpriseApiProvider).saveProductVariant(
              userId: session.licenceUserId,
              variant: saved.toJson(),
            );
      } catch (_) {}
    }
    return saved;
  }
}

final variantControllerProvider = NotifierProvider<VariantController, void>(
  VariantController.new,
);

class VariantMatrixPage extends ConsumerWidget {
  const VariantMatrixPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(variantsProvider);
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: const Text('Size / Color Matrix')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/masters/variants/new'),
        icon: const Icon(Icons.add),
        label: const Text('Add variant'),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (list) {
          if (list.isEmpty) {
            return const Center(
              child: Text('No variants yet — add size × color SKUs'),
            );
          }
          return ResponsiveScrollShell(
            child: ListView.separated(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                16,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                100,
              ),
              itemCount: list.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, i) {
                final v = list[i];
                return ListTile(
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                    side: const BorderSide(color: AppColors.border),
                  ),
                  title: Text(
                    v.productName,
                    style: const TextStyle(
                      fontFamily: AppFonts.family,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  subtitle: Text(
                    '${v.sizeLabel}/${v.colorLabel}'
                    '${v.brand.isEmpty ? '' : ' · ${v.brand}'}'
                    ' · Stock ${v.stockQty}'
                    '${v.sku.isEmpty ? '' : ' · ${v.sku}'}',
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '₹${v.sellingPrice.toStringAsFixed(0)}',
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          color: AppColors.primary,
                        ),
                      ),
                      IconButton(
                        tooltip: 'Add to POS cart',
                        icon: const Icon(Icons.add_shopping_cart_rounded),
                        onPressed: () async {
                          final pid = v.productId > 0
                              ? v.productId
                              : (v.id.hashCode.abs() % 100000);
                          final product = Product(
                            productId: pid,
                            productName:
                                '${v.productName} (${v.sizeLabel}/${v.colorLabel})',
                            productPrice: v.sellingPrice,
                            productMrp:
                                v.mrp > 0 ? v.mrp : v.sellingPrice,
                            priceIncludesGst: '0',
                            openPrice: '0',
                            productCgst: 0,
                            productSgst: 0,
                            productWithGstPrice: v.sellingPrice,
                            productDeletedStatus: '0',
                            productStatus: '1',
                            productSyncStatus: '0',
                            productCode: v.sku.isNotEmpty ? v.sku : v.barcode,
                          );
                          await ref
                              .read(posCartControllerProvider.notifier)
                              .addProduct(
                                product,
                                unitPriceOverride: v.sellingPrice,
                              );
                          if (context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(
                                  'Added ${v.productName} ${v.sizeLabel}/${v.colorLabel}',
                                ),
                                action: SnackBarAction(
                                  label: 'POS',
                                  onPressed: () => context.go('/pos'),
                                ),
                              ),
                            );
                          }
                        },
                      ),
                    ],
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class VariantFormPage extends ConsumerStatefulWidget {
  const VariantFormPage({super.key});

  @override
  ConsumerState<VariantFormPage> createState() => VariantFormPageState();
}

class VariantFormPageState extends ConsumerState<VariantFormPage> {
  final name = TextEditingController();
  final brand = TextEditingController();
  final size = TextEditingController();
  final color = TextEditingController();
  final sku = TextEditingController();
  final barcode = TextEditingController();
  final fabric = TextEditingController();
  final stock = TextEditingController(text: '0');
  final price = TextEditingController(text: '0');
  final mrp = TextEditingController(text: '0');
  var busy = false;

  @override
  void dispose() {
    name.dispose();
    brand.dispose();
    size.dispose();
    color.dispose();
    sku.dispose();
    barcode.dispose();
    fabric.dispose();
    stock.dispose();
    price.dispose();
    mrp.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() => busy = true);
    try {
      await ref.read(variantControllerProvider.notifier).save(
            productName: name.text,
            brand: brand.text,
            sizeLabel: size.text,
            colorLabel: color.text,
            sku: sku.text,
            barcode: barcode.text,
            fabric: fabric.text,
            stockQty: double.tryParse(stock.text) ?? 0,
            sellingPrice: double.tryParse(price.text) ?? 0,
            mrp: double.tryParse(mrp.text) ?? 0,
          );
      if (mounted) context.pop();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Add variant')),
      body: ResponsivePageBody(
        child: Column(
          children: [
            AppTextField(controller: name, label: 'Product / Style *'),
            const SizedBox(height: 12),
            ResponsiveFormColumns(
              children: [
                AppTextField(controller: brand, label: 'Brand'),
                AppTextField(controller: size, label: 'Size'),
                AppTextField(controller: color, label: 'Color'),
                AppTextField(controller: fabric, label: 'Fabric'),
                AppTextField(controller: sku, label: 'SKU'),
                AppTextField(controller: barcode, label: 'Barcode'),
                AppTextField(
                  controller: stock,
                  label: 'Stock qty',
                  keyboardType: TextInputType.number,
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                ),
                AppTextField(
                  controller: price,
                  label: 'Selling price',
                  keyboardType: TextInputType.number,
                ),
                AppTextField(
                  controller: mrp,
                  label: 'MRP',
                  keyboardType: TextInputType.number,
                ),
              ],
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: busy ? null : save,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(52),
                backgroundColor: AppColors.primary,
              ),
              child: Text(busy ? 'Saving…' : 'Save variant'),
            ),
          ],
        ),
      ),
    );
  }
}
