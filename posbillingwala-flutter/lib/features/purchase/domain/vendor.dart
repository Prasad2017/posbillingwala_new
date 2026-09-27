class Vendor {
  const Vendor({
    required this.id,
    required this.name,
    this.gstin = '',
    this.contactName = '',
    this.mobile = '',
    this.email = '',
    this.address = '',
    this.paymentTerms = 'Net 30',
    this.creditLimit = 0,
    this.openingBalance = 0,
    this.bankDetails = '',
    this.status = 'ACTIVE',
    this.organizationId = '',
    this.branchId = '',
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String name;
  final String gstin;
  final String contactName;
  final String mobile;
  final String email;
  final String address;
  final String paymentTerms;
  final double creditLimit;
  final double openingBalance;
  final String bankDetails;
  final String status;
  final String organizationId;
  final String branchId;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  bool get isActive => status.toUpperCase() == 'ACTIVE';

  Vendor copyWith({
    String? id,
    String? name,
    String? gstin,
    String? contactName,
    String? mobile,
    String? email,
    String? address,
    String? paymentTerms,
    double? creditLimit,
    double? openingBalance,
    String? bankDetails,
    String? status,
    String? organizationId,
    String? branchId,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return Vendor(
      id: id ?? this.id,
      name: name ?? this.name,
      gstin: gstin ?? this.gstin,
      contactName: contactName ?? this.contactName,
      mobile: mobile ?? this.mobile,
      email: email ?? this.email,
      address: address ?? this.address,
      paymentTerms: paymentTerms ?? this.paymentTerms,
      creditLimit: creditLimit ?? this.creditLimit,
      openingBalance: openingBalance ?? this.openingBalance,
      bankDetails: bankDetails ?? this.bankDetails,
      status: status ?? this.status,
      organizationId: organizationId ?? this.organizationId,
      branchId: branchId ?? this.branchId,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'gstin': gstin,
    'contactName': contactName,
    'mobile': mobile,
    'email': email,
    'address': address,
    'paymentTerms': paymentTerms,
    'creditLimit': creditLimit,
    'openingBalance': openingBalance,
    'bankDetails': bankDetails,
    'status': status,
    'organizationId': organizationId,
    'branchId': branchId,
    'createdAt': createdAt?.toIso8601String(),
    'updatedAt': updatedAt?.toIso8601String(),
  };

  factory Vendor.fromJson(Map<String, dynamic> json) {
    double numOf(Object? v) {
      if (v is num) return v.toDouble();
      return double.tryParse('$v') ?? 0;
    }

    return Vendor(
      id: '${json['id'] ?? ''}',
      name: '${json['name'] ?? ''}'.trim(),
      gstin: '${json['gstin'] ?? ''}'.trim(),
      contactName: '${json['contactName'] ?? ''}'.trim(),
      mobile: '${json['mobile'] ?? ''}'.trim(),
      email: '${json['email'] ?? ''}'.trim(),
      address: '${json['address'] ?? ''}'.trim(),
      paymentTerms: '${json['paymentTerms'] ?? 'Net 30'}'.trim(),
      creditLimit: numOf(json['creditLimit']),
      openingBalance: numOf(json['openingBalance']),
      bankDetails: '${json['bankDetails'] ?? ''}'.trim(),
      status: '${json['status'] ?? 'ACTIVE'}'.trim(),
      organizationId: '${json['organizationId'] ?? ''}',
      branchId: '${json['branchId'] ?? ''}',
      createdAt: DateTime.tryParse('${json['createdAt'] ?? ''}'),
      updatedAt: DateTime.tryParse('${json['updatedAt'] ?? ''}'),
    );
  }
}
