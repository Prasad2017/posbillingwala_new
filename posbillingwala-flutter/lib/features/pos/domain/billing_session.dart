import 'package:flutter_riverpod/flutter_riverpod.dart';

/* Active billing context for POS / Takeaway / Table flows. */
class BillingSession {
  const BillingSession({
    required this.invoiceType,
    required this.title,
    required this.billingRoute,
    required this.paymentRoute,
    this.customerId,
    this.customerName,
    this.customerPhone,
    this.customerEmail,
    this.customerAddress,
    this.loyaltyPoints = 0,
    this.walletBalance = 0,
    this.creditLimit = 0,
    this.offerCode,
    this.invoicePrefix = 'PB',
    this.cartScope = '',
    this.tableNumber,
    this.diningSessionId,
  });

  final String invoiceType;
  final String title;
  final String billingRoute;
  final String paymentRoute;
  final String? customerId;
  final String? customerName;
  final String? customerPhone;
  final String? customerEmail;
  final String? customerAddress;
  final double loyaltyPoints;
  final double walletBalance;
  final double creditLimit;
  final String? offerCode;
  final String invoicePrefix;
  final String cartScope;
  final String? tableNumber;
  final int? diningSessionId;

  static const pos = BillingSession(
    invoiceType: 'fast_billing',
    title: 'Fast Billing',
    billingRoute: '/pos',
    paymentRoute: '/pos/payment',
  );

  BillingSession copyWith({
    String? invoiceType,
    String? title,
    String? billingRoute,
    String? paymentRoute,
    String? customerId,
    String? customerName,
    String? customerPhone,
    String? customerEmail,
    String? customerAddress,
    double? loyaltyPoints,
    double? walletBalance,
    double? creditLimit,
    String? offerCode,
    String? invoicePrefix,
    String? cartScope,
    String? tableNumber,
    int? diningSessionId,
    bool clearCustomer = false,
    bool clearTable = false,
    bool clearOffer = false,
  }) {
    return BillingSession(
      invoiceType: invoiceType ?? this.invoiceType,
      title: title ?? this.title,
      billingRoute: billingRoute ?? this.billingRoute,
      paymentRoute: paymentRoute ?? this.paymentRoute,
      customerId: clearCustomer ? null : (customerId ?? this.customerId),
      customerName: clearCustomer ? null : (customerName ?? this.customerName),
      customerPhone: clearCustomer
          ? null
          : (customerPhone ?? this.customerPhone),
      customerEmail: clearCustomer
          ? null
          : (customerEmail ?? this.customerEmail),
      customerAddress: clearCustomer
          ? null
          : (customerAddress ?? this.customerAddress),
      loyaltyPoints: clearCustomer ? 0 : (loyaltyPoints ?? this.loyaltyPoints),
      walletBalance: clearCustomer ? 0 : (walletBalance ?? this.walletBalance),
      creditLimit: clearCustomer ? 0 : (creditLimit ?? this.creditLimit),
      offerCode: clearOffer ? null : (offerCode ?? this.offerCode),
      invoicePrefix: invoicePrefix ?? this.invoicePrefix,
      cartScope: cartScope ?? this.cartScope,
      tableNumber: clearTable ? null : (tableNumber ?? this.tableNumber),
      diningSessionId: clearTable
          ? null
          : (diningSessionId ?? this.diningSessionId),
    );
  }
}

class BillingSessionController extends Notifier<BillingSession> {
  @override
  BillingSession build() => BillingSession.pos;

  void usePos() => state = BillingSession.pos;

  void startTakeaway({
    String? customerName,
    String? customerPhone,
    String? parcelNumber,
  }) {
    final parcel = trimOrNull(parcelNumber);
    state = BillingSession(
      invoiceType: 'take_away',
      title: parcel == null ? 'Takeaway Billing' : 'Parcel $parcel',
      billingRoute: '/takeaway/billing',
      paymentRoute: '/takeaway/payment',
      customerName: trimOrNull(customerName),
      customerPhone: trimOrNull(customerPhone),
      cartScope: parcel ?? '',
      tableNumber: parcel,
    );
  }

  void startTableBilling({
    required String tableNumber,
    required String tableName,
    required int diningSessionId,
  }) {
    state = BillingSession(
      invoiceType: 'table_wise',
      title: 'Table $tableName',
      billingRoute: '/tables/billing',
      paymentRoute: '/tables/payment',
      cartScope: tableNumber,
      tableNumber: tableNumber,
      diningSessionId: diningSessionId,
    );
  }

  void updateCustomer({
    String? id,
    String? name,
    String? phone,
    String? email,
    String? address,
    double? loyaltyPoints,
    double? walletBalance,
    double? creditLimit,
  }) {
    state = state.copyWith(
      customerId: id ?? state.customerId,
      customerName: name != null ? trimOrNull(name) : state.customerName,
      customerPhone: phone != null ? trimOrNull(phone) : state.customerPhone,
      customerEmail: email != null ? trimOrNull(email) : state.customerEmail,
      customerAddress:
          address != null ? trimOrNull(address) : state.customerAddress,
      loyaltyPoints: loyaltyPoints ?? state.loyaltyPoints,
      walletBalance: walletBalance ?? state.walletBalance,
      creditLimit: creditLimit ?? state.creditLimit,
    );
  }

  void setOfferCode(String? code) {
    final c = trimOrNull(code);
    state = state.copyWith(offerCode: c, clearOffer: c == null);
  }

  String? trimOrNull(String? value) {
    final trimmed = value?.trim();
    if (trimmed == null || trimmed.isEmpty) return null;
    return trimmed;
  }
}

final billingSessionProvider =
    NotifierProvider<BillingSessionController, BillingSession>(
      BillingSessionController.new,
    );
