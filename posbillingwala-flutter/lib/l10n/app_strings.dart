import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/l10n/ui_catalog.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Persisted app language (en / hi / mr). Applies without restart.
class AppLocaleController extends Notifier<Locale> {
  static const prefsKey = 'appLanguage';

  @override
  Locale build() {
    // Sync bootstrap; prefs load happens in [hydrate].
    Future.microtask(hydrate);
    return const Locale('en');
  }

  Future<void> hydrate() async {
    final prefs = await SharedPreferences.getInstance();
    state = _fromCode(prefs.getString(prefsKey));
  }

  Future<void> setLanguage(String code) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(prefsKey, code);
    state = _fromCode(code);
  }

  Locale _fromCode(String? code) {
    switch (code) {
      case 'hi':
        return const Locale('hi');
      case 'mr':
        return const Locale('mr');
      default:
        return const Locale('en');
    }
  }
}

final appLocaleProvider =
    NotifierProvider<AppLocaleController, Locale>(AppLocaleController.new);

/// Lightweight EN/HI/MR strings for hub surfaces (Material widgets still use locale).
class AppStrings {
  AppStrings(this.locale);

  final Locale locale;

  static AppStrings of(WidgetRef ref) =>
      AppStrings(ref.watch(appLocaleProvider));

  String get _code => locale.languageCode;

  String ui(String key) => UiCatalog.get(_code, key);

  String speechLocaleId() {
    switch (_code) {
      case 'hi':
        return 'hi_IN';
      case 'mr':
        return 'mr_IN';
      default:
        return 'en_IN';
    }
  }

  String _t(String en, String hi, String mr) {
    switch (_code) {
      case 'hi':
        return hi;
      case 'mr':
        return mr;
      default:
        return en;
    }
  }

  String get settings => _t('Settings', 'सेटिंग्स', 'सेटिंग्ज');
  String get language => _t('Language', 'भाषा', 'भाषा');
  String get languageApplied =>
      _t('Language updated', 'भाषा अपडेट हुई', 'भाषा अपडेट झाली');
  String get reports => _t('Reports', 'रिपोर्ट्स', 'अहवाल');
  String get masterData =>
      _t('Master Data', 'मास्टर डेटा', 'मास्टर डेटा');
  String get support => _t('Support', 'सपोर्ट', 'सपोर्ट');
  String get sync => _t('Sync', 'सिंक', 'सिंक');
  String get customer => _t('Customer', 'ग्राहक', 'ग्राहक');
  String get customerName => _t('Name', 'नाम', 'नाव');
  String get customerMobile => _t('Mobile', 'मोबाइल', 'मोबाइल');
  String get customerEmail => _t('Email', 'ईमेल', 'ईमेल');
  String get customerAddress => _t('Address', 'पता', 'पत्ता');
  String get clearCart =>
      _t('Clear Cart', 'कार्ट साफ़ करें', 'कार्ट साफ करा');
  String get invoicePreview =>
      _t('Invoice Preview', 'इनवॉइस पूर्वावलोकन', 'इनव्हॉइस पूर्वावलोकन');
  String get attachOptional =>
      _t('Add attachment (optional)', 'अटैचमेंट जोड़ें (वैकल्पिक)', 'अटॅचमेंट जोडा (पर्यायी)');
  String get paper2Inch => _t('2-Inch', '2-इंच', '२-इंच');
  String get paper3Inch => _t('3-Inch', '3-इंच', '३-इंच');
  String get testPrint =>
      _t('Test print', 'टेस्ट प्रिंट', 'टेस्ट प्रिंट');
  String get combos => _t('Combos', 'कॉम्बो', 'कॉम्बो');
  String get salesOverview =>
      _t('Sales Overview', 'सेल्स ओवरव्यू', 'सेल्स आढावा');
  String get salesList => _t('Sales List', 'सेल्स लिस्ट', 'सेल्स यादी');
  String get duplicatePrint =>
      _t('Duplicate print', 'डुप्लिकेट प्रिंट', 'डुप्लिकेट प्रिंट');
  String get saveWithoutPrint => _t(
        'Save without print',
        'प्रिंट के बिना सेव',
        'प्रिंट न करता सेव्ह',
      );
  String get guestsWaiter =>
      _t('Guests / Waiter', 'मेहमान / वेटर', 'पाहुणे / वेटर');
  String get shareBillImage =>
      _t('Share bill image', 'बिल इमेज शेयर करें', 'बिल इमेज शेअर करा');
  String get shareBillText =>
      _t('Share bill text', 'बिल टेक्स्ट शेयर करें', 'बिल मजकूर शेअर करा');
  String get addInventory =>
      _t('Add Inventory', 'इन्वेंटरी जोड़ें', 'इन्व्हेंटरी जोडा');
  String get memberPaymentReport => _t(
        'Invoice Member Payment Report',
        'सदस्य पेमेंट रिपोर्ट',
        'सदस्य पेमेंट अहवाल',
      );
  String get dineInTables =>
      _t('Dine-in Tables', 'डाइन-इन टेबल', 'डाइन-इन टेबल');
  String get saveAndShare =>
      _t('Save and share', 'सेव और शेयर', 'सेव्ह आणि शेअर');
  String get welcomeBack => _t('Welcome Back', 'वापसी पर स्वागत है', 'परत स्वागत आहे');
  String get signInSubtitle => _t(
        'Sign in to your POS Billingwala account.',
        'अपने POS Billingwala खाते में साइन इन करें।',
        'आपल्या POS Billingwala खात्यात साइन इन करा.',
      );
  String get licenceKey => _t('Licence key', 'लायसन्स की', 'लायसन्स की');
  String get licenceKeyHint => _t(
        'Enter your shop licence key',
        'अपनी दुकान की लायसन्स की दर्ज करें',
        'आपली दुकान लायसन्स की टाका',
      );
  String get licenceRequired =>
      _t('Licence key is required', 'लायसन्स की आवश्यक है', 'लायसन्स की आवश्यक आहे');
  String get forgotLicence =>
      _t('Forgot Licence key?', 'लायसन्स की भूल गए?', 'लायसन्स की विसरलात?');
  String get login => _t('Login', 'लॉगिन', 'लॉगिन');
  String get pleaseWait => _t('Please wait…', 'कृपया प्रतीक्षा करें…', 'कृपया थांबा…');
  String get newUserTrial => _t(
        'New user? Create free account',
        'नए उपयोगकर्ता? मुफ्त खाता बनाएं',
        'नवीन वापरकर्ता? मोफत खाते तयार करा',
      );
  String get startFreeTrial =>
      _t('Start Free Trial', 'फ्री ट्रायल शुरू करें', 'फ्री ट्रायल सुरू करा');
  String get close => _t('Close', 'बंद करें', 'बंद करा');
  String get fastBilling => _t('Fast Billing', 'फास्ट बिलिंग', 'फास्ट बिलिंग');
  String get dineIn => _t('Dine In', 'डाइन इन', 'डाइन इन');
  String get takeAway => _t('Take Away', 'टेक अवे', 'टेक अवे');
  String get mess => _t('Mess', 'मेस', 'मेस');
  String get catalog => _t('Catalog', 'कैटलॉग', 'कॅटलॉग');
  String get categories => _t('Categories', 'श्रेणियाँ', 'श्रेणी');
  String get subcategories => _t('Subcategories', 'उपश्रेणियाँ', 'उपश्रेणी');
  String get products => _t('Products', 'प्रोडक्ट्स', 'प्रॉडक्ट्स');
  String get portions => _t('Portions', 'पोर्शन', 'पोर्शन');
  String get tableMaster => _t('Table Master', 'टेबल मास्टर', 'टेबल मास्टर');
  String get today => _t('Today', 'आज', 'आज');
  String get month => _t('Month', 'महीना', 'महिना');
  String get todaySales => _t("Today's Sales", 'आज की सेल्स', 'आजची सेल्स');
  String get invoiceDetails =>
      _t('Invoice Details', 'इनवॉइस डिटेल्स', 'इनव्हॉइस डिटेल्स');
  String get shopDetails => _t('Shop Details', 'दुकान विवरण', 'दुकान तपशील');
  String get printerDetails =>
      _t('Printer Details', 'प्रिंटर विवरण', 'प्रिंटर तपशील');
  String get businessHours =>
      _t('Business Hours', 'व्यापार समय', 'व्यवसाय वेळ');
  String get inventory => _t('Inventory', 'इन्वेंटरी', 'इन्व्हेंटरी');
  String get expenses => _t('Expenses', 'खर्च', 'खर्च');
  String get helpSupport => _t('Help & Support', 'हेल्प और सपोर्ट', 'मदत आणि सपोर्ट');
  String get about => _t('About', 'अबाउट', 'अबाउट');
  String get shareApp => _t('Share App', 'ऐप शेयर करें', 'अॅप शेअर करा');
  String get billingCatalog =>
      _t('Billing & Catalog', 'बिलिंग और कैटलॉग', 'बिलिंग आणि कॅटलॉग');
  String get store => _t('Store', 'स्टोर', 'स्टोअर');
  String get cloudApp => _t('Cloud & App', 'क्लाउड और ऐप', 'क्लाउड आणि अॅप');
  String get account => _t('Account', 'खाता', 'खाते');
  String get salesDashboard =>
      _t('Sales Dashboard', 'सेल्स डैशबोर्ड', 'सेल्स डॅशबोर्ड');
  String get invoiceReport =>
      _t('Invoice Report', 'इनवॉइस रिपोर्ट', 'इनव्हॉइस अहवाल');
  String get print => _t('Print', 'प्रिंट', 'प्रिंट');
  String get tableActions =>
      _t('Table actions', 'टेबल क्रियाएँ', 'टेबल क्रिया');
  String get cancel => ui('cancel');
  String get productMenu => ui('ui_product_menu');
  String get saleReports => ui('ui_sale_reports');
  String get saleWiseReport => ui('ui_sale_wise_report');
  String get invoiceTableReport => ui('ui_invoice_table_report');
  String get invoiceTakeAwayReport => ui('ui_invoice_take_away_report');
  String get invoicePaymentModeReport => ui('ui_invoice_payment_mode_report');
  String get discountWiseReport => ui('discount_wise_report');
  String get refundWiseReport => ui('refund_wise_report');
  String get productWiseReport => ui('ui_product_wise_report');
  String get comboWiseReport => ui('ui_combo_wise_report');
  String get expenseWiseReport => ui('ui_expense_wise_report');
  String get invoiceMemberReport => ui('ui_invoice_member_report');
  String get invoiceMessReport => ui('ui_invoice_mess_report');
  String get thisBranchOnly => ui('ui_this_branch_only');
  String get reportDetail => ui('ui_report_detail');
  String get salesAndAnalytics => ui('ui_sales_and_reports');
  String get operationalReports => ui('ui_operational_reports');
  String get dataManagement => ui('ui_data_management');
  String get deleteAllInvoice => ui('ui_delete_all_invoice_सर्व_बले_हटव')
      .replaceAll('\n', ' ');
  String get deleteAllInvoicesHint => ui('ui_delete_all_invoices_hint');
  String get noDataFound => ui('ui_no_data_found');
  String get addToCart => ui('ui_add_to_cart');
  String get addCustomer => ui('ui_add_customer');
  String get addDiscount => ui('ui_add_discount');
  String get addPacking => ui('ui_add_packing');
  String get addMember => ui('ui_add_member');
  String get addPayment => ui('ui_add_payment');
  String get addProduct => ui('ui_add_product');
  String get addItems => ui('ui_add_items');
  String get cash => ui('ui_cash');
  String get upi => 'UPI';
  String get paymentMode => ui('ui_payment_mode');
  String get memberList => ui('ui_member_list');
  String get expense => ui('ui_expense');
  String get invoiceReports => ui('ui_invoice_reports');
  String get productList => ui('ui_product_list');
  String get categoryList => _t('Category List', 'श्रेणी सूची', 'श्रेणी यादी');
  String get comboMaster => _t('Combo Master', 'कॉम्बो मास्टर', 'कॉम्बो मास्टर');
  String get mealSessions => _t('Meal Sessions', 'मील सेशन', 'मील सेशन');
  String get scanMessToken => _t('Scan Mess Token', 'मेस टोकन स्कैन', 'मेस टोकन स्कॅन');
  String get paperMessCoupon => ui('ui_mess_coupon');
  String get helpSupportTitle => helpSupport;
  String get aboutUs => _t('About Us', 'हमारे बारे में', 'आमच्याबद्दल');
  String get changePin => ui('ui_change_app_login_pbpin_अँपच_पन_टकबदल');
  String get notifications => _t('Notifications', 'सूचनाएँ', 'सूचना');
  String get splitBill => _t('Split Bill', 'बिल स्प्लिट', 'बिल स्प्लिट');
  String get clearCartConfirm =>
      _t('Remove all items from this bill?', 'इस बिल की सभी वस्तुएँ हटाएँ?', 'या बिलातील सर्व वस्तू काढा?');
  String get billSaved => _t('Bill saved', 'बिल सेव हुआ', 'बिल सेव्ह झाले');
  String get printShare => _t('Print / Share', 'प्रिंट / शेयर', 'प्रिंट / शेअर');
  String get home => _t('Home', 'होम', 'होम');
  String get cartEmptyPay =>
      _t('Cart is empty. Add products before payment.', 'कार्ट खाली है। भुगतान से पहले प्रोडक्ट जोड़ें।', 'कार्ट रिकामी आहे. पेमेंटपूर्वी प्रॉडक्ट जोडा.');
  String get noItems => _t('No items', 'कोई आइटम नहीं', 'आयटम नाहीत');
  String get noCategoriesFound =>
      ui('ui_no_category_found_please_add_new_categor');
  String get noCombosFound => ui('empty_sub_combos');
  String get noBillsPeriod => ui('empty_sub_sales');
  String get billsAndLines =>
      _t('Bills and product lines', 'बिल और प्रोडक्ट लाइनें', 'बिले आणि प्रॉडक्ट ओळी');
  String get syncPendingBills => _t(
        'Sync pending bill(s) first.',
        'पहले पेंडिंग बिल सिंक करें।',
        'आधी पेंडिंग बिले सिंक करा.',
      );
  String get allInvoicesCleared =>
      _t('All invoices cleared', 'सभी इनवॉइस हटाए गए', 'सर्व इनव्हॉइस काढले');
  String get viewBill => _t('View Bill', 'बिल देखें', 'बिल पहा');
  String get settleBill => _t('Settle Bill', 'बिल सेटल', 'बिल सेटल');
  String get more => _t('More…', 'और…', 'आणखी…');
  String get openBilling => _t('Open billing', 'बिलिंग खोलें', 'बिलिंग उघडा');
  String get addItemsExisting => ui('ui_add_items');
  String get joinTable => _t('Join with another table', 'दूसरे टेबल से जोड़ें', 'दुसऱ्या टेबलशी जोडा');
  String get transferTable =>
      _t('Transfer to another table', 'दूसरे टेबल पर ट्रांसफर', 'दुसऱ्या टेबलवर ट्रान्सफर');
  String get moveItemsTable =>
      _t('Move items to another table', 'आइटम दूसरे टेबल पर भेजें', 'आयटम दुसऱ्या टेबलवर हलवा');
  String get holdTable => _t('Hold table', 'टेबल होल्ड', 'टेबल होल्ड');
  String get resumeTable => _t('Resume table', 'टेबल रिज्यूम', 'टेबल रिज्यूम');
  String get markBillRequested =>
      _t('Mark bill requested', 'बिल रिक्वेस्टेड', 'बिल विनंती');
  String get splitJoined =>
      _t('Split joined tables', 'जुड़े टेबल स्प्लिट', 'जोडलेली टेबल स्प्लिट');
  String get moreActions => _t('More actions', 'और क्रियाएँ', 'आणखी क्रिया');
  String get editInvoice => _t('Edit Invoice', 'इनवॉइस संपादित करें', 'इनव्हॉइस संपादित करा');
  String get billDetails => _t('Bill details', 'बिल विवरण', 'बिल तपशील');
  String get dayWise => ui('ui_day_wise');
  String get monthWise => ui('ui_month_wise');
  String get viewAll => ui('ui_view_all');
  String get noExpenseFound => ui('ui_no_expense_found');
  String get noInventoryFound => ui('ui_no_inventory_found');
  String get newParcel => _t('New parcel', 'नया पार्सल', 'नवे पार्सल');
  String get tableNo => _t('Table', 'टेबल', 'टेबल');
  String get addProducts => _t('Add products', 'प्रोडक्ट जोड़ें', 'प्रॉडक्ट जोडा');
  String get dismiss => ui('ui_dismiss');
  String get quantity => ui('ui_quantity');
  String get gst => ui('ui_gst');
  String get proceedToPayment =>
      _t('Proceed to Payment', 'भुगतान करें', 'पेमेंट करा');
  String get cartIsEmpty =>
      _t('Cart is empty', 'कार्ट खाली है', 'कार्ट रिकामी आहे');
  String get cartEmptyHint => _t(
        'Tap products to add them to the current bill.',
        'बिल में जोड़ने के लिए प्रोडक्ट टैप करें।',
        'बिलात जोडण्यासाठी प्रॉडक्ट टॅप करा.',
      );
  String get noProductsYet =>
      _t('No products yet', 'अभी कोई प्रोडक्ट नहीं', 'अजून प्रॉडक्ट नाहीत');
  String get noProductsCategory => _t(
        'No products in this category',
        'इस श्रेणी में प्रोडक्ट नहीं',
        'या श्रेणीत प्रॉडक्ट नाहीत',
      );
  String get speechUnavailable => _t(
        'Speech recognition unavailable',
        'वॉइस सर्च उपलब्ध नहीं',
        'व्हॉइस सर्च उपलब्ध नाही',
      );
  String get openTableFirst =>
      _t('Open a table first', 'पहले टेबल खोलें', 'आधी टेबल उघडा');
  String get sendKot => _t('Send KOT', 'KOT भेजें', 'KOT पाठवा');
  String get kotUpToDate =>
      _t('KOT up to date', 'KOT अप टू डेट', 'KOT अप टू डेट');
  String get items => _t('Items', 'आइटम', 'आयटम');
  String get subtotal => ui('ui_sub_total');
  String get grandTotal =>
      _t('Grand Total', 'कुल योग', 'एकूण रक्कम');
  String get reviewOrder => _t(
        'Review your order before billing',
        'बिलिंग से पहले ऑर्डर जाँचें',
        'बिलिंगपूर्वी ऑर्डर तपासा',
      );
  String get settlementNotMatched => ui('ui_settlement_not_matched');
  String get totalSettlement => ui('ui_total_settlement');
  String get cashAmount => ui('ui_cash_amount');
  String get upiAmount => ui('ui_upi_amount');
  String get billNotFound =>
      _t('Bill not found', 'बिल नहीं मिला', 'बिल सापडले नाही');
  String get voided => _t('Voided', 'रद्द', 'रद्द');
  String get refunded => _t('Refunded', 'रिफंड', 'रिफंड');
  String get add => _t('Add', 'जोड़ें', 'जोडा');
  String get editItem => _t('Edit item', 'आइटम संपादित करें', 'आयटम संपादित करा');
  String get selectPortion =>
      _t('Select portion', 'पोर्शन चुनें', 'पोर्शन निवडा');
  String get billHeader =>
      _t('Bill header', 'बिल हेडर', 'बिल हेडर');
  String get noPaymentsYet => ui('empty_sub_member_payments');
  String get institutePays =>
      _t('Institute pays', 'संस्थान भुगतान', 'संस्था पेमेंट');
  String get walkInToken => _t(
        'Walk-in Mess Token',
        'वॉक-इन मेस टोकन',
        'वॉक-इन मेस टोकन',
      );
  String get noTokensToday => _t(
        'No tokens issued today',
        'आज कोई टोकन नहीं',
        'आज टोकन नाहीत',
      );
  String get cancelToken =>
      _t('Cancel token', 'टोकन रद्द करें', 'टोकन रद्द करा');
  String get active => _t('Active', 'सक्रिय', 'सक्रिय');
  String get allPayments =>
      _t('All payments', 'सभी भुगतान', 'सर्व पेमेंट्स');
  String get noMessMembers =>
      _t('No mess members found', 'मेस सदस्य नहीं मिले', 'मेस सदस्य नाहीत');
  String get tableList => _t('Table List', 'टेबल सूची', 'टेबल यादी');
  String get yearWise => _t('Year Wise', 'वर्ष अनुसार', 'वर्षानुसार');
  String get noData => ui('ui_no_data_found');
  String get invoiceNotFound =>
      _t('Invoice not found', 'इनवॉइस नहीं मिला', 'इनव्हॉइस सापडले नाही');
  String get printed => _t('Printed', 'प्रिंट हुआ', 'प्रिंट झाले');
  String get shared => _t('Shared', 'शेयर हुआ', 'शेअर झाले');
  String get listening => _t('Listening…', 'सुन रहा है…', 'ऐकत आहे…');
  String get voiceSearch => _t('Voice search', 'वॉइस सर्च', 'व्हॉइस सर्च');
  String get removeItem => _t('Remove item', 'आइटम हटाएँ', 'आयटम काढा');
  String get regular => _t('Regular', 'रेगुलर', 'रेगुलर');
  String get enterValidPrice =>
      _t('Enter a valid price', 'सही कीमत दर्ज करें', 'योग्य किंमत टाका');
  String get openSalesList =>
      _t('Open sales list', 'सेल्स लिस्ट खोलें', 'सेल्स यादी उघडा');
  String get noTableBills => _t(
        'No table bills for this filter',
        'इस फ़िल्टर में टेबल बिल नहीं',
        'या फिल्टरमध्ये टेबल बिले नाहीत',
      );
  String get memberUpdated =>
      _t('Member updated', 'सदस्य अपडेट हुआ', 'सदस्य अपडेट झाला');
  String get nameRequired =>
      _t('Name is required', 'नाम आवश्यक है', 'नाव आवश्यक आहे');
  String get memberSaved =>
      _t('Member saved locally', 'सदस्य सेव हुआ', 'सदस्य सेव्ह झाला');
  String get tokenNotFound =>
      _t('Token not found', 'टोकन नहीं मिला', 'टोकन सापडले नाही');
  String get qrSaved =>
      _t('QR saved to gallery', 'QR गैलरी में सेव', 'QR गॅलरीत सेव्ह');
  String get addAMemberFirst => _t(
        'Add a mess member first',
        'पहले मेस सदस्य जोड़ें',
        'आधी मेस सदस्य जोडा',
      );
  String get alreadyPaidMonth => _t(
        'Already paid for this month',
        'इस महीने भुगतान हो चुका है',
        'या महिन्याचे पेमेंट झाले आहे',
      );
  String get paymentUpdated =>
      _t('Payment updated', 'भुगतान अपडेट हुआ', 'पेमेंट अपडेट झाले');
  String get noQrTokensToday => _t(
        'No QR meal tokens today',
        'आज QR मील टोकन नहीं',
        'आज QR मील टोकन नाहीत',
      );
  String get kotCopied => _t('KOT copied', 'KOT कॉपी हुआ', 'KOT कॉपी झाले');
  String get incorrectReportPin => _t(
        'Incorrect Report PIN',
        'गलत रिपोर्ट PIN',
        'चुकीचा रिपोर्ट PIN',
      );
  String get voidedCannotEdit => _t(
        'Voided / refunded bills cannot be edited',
        'रद्द/रिफंड बिल संपादित नहीं हो सकते',
        'रद्द/रिफंड बिले संपादित करता येत नाहीत',
      );
  String get productPrice => ui('ui_product_price_2');
  String get productQuantity => ui('ui_product_quantity');
  String get product => ui('ui_product_2');
  String get save => ui('ui_save_details');
  String get unitPrice => _t('Unit price', 'यूनिट कीमत', 'युनिट किंमत');
  String get refundBill => _t('Refund bill', 'बिल रिफंड', 'बिल रिफंड');
  String get refundBillConfirm => _t(
        'Mark this bill as refunded? It will show in Refund Report and re-upload on sync.',
        'इस बिल को रिफंड करें? यह रिफंड रिपोर्ट में दिखेगा और सिंक पर दोबारा अपलोड होगा।',
        'हे बिल रिफंड करा? ते रिफंड अहवालात दिसेल आणि सिंकवर पुन्हा अपलोड होईल.',
      );
  String get refund => _t('Refund', 'रिफंड', 'रिफंड');
  String get voidBill => _t('Void bill', 'बिल रद्द करें', 'बिल रद्द करा');
  String get voidBillConfirm => _t(
        'Void this bill locally? It will be removed from sales and queued for cloud delete.',
        'इस बिल को स्थानीय रूप से रद्द करें? यह सेल्स से हटेगा और क्लाउड डिलीट कतार में जाएगा।',
        'हे बिल स्थानिकरित्या रद्द करा? ते सेल्समधून काढले जाईल आणि क्लाउड डिलीट रांगेत जाईल.',
      );
  String get voidAction => _t('Void', 'रद्द', 'रद्द');
  String get billRefunded =>
      _t('Bill marked refunded', 'बिल रिफंड हुआ', 'बिल रिफंड झाले');
  String get billVoided =>
      _t('Bill voided locally', 'बिल रद्द हुआ', 'बिल रद्द झाले');
  String get editCustomerPayment => _t(
        'Edit customer / payment',
        'ग्राहक / भुगतान संपादित करें',
        'ग्राहक / पेमेंट संपादित करा',
      );
  String get editCustomerDiscountPayment => _t(
        'Edit customer / discount / payment',
        'ग्राहक / छूट / भुगतान संपादित करें',
        'ग्राहक / सूट / पेमेंट संपादित करा',
      );
  String get uploadToCloud =>
      _t('Upload to cloud', 'क्लाउड पर अपलोड', 'क्लाउडवर अपलोड');
  String get noProductsCatalog => _t(
        'No products in catalog',
        'कैटलॉग में प्रोडक्ट नहीं',
        'कॅटलॉगमध्ये प्रॉडक्ट नाहीत',
      );
  String get itemAddedPending => _t(
        'Item added · pending sync',
        'आइटम जोड़ा · सिंक पेंडिंग',
        'आयटम जोडला · सिंक पेंडिंग',
      );
  String get billUpdatedPending => _t(
        'Bill updated · pending sync',
        'बिल अपडेट · सिंक पेंडिंग',
        'बिल अपडेट · सिंक पेंडिंग',
      );
  String get lineRemoved => _t('Line removed', 'लाइन हटाई गई', 'ओळ काढली');
  String get editBill => _t('Edit bill', 'बिल संपादित करें', 'बिल संपादित करा');
  String get discountType => ui('ui_discount_type');
  String get packingType => ui('ui_packing_type');
  String get packingCharge =>
      _t('Packing charge', 'पैकिंग चार्ज', 'पॅकिंग चार्ज');
  String get saveAndPrint =>
      _t('Save and print', 'सेव और प्रिंट', 'सेव्ह आणि प्रिंट');
  String get payment => ui('ui_payment_mode');
  String get packingLabel => ui('ui_packing');
  String get discountLabel => ui('ui_discount');
  String get pendingSync =>
      _t('Pending sync', 'सिंक पेंडिंग', 'सिंक पेंडिंग');
  String get synced => _t('Synced', 'सिंक हुआ', 'सिंक झाले');
  String get combo => _t('Combo', 'कॉम्बो', 'कॉम्बो');
  String get editLine =>
      _t('Edit line', 'लाइन संपादित करें', 'ओळ संपादित करा');
  String get deleteLine =>
      _t('Delete line', 'लाइन हटाएँ', 'ओळ काढा');
  String get syncFinished =>
      _t('Sync finished', 'सिंक पूरा', 'सिंक पूर्ण');
  String get posLabel => 'POS';
  String get customerNameField =>
      _t('Customer name', 'ग्राहक नाम', 'ग्राहक नाव');
  String get voidPendingBillConfirm => _t(
        'Mark this pending bill as cancelled?',
        'इस पेंडिंग बिल को रद्द करें?',
        'हे पेंडिंग बिल रद्द करा?',
      );
  String get syncTables =>
      _t('Sync tables', 'टेबल सिंक', 'टेबल सिंक');
  String get allAreas => _t('All', 'सभी', 'सर्व');
  String get tableAvailable =>
      _t('Available', 'उपलब्ध', 'उपलब्ध');
  String get tableRunning => _t('Running', 'रनिंग', 'रनिंग');
  String get tableHold => _t('Hold', 'होल्ड', 'होल्ड');
  String get tableBill => _t('Bill', 'बिल', 'बिल');
  String get tableBlocked => _t('Blocked', 'ब्लॉक्ड', 'ब्लॉक्ड');
  String get tableReserved =>
      _t('Reserved', 'रिज़र्व्ड', 'रिझर्व्ह्ड');
  String get longPressTableHint => _t(
        'Long press a table for more actions',
        'और क्रियाओं के लिए टेबल को देर तक दबाएँ',
        'अधिक क्रियांसाठी टेबल दीर्घ दाबा',
      );
  String get noTablesYet =>
      _t('No tables yet', 'अभी टेबल नहीं', 'अजून टेबल नाहीत');
  String get noTablesHint => _t(
        'Sync tables from cloud, or defaults will be created automatically.',
        'क्लाउड से टेबल सिंक करें, या डिफ़ॉल्ट अपने आप बनेंगे।',
        'क्लाउडवरून टेबल सिंक करा, किंवा डीफॉल्ट आपोआप तयार होतील.',
      );
}
