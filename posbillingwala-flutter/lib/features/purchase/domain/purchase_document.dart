enum PurchaseDocType { request, order, grn, invoice }

enum PurchaseDocStatus {
  draft,
  submitted,
  approved,
  ordered,
  partial,
  received,
  cancelled,
}

extension PurchaseDocTypeX on PurchaseDocType {
  String get id => name;

  String get label => switch (this) {
    PurchaseDocType.request => 'Purchase Request',
    PurchaseDocType.order => 'Purchase Order',
    PurchaseDocType.grn => 'GRN',
    PurchaseDocType.invoice => 'Purchase Invoice',
  };

  String get shortLabel => switch (this) {
    PurchaseDocType.request => 'PR',
    PurchaseDocType.order => 'PO',
    PurchaseDocType.grn => 'GRN',
    PurchaseDocType.invoice => 'PI',
  };

  static PurchaseDocType parse(String? raw) {
    final v = (raw ?? '').trim().toLowerCase();
    return PurchaseDocType.values.firstWhere(
      (e) => e.name == v || e.id == v,
      orElse: () => PurchaseDocType.order,
    );
  }
}

extension PurchaseDocStatusX on PurchaseDocStatus {
  String get id => name;

  String get label => switch (this) {
    PurchaseDocStatus.draft => 'Draft',
    PurchaseDocStatus.submitted => 'Submitted',
    PurchaseDocStatus.approved => 'Approved',
    PurchaseDocStatus.ordered => 'Ordered',
    PurchaseDocStatus.partial => 'Partial',
    PurchaseDocStatus.received => 'Received',
    PurchaseDocStatus.cancelled => 'Cancelled',
  };

  static PurchaseDocStatus parse(String? raw) {
    final v = (raw ?? '').trim().toLowerCase();
    return PurchaseDocStatus.values.firstWhere(
      (e) => e.name == v || e.id == v,
      orElse: () => PurchaseDocStatus.draft,
    );
  }
}

class PurchaseLine {
  const PurchaseLine({
    required this.productId,
    required this.productName,
    required this.quantity,
    this.receivedQty = 0,
    this.unitCost = 0,
    this.unit = '',
    this.note = '',
  });

  final int productId;
  final String productName;
  final double quantity;
  final double receivedQty;
  final double unitCost;
  final String unit;
  final String note;

  double get lineTotal => quantity * unitCost;

  double get pendingQty {
    final left = quantity - receivedQty;
    return left < 0 ? 0 : left;
  }

  PurchaseLine copyWith({
    int? productId,
    String? productName,
    double? quantity,
    double? receivedQty,
    double? unitCost,
    String? unit,
    String? note,
  }) {
    return PurchaseLine(
      productId: productId ?? this.productId,
      productName: productName ?? this.productName,
      quantity: quantity ?? this.quantity,
      receivedQty: receivedQty ?? this.receivedQty,
      unitCost: unitCost ?? this.unitCost,
      unit: unit ?? this.unit,
      note: note ?? this.note,
    );
  }

  Map<String, dynamic> toJson() => {
    'productId': productId,
    'productName': productName,
    'quantity': quantity,
    'receivedQty': receivedQty,
    'unitCost': unitCost,
    'unit': unit,
    'note': note,
  };

  factory PurchaseLine.fromJson(Map<String, dynamic> json) {
    double numOf(Object? v) {
      if (v is num) return v.toDouble();
      return double.tryParse('$v') ?? 0;
    }

    return PurchaseLine(
      productId: int.tryParse('${json['productId']}') ?? 0,
      productName: '${json['productName'] ?? ''}',
      quantity: numOf(json['quantity']),
      receivedQty: numOf(json['receivedQty']),
      unitCost: numOf(json['unitCost']),
      unit: '${json['unit'] ?? ''}',
      note: '${json['note'] ?? ''}',
    );
  }
}

class PurchaseDocument {
  const PurchaseDocument({
    required this.id,
    required this.docNo,
    required this.type,
    required this.status,
    required this.vendorId,
    required this.vendorName,
    this.lines = const [],
    this.notes = '',
    this.referenceNo = '',
    this.parentDocId = '',
    this.organizationId = '',
    this.branchId = '',
    this.createdBy = '',
    this.createdAt,
    this.updatedAt,
    this.receivedAt,
  });

  final String id;
  final String docNo;
  final PurchaseDocType type;
  final PurchaseDocStatus status;
  final String vendorId;
  final String vendorName;
  final List<PurchaseLine> lines;
  final String notes;
  final String referenceNo;
  final String parentDocId;
  final String organizationId;
  final String branchId;
  final String createdBy;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final DateTime? receivedAt;

  double get subTotal {
    var t = 0.0;
    for (final line in lines) {
      t += line.lineTotal;
    }
    return double.parse(t.toStringAsFixed(2));
  }

  int get itemCount => lines.length;

  bool get canReceive =>
      type == PurchaseDocType.order &&
      (status == PurchaseDocStatus.ordered ||
          status == PurchaseDocStatus.approved ||
          status == PurchaseDocStatus.partial ||
          status == PurchaseDocStatus.draft);

  PurchaseDocument copyWith({
    String? id,
    String? docNo,
    PurchaseDocType? type,
    PurchaseDocStatus? status,
    String? vendorId,
    String? vendorName,
    List<PurchaseLine>? lines,
    String? notes,
    String? referenceNo,
    String? parentDocId,
    String? organizationId,
    String? branchId,
    String? createdBy,
    DateTime? createdAt,
    DateTime? updatedAt,
    DateTime? receivedAt,
  }) {
    return PurchaseDocument(
      id: id ?? this.id,
      docNo: docNo ?? this.docNo,
      type: type ?? this.type,
      status: status ?? this.status,
      vendorId: vendorId ?? this.vendorId,
      vendorName: vendorName ?? this.vendorName,
      lines: lines ?? this.lines,
      notes: notes ?? this.notes,
      referenceNo: referenceNo ?? this.referenceNo,
      parentDocId: parentDocId ?? this.parentDocId,
      organizationId: organizationId ?? this.organizationId,
      branchId: branchId ?? this.branchId,
      createdBy: createdBy ?? this.createdBy,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      receivedAt: receivedAt ?? this.receivedAt,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'docNo': docNo,
    'type': type.id,
    'status': status.id,
    'vendorId': vendorId,
    'vendorName': vendorName,
    'lines': lines.map((e) => e.toJson()).toList(),
    'notes': notes,
    'referenceNo': referenceNo,
    'parentDocId': parentDocId,
    'organizationId': organizationId,
    'branchId': branchId,
    'createdBy': createdBy,
    'createdAt': createdAt?.toIso8601String(),
    'updatedAt': updatedAt?.toIso8601String(),
    'receivedAt': receivedAt?.toIso8601String(),
  };

  factory PurchaseDocument.fromJson(Map<String, dynamic> json) {
    final rawLines = json['lines'];
    final lines = <PurchaseLine>[];
    if (rawLines is List) {
      for (final row in rawLines) {
        if (row is Map) {
          lines.add(PurchaseLine.fromJson(Map<String, dynamic>.from(row)));
        }
      }
    }
    return PurchaseDocument(
      id: '${json['id'] ?? ''}',
      docNo: '${json['docNo'] ?? ''}',
      type: PurchaseDocTypeX.parse('${json['type']}'),
      status: PurchaseDocStatusX.parse('${json['status']}'),
      vendorId: '${json['vendorId'] ?? ''}',
      vendorName: '${json['vendorName'] ?? ''}',
      lines: lines,
      notes: '${json['notes'] ?? ''}',
      referenceNo: '${json['referenceNo'] ?? ''}',
      parentDocId: '${json['parentDocId'] ?? ''}',
      organizationId: '${json['organizationId'] ?? ''}',
      branchId: '${json['branchId'] ?? ''}',
      createdBy: '${json['createdBy'] ?? ''}',
      createdAt: DateTime.tryParse('${json['createdAt'] ?? ''}'),
      updatedAt: DateTime.tryParse('${json['updatedAt'] ?? ''}'),
      receivedAt: DateTime.tryParse('${json['receivedAt'] ?? ''}'),
    );
  }
}
