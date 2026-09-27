import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/features/auth/data/device_identity_service.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/domain/license_validator.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_session.dart';
import 'package:pos_billingwala_v2/features/pos/domain/billing_date.dart';
import 'package:pos_billingwala_v2/features/pos/domain/payment_mode.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/domain/enterprise_runtime.dart';
import 'package:pos_billingwala_v2/features/enterprise_ops/domain/ops_providers.dart';
import 'package:pos_billingwala_v2/features/pos/domain/pos_providers.dart';
import 'package:pos_billingwala_v2/features/staff/data/staff_store.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/staff/domain/staff_user.dart';

class PaymentCheckoutState {
  const PaymentCheckoutState({
    this.mode = PaymentMode.cash,
    this.cashAmount = 0,
    this.upiAmount = 0,
    this.discount = 0,
    this.discountType = 'Amount',
    this.packingCharge = 0,
    this.packingChargeType = 'Amount',
    this.loyaltyRedeem = 0,
    this.offerCode = '',
    this.offerLabel = '',
    this.serialByProductId = const {},
    this.busy = false,
    this.errorMessage,
    this.result,
  });

  final PaymentMode mode;
  final double cashAmount;
  final double upiAmount;
  final double discount;
  final String discountType;
  final double packingCharge;
  final String packingChargeType;
  final double loyaltyRedeem;
  final String offerCode;
  final String offerLabel;
  final Map<int, String> serialByProductId;
  final bool busy;
  final String? errorMessage;
  final SavedInvoiceResult? result;

  double discountValue(double subtotal) {
    if (discount <= 0 || subtotal <= 0) return 0;
    final double raw;
    if (discountType.toLowerCase().startsWith('p')) {
      raw = subtotal * discount.clamp(0, 100) / 100;
    } else {
      raw = discount;
    }
    /* Discount amount can never exceed subtotal. */
    return double.parse(raw.clamp(0, subtotal).toStringAsFixed(2));
  }

  double packingValue(double subtotal) {
    if (packingCharge <= 0) return 0;
    if (packingChargeType.toLowerCase().startsWith('p')) {
      return double.parse((subtotal * packingCharge / 100).toStringAsFixed(2));
    }
    return double.parse(packingCharge.toStringAsFixed(2));
  }

  double payableTotal({required double subtotal, required double taxTotal}) {
    final disc = discountValue(subtotal);
    final pack = packingValue(subtotal);
    final loyalty = loyaltyRedeem.clamp(0, double.infinity);
    final raw = (subtotal + taxTotal + pack - disc - loyalty)
        .clamp(0, double.infinity)
        .toDouble();
    /* Match Android CreatePos / BluetoothPrint — bill total rounds up to ₹. */
    return raw.ceilToDouble();
  }

  PaymentCheckoutState copyWith({
    PaymentMode? mode,
    double? cashAmount,
    double? upiAmount,
    double? discount,
    String? discountType,
    double? packingCharge,
    String? packingChargeType,
    double? loyaltyRedeem,
    String? offerCode,
    String? offerLabel,
    Map<int, String>? serialByProductId,
    bool? busy,
    String? errorMessage,
    SavedInvoiceResult? result,
    bool clearError = false,
    bool clearResult = false,
  }) {
    return PaymentCheckoutState(
      mode: mode ?? this.mode,
      cashAmount: cashAmount ?? this.cashAmount,
      upiAmount: upiAmount ?? this.upiAmount,
      discount: discount ?? this.discount,
      discountType: discountType ?? this.discountType,
      packingCharge: packingCharge ?? this.packingCharge,
      packingChargeType: packingChargeType ?? this.packingChargeType,
      loyaltyRedeem: loyaltyRedeem ?? this.loyaltyRedeem,
      offerCode: offerCode ?? this.offerCode,
      offerLabel: offerLabel ?? this.offerLabel,
      serialByProductId: serialByProductId ?? this.serialByProductId,
      busy: busy ?? this.busy,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      result: clearResult ? null : (result ?? this.result),
    );
  }
}

class PaymentCheckoutController extends Notifier<PaymentCheckoutState> {
  @override
  PaymentCheckoutState build() => const PaymentCheckoutState();

  void selectMode(PaymentMode mode, double totalAmount) {
    switch (mode) {
      case PaymentMode.cash:
        state = state.copyWith(
          mode: mode,
          cashAmount: totalAmount,
          upiAmount: 0,
          clearError: true,
        );
      case PaymentMode.upi:
        state = state.copyWith(
          mode: mode,
          cashAmount: 0,
          upiAmount: totalAmount,
          clearError: true,
        );
      case PaymentMode.credit:
      case PaymentMode.wallet:
        state = state.copyWith(
          mode: mode,
          cashAmount: 0,
          upiAmount: 0,
          clearError: true,
        );
      case PaymentMode.cashPlusUpi:
        state = state.copyWith(
          mode: mode,
          cashAmount: 0,
          upiAmount: 0,
          clearError: true,
        );
    }
  }

  void setLoyaltyRedeem(double points) {
    final session = ref.read(billingSessionProvider);
    final maxPts = session.loyaltyPoints;
    final next = points.clamp(0, maxPts).toDouble();
    state = state.copyWith(
      loyaltyRedeem: double.parse(next.toStringAsFixed(2)),
      clearError: true,
    );
  }

  Future<String?> applyOfferCode(String code, double subtotal) async {
    final offer = await EnterpriseRuntime.findActiveOffer(ref, code);
    if (offer == null) {
      state = state.copyWith(
        offerCode: '',
        offerLabel: '',
        errorMessage: 'Invalid or expired coupon',
      );
      return 'Invalid or expired coupon';
    }
    final cart = await ref.read(cartItemsProvider.future);
    var cheapest = 0.0;
    for (final line in cart) {
      final linePrice = line.unitPrice;
      if (cheapest <= 0 || linePrice < cheapest) cheapest = linePrice;
    }
    final amt = EnterpriseRuntime.offerDiscountAmount(
      offer,
      subtotal,
      cartLineCount: cart.fold<int>(0, (s, e) => s + e.quantity.round()),
      cheapestLine: cheapest,
    );
    setDiscount(amt, type: 'Amount', subtotal: subtotal);
    final label = (offer['name'] ?? offer['title'] ?? code).toString();
    state = state.copyWith(
      offerCode: code.trim().toUpperCase(),
      offerLabel: label,
      clearError: true,
    );
    ref.read(billingSessionProvider.notifier).setOfferCode(code);
    return null;
  }

  void clearOffer() {
    state = state.copyWith(offerCode: '', offerLabel: '', discount: 0);
    ref.read(billingSessionProvider.notifier).setOfferCode(null);
  }

  void setSerialForProduct(int productId, String serialNo) {
    final next = Map<int, String>.from(state.serialByProductId)
      ..[productId] = serialNo.trim();
    state = state.copyWith(serialByProductId: next, clearError: true);
  }

  void setCashAmount(double value, double totalAmount) {
    final cash = value < 0 ? 0.0 : value;
    final remaining = double.parse(
      (totalAmount - cash).clamp(0, totalAmount).toStringAsFixed(2),
    );
    state = state.copyWith(
      cashAmount: double.parse(cash.toStringAsFixed(2)),
      upiAmount: remaining,
      clearError: true,
    );
  }

  void setUpiAmount(double value, double totalAmount) {
    final upi = value < 0 ? 0.0 : value;
    final remaining = double.parse(
      (totalAmount - upi).clamp(0, totalAmount).toStringAsFixed(2),
    );
    state = state.copyWith(
      upiAmount: double.parse(upi.toStringAsFixed(2)),
      cashAmount: remaining,
      clearError: true,
    );
  }

  void setSplitAmounts({
    required double cash,
    required double upi,
  }) {
    state = state.copyWith(
      mode: PaymentMode.cashPlusUpi,
      cashAmount: double.parse((cash < 0 ? 0 : cash).toStringAsFixed(2)),
      upiAmount: double.parse((upi < 0 ? 0 : upi).toStringAsFixed(2)),
      clearError: true,
    );
  }

  void setDiscount(double value, {String? type, double? subtotal}) {
    final nextType = type ?? state.discountType;
    final isPercent = nextType.toLowerCase().startsWith('p');
    var next = value < 0 ? 0.0 : value;
    final perms = ref.read(permissionControllerProvider);
    final cap = ref.read(maxDiscountPctProvider).asData?.value ?? 100.0;
    if (isPercent) {
      next = next.clamp(0, 100).toDouble();
      if (!perms.allows('billing.max_discount') && next > cap) {
        next = cap;
      }
    } else if (subtotal != null) {
      next = next.clamp(0, subtotal).toDouble();
      if (!perms.allows('billing.max_discount') && subtotal > 0) {
        final maxAmt = subtotal * cap / 100;
        if (next > maxAmt) next = maxAmt;
      }
    }
    state = state.copyWith(
      discount: double.parse(next.toStringAsFixed(2)),
      discountType: type,
      clearError: true,
    );
  }

  void setPacking(double value, {String? type}) {
    state = state.copyWith(
      packingCharge: value < 0 ? 0 : double.parse(value.toStringAsFixed(2)),
      packingChargeType: type,
      clearError: true,
    );
  }

  Future<SavedInvoiceResult?> completePayment({
    required double subtotal,
    required double taxTotal,
  }) async {
    final totalAmount = state.payableTotal(
      subtotal: subtotal,
      taxTotal: taxTotal,
    );
    state = state.copyWith(busy: true, clearError: true, clearResult: true);
    try {
      if (!await ensureOnline(force: AppPlatform.requiresNetwork)) {
        state = state.copyWith(
          busy: false,
          errorMessage: kOnlineRequiredMessage,
        );
        return null;
      }

      final billing = ref.read(billingSessionProvider);

      /* Block sale when expired lots are linked to cart products. */
      final cart = await ref.read(cartItemsProvider.future);
      final productIds = cart.map((e) => e.productId).toSet();
      final warnings = await EnterpriseRuntime.expiredLotWarnings(
        ref,
        productIds: productIds,
      );
      final expired = warnings.where((w) => w.startsWith('Expired')).toList();
      if (expired.isNotEmpty) {
        state = state.copyWith(
          busy: false,
          errorMessage: expired.first,
        );
        return null;
      }

      final serialErr = await EnterpriseRuntime.validateSerialsForSale(
        ref,
        productIds: productIds,
        serialByProductId: state.serialByProductId,
      );
      if (serialErr != null) {
        state = state.copyWith(busy: false, errorMessage: serialErr);
        return null;
      }

      if (state.mode == PaymentMode.credit) {
        if ((billing.customerId ?? '').isEmpty) {
          state = state.copyWith(
            busy: false,
            errorMessage: 'Select a CRM customer for credit sale',
          );
          return null;
        }
        if (billing.creditLimit > 0 && totalAmount > billing.creditLimit) {
          state = state.copyWith(
            busy: false,
            errorMessage:
                'Credit limit ₹${billing.creditLimit.toStringAsFixed(0)} exceeded',
          );
          return null;
        }
      }

      if (state.mode == PaymentMode.wallet) {
        if ((billing.customerId ?? '').isEmpty) {
          state = state.copyWith(
            busy: false,
            errorMessage: 'Select a CRM customer for wallet payment',
          );
          return null;
        }
        if (totalAmount > billing.walletBalance + 0.01) {
          state = state.copyWith(
            busy: false,
            errorMessage:
                'Wallet balance ₹${billing.walletBalance.toStringAsFixed(2)} insufficient',
          );
          return null;
        }
      }

      if (state.loyaltyRedeem > 0 &&
          state.loyaltyRedeem > billing.loyaltyPoints + 0.01) {
        state = state.copyWith(
          busy: false,
          errorMessage: 'Not enough loyalty points',
        );
        return null;
      }

      final tender = PaymentTender.resolve(
        mode: state.mode,
        totalAmount: totalAmount,
        cashAmount: state.cashAmount,
        upiAmount: state.upiAmount,
      );
      if (!tender.isValidFor(totalAmount)) {
        state = state.copyWith(
          busy: false,
          errorMessage: 'Cash + UPI must equal the bill total',
        );
        return null;
      }

      final session = ref.read(billingSessionProvider);
      final printer = ref.read(printerSettingsProvider);
      final authSession = ref.read(authControllerProvider).session;
      /* Parallel prep — avoid serial I/O before the local save. */
      final prep = await Future.wait<Object?>([
        ref.read(appDatabaseProvider).countTotalInvoices(),
        DeviceIdentityService().resolve(),
        StaffStore().read(),
      ]);
      final invoiceCount = prep[0]! as int;
      final device = prep[1]! as DeviceIdentity;
      final staff = prep[2] as StaffUser?;
      final licence = await LicenseValidator.validate(
        deviceId: device.deviceId,
        licenceKey: authSession?.licenceKey ?? '',
        localInvoiceCount: invoiceCount,
      );
      if (licence.trialBillBlocked ||
          (!licence.valid && !licence.legacyFallback)) {
        state = state.copyWith(
          busy: false,
          errorMessage: licence.message.isNotEmpty
              ? licence.message
              : 'Billing is not allowed for this licence',
        );
        return null;
      }

      final prefix = printer.invoicePrefix.trim().isNotEmpty
          ? printer.invoicePrefix.trim()
          : session.invoicePrefix;
      final staffId = int.tryParse(staff?.id ?? '');
      final result = await ref
          .read(appDatabaseProvider)
          .saveInvoiceFromCart(
            tender: tender,
            invoiceType: session.invoiceType,
            invoicePrefix: prefix,
            customerName: session.customerName,
            customerMobile: session.customerPhone,
            customerEmail: session.customerEmail,
            customerAddress: session.customerAddress,
            cartScope: session.cartScope,
            tableNumber: session.tableNumber,
            diningSessionId: session.diningSessionId,
            discount: state.discount + state.loyaltyRedeem,
            discountType: state.discountType,
            packingCharge: state.packingCharge,
            packingChargeType: state.packingChargeType,
            updateInventory: printer.productQuantityUpdate,
            createdByStaffId: staffId,
            createdByStaffName: staff?.name,
            billingDate: resolveBillingDateTimeFromRef(ref),
          );

      /* Post-sale: loyalty earn/redeem, wallet debit, offer use count. */
      final cid = session.customerId;
      if (cid != null && cid.isNotEmpty) {
        final earn = EnterpriseRuntime.earnPoints(totalAmount);
        await EnterpriseRuntime.adjustLoyalty(
          ref,
          customerId: cid,
          earn: earn,
          redeem: state.loyaltyRedeem,
        );
        if (state.mode == PaymentMode.wallet) {
          await EnterpriseRuntime.adjustWallet(
            ref,
            customerId: cid,
            debit: totalAmount,
          );
        }
      }
      if (state.offerCode.isNotEmpty) {
        final offer =
            await EnterpriseRuntime.findActiveOffer(ref, state.offerCode);
        if (offer != null) {
          await EnterpriseRuntime.bumpOfferUse(ref, offer);
        }
      }
      for (final sn in state.serialByProductId.values) {
        if (sn.trim().isEmpty) continue;
        await EnterpriseRuntime.markSerialSold(ref, serialNo: sn);
      }

      state = state.copyWith(busy: false, result: result);
      return result;
    } catch (e) {
      state = state.copyWith(
        busy: false,
        errorMessage: e.toString().replaceFirst('Bad state: ', ''),
      );
      return null;
    }
  }

  void reset() {
    state = const PaymentCheckoutState();
  }
}

final paymentCheckoutControllerProvider =
    NotifierProvider<PaymentCheckoutController, PaymentCheckoutState>(
      PaymentCheckoutController.new,
    );
