import 'package:pos_billingwala_v2/core/database/app_database.dart';

/* Fake bill / KOT used by printer Settings → Test and invoice preview. */
/* */
/* User-entered fields (product / customer / address) intentionally mix */
/* English + Marathi + Hindi (+ Tamil on one line) so thermal print can be */
/* verified for any language the merchant types. App UI translation is */
/* separate — these strings are sample *data*, not LocaleCatalog keys. */
class SampleReceiptData {
  SampleReceiptData._();

  static const invoiceNumber = 'POS-TEST';

  /* Multilingual sample for invoice preview + test print (same bytes path */
  /* on Android / iOS / web via ReceiptRasterizer). */
  static ({Invoice invoice, List<InvoiceItem> items}) sampleBill({
    DateTime? now,
  }) {
    final at = now ?? DateTime.now();
    const items = <InvoiceItem>[
      InvoiceItem(
        invoiceItemId: -1,
        invoiceNumber: invoiceNumber,
        productName: 'मिसळ पाव (हाफ)',
        productPrice: 80,
        productQuantity: 1,
        productCgst: 0,
        productSgst: 0,
        invoiceItemType: 'product',
        productStatus: 'completed',
        invoiceItemSyncStatus: '0',
        organizationId: '',
        branchId: '',
        deviceId: '',
      ),
      InvoiceItem(
        invoiceItemId: -2,
        invoiceNumber: invoiceNumber,
        productName: 'Masala Dosa',
        productPrice: 120,
        productQuantity: 1,
        productCgst: 0,
        productSgst: 0,
        invoiceItemType: 'product',
        productStatus: 'completed',
        invoiceItemSyncStatus: '0',
        organizationId: '',
        branchId: '',
        deviceId: '',
      ),
      InvoiceItem(
        invoiceItemId: -3,
        invoiceNumber: invoiceNumber,
        productName: 'छोले भटूरे',
        productPrice: 100,
        productQuantity: 2,
        productCgst: 0,
        productSgst: 0,
        invoiceItemType: 'product',
        productStatus: 'completed',
        invoiceItemSyncStatus: '0',
        organizationId: '',
        branchId: '',
        deviceId: '',
      ),
      InvoiceItem(
        invoiceItemId: -4,
        invoiceNumber: invoiceNumber,
        productName: 'Filter Coffee / फिल्टर कॉफी',
        productPrice: 40,
        productQuantity: 1,
        productCgst: 0,
        productSgst: 0,
        invoiceItemType: 'product',
        productStatus: 'completed',
        invoiceItemSyncStatus: '0',
        organizationId: '',
        branchId: '',
        deviceId: '',
      ),
    ];
    /* 80 + 120 + 200 + 40 = 440 */
    const subTotal = 440.0;
    final invoice = Invoice(
      invoiceId: -1,
      invoiceNumber: invoiceNumber,
      invoiceDate: at,
      invoiceType: 'fast_billing',
      subTotal: subTotal,
      totalGstAmount: 0,
      discount: 0,
      discountType: 'Amount',
      packingCharge: 0,
      packingChargeType: 'Amount',
      totalAmount: subTotal,
      paymentMode: 'Cash',
      cashAmount: subTotal,
      upiAmount: 0,
      invoiceOrderStatus: 'completed',
      invoiceNetworkStatus: 'test-preview',
      invoiceSyncStatus: '0',
      noOfTable: '1',
      customerName: 'नमस्ते · Hello · ஆதித்யா',
      customerMobile: '9999999999',
      customerAddress: 'पुणे / Pune · मुख्य रस्ता',
      billPrintStatus: '',
      createdByStaffName: 'Staff Demo',
      itemCount: items.length,
      createdAt: at,
      organizationId: '',
      branchId: '',
      deviceId: '',
    );
    return (invoice: invoice, items: items);
  }

  /* Demo shop header when session shop name is empty — user-entered style. */
  static const demoShopName = 'श्री गणेश स्टोअर · Shree Ganesh Store';

  static KotTicket sampleKot({DateTime? now, String prefix = ''}) {
    final at = now ?? DateTime.now();
    final p = prefix.trim();
    final kotNumber = p.isEmpty ? 'KOT-TEST' : '$p-TEST';
    return KotTicket(
      kot: Kot(
        kotId: -1,
        sessionId: -1,
        orderRoundId: -1,
        kotNumber: kotNumber,
        tableNumber: '1',
        printStatus: 'pending',
        kitchenName: 'Main Kitchen / मुख्य किचन',
        createdAt: at,
        organizationId: '',
        branchId: '',
        deviceId: '',
      ),
      roundNumber: 1,
      items: const [
        KotItem(
          kotItemId: -1,
          kotId: -1,
          productName: 'मिसळ पाव (हाफ)',
          productQuantity: 1,
          portionName: 'हाफ',
          organizationId: '',
          branchId: '',
          deviceId: '',
        ),
        KotItem(
          kotItemId: -2,
          kotId: -1,
          productName: 'Masala Dosa',
          productQuantity: 1,
          portionName: 'Full',
          organizationId: '',
          branchId: '',
          deviceId: '',
        ),
        KotItem(
          kotItemId: -3,
          kotId: -1,
          productName: 'छोले भटूरे',
          productQuantity: 2,
          portionName: 'Plate',
          organizationId: '',
          branchId: '',
          deviceId: '',
        ),
      ],
    );
  }
}
