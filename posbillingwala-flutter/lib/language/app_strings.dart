import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/language/app_languages.dart';
import 'package:pos_billingwala_v2/language/locale_catalog.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Persisted app language. Applies without restart.
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
    final code = prefs.getString(prefsKey) ?? 'en';
    await LocaleCatalog.ensureLang(code);
    state = AppLanguages.localeFromCode(code);
  }

  Future<void> setLanguage(String code) async {
    await LocaleCatalog.ensureLang(code);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(prefsKey, code);
    state = AppLanguages.localeFromCode(code);
  }
}

final appLocaleProvider = NotifierProvider<AppLocaleController, Locale>(
  AppLocaleController.new,
);

// Lightweight UI strings loaded from assets/locale JSON files.
class AppStrings {
  AppStrings(this.locale);

  final Locale locale;

  static AppStrings of(WidgetRef ref) =>
      AppStrings(ref.watch(appLocaleProvider));

  String get appStringsCode => locale.languageCode;

  String tr(String key) => LocaleCatalog.get(appStringsCode, key);

  String ui(String key) => tr(key);

  String get about => tr('app_about');

  String get aboutUs => tr('app_about_us');

  String get account => tr('app_account');

  String get active => tr('app_active');

  String get add => tr('app_add');

  String get addAMemberFirst => tr('app_add_a_member_first');

  String get addCustomer => tr('ui_add_customer');

  String get addDiscount => tr('ui_add_discount');

  String get addInventory => tr('app_add_inventory');

  String get addItems => tr('ui_add_items');

  String get addItemsExisting => tr('ui_add_items');

  String get addMember => tr('ui_add_member');

  String get addPacking => tr('ui_add_packing');

  String get addPayment => tr('ui_add_payment');

  String get addProduct => tr('ui_add_product');

  String get addProducts => tr('app_add_products');

  String get addToCart => tr('ui_add_to_cart');

  String get allAreas => tr('app_all_areas');

  String get allInvoicesCleared => tr('app_all_invoices_cleared');

  String get allPayments => tr('app_all_payments');

  String get alreadyPaidMonth => tr('app_already_paid_month');

  String get appFailedToUpdate => tr('toast_app_failed_to_update');

  String get appUpdateNotAvailable => tr('toast_app_update_not_available');

  String get attachOptional => tr('app_attach_optional');

  String get billDetails => tr('app_bill_details');

  String get billHeader => tr('app_bill_header');

  String get billingCatalog => tr('app_billing_catalog');

  String get billNotFound => tr('app_bill_not_found');

  String get billRefunded => tr('app_bill_refunded');

  String get billRequested => tr('app_bill_requested');

  String get billsAndLines => tr('app_bills_and_lines');

  String get billSaved => tr('app_bill_saved');

  String get billUpdatedPending => tr('app_bill_updated_pending');

  String get billVoided => tr('app_bill_voided');

  String get businessHours => tr('app_business_hours');

  String get cancel => tr('cancel');

  String get cancelToken => tr('app_cancel_token');

  String get cartEmptyHint => tr('app_cart_empty_hint');

  String get cartEmptyPay => tr('app_cart_empty_pay');

  String get cartIsEmpty => tr('app_cart_is_empty');

  String get cash => tr('ui_cash');

  String get cashAmount => tr('ui_cash_amount');

  String get catalog => tr('app_catalog');

  String get categories => tr('app_categories');

  String get categoryList => tr('app_category_list');

  String get categorySaved => tr('app_category_saved');

  String get changePin =>
      tr('ui_change_app_login_pbpin_αñàαñüαñ¬αñÜ_αñ¬αñ¿_αñƒαñòαñ¼αñªαñ▓');

  String get clearCart => tr('app_clear_cart');

  String get clearCartConfirm => tr('app_clear_cart_confirm');

  String get close => tr('app_close');

  String get cloudApp => tr('app_cloud_app');

  String get combo => tr('app_combo');

  String get comboMaster => tr('app_combo_master');

  String get combos => tr('app_combos');

  String get comboWiseReport => tr('ui_combo_wise_report');

  String get customer => tr('app_customer');

  String get customerAddress => tr('app_customer_address');

  String get customerEmail => tr('app_customer_email');

  String get customerMobile => tr('app_customer_mobile');

  String get customerName => tr('app_customer_name');

  String get customerNameField => tr('app_customer_name_field');

  String get dataManagement => tr('ui_data_management');

  String get dataUploadingOnServer => tr('toast_data_uploading_on_server');

  String get dayWise => tr('ui_day_wise');

  String get deleteAllInvoice => tr('ui_delete_all_bills');

  String get deleteAllInvoicesHint => tr('ui_delete_all_invoices_hint');

  String get deleteLine => tr('app_delete_line');

  String get dineIn => tr('app_dine_in');

  String get dineInTables => tr('app_dine_in_tables');

  String get discountLabel => tr('ui_discount');

  String get discountType => tr('ui_discount_type');

  String get discountWiseReport => tr('discount_wise_report');

  String get dismiss => tr('ui_dismiss');

  String get duplicatePrint => tr('app_duplicate_print');

  String get duplicatePrintLastBill => tr('app_duplicate_print_last_bill');

  String get editBill => tr('app_edit_bill');

  String get editCustomerDiscountPayment =>
      tr('app_edit_customer_discount_payment');

  String get editCustomerPayment => tr('app_edit_customer_payment');

  String get editInvoice => tr('app_edit_invoice');

  String get editItem => tr('app_edit_item');

  String get editLine => tr('app_edit_line');

  String get enterCategoryName => tr('app_enter_category_name');

  String get enterValidPrice => tr('app_enter_valid_price');

  String get expense => tr('ui_expense');

  String get expenses => tr('app_expenses');

  String get expenseWiseReport => tr('ui_expense_wise_report');

  String get fastBilling => tr('app_fast_billing');

  String get forgotLicence => tr('app_forgot_licence');

  String get grandTotal => tr('app_grand_total');

  String get gst => tr('ui_gst');

  String get guestCount => tr('app_guest_count');

  String get guestsWaiter => tr('app_guests_waiter');

  String get helpSupport => tr('app_help_support');

  String get helpSupportTitle => helpSupport;

  String get holdTable => tr('app_hold_table');

  String get home => tr('app_home');

  String get incorrectReportPin => tr('app_incorrect_report_pin');

  String get institutePays => tr('app_institute_pays');

  String get inventory => tr('app_inventory');

  String get invoiceDetails => tr('app_invoice_details');

  String get invoiceMemberReport => tr('ui_invoice_member_report');

  String get invoiceMessReport => tr('ui_invoice_mess_report');

  String get invoiceNotFound => tr('app_invoice_not_found');

  String get invoicePaymentModeReport => tr('ui_invoice_payment_mode_report');

  String get invoicePreview => tr('app_invoice_preview');

  String get invoiceReport => tr('app_invoice_report');

  String get invoiceReports => tr('ui_invoice_reports');

  String get invoiceTableListReport => tr('app_invoice_table_list_report');

  String get invoiceTableReport => tr('ui_invoice_table_report');

  String get invoiceTakeAwayReport => tr('ui_invoice_take_away_report');

  String get itemAddedPending => tr('app_item_added_pending');

  String get items => tr('app_items');

  String get joinTable => tr('app_join_table');

  String get kotCopied => tr('app_kot_copied');

  String get kotPrinted => tr('app_kot_printed');

  String get kotUpToDate => tr('app_kot_up_to_date');

  String get language => tr('app_language');

  String get languageApplied => tr('app_language_applied');

  String get licenceKey => tr('app_licence_key');

  String get licenceKeyHint => tr('app_licence_key_hint');

  String get licenceRequired => tr('app_licence_required');

  String get lineRemoved => tr('app_line_removed');

  String get listening => tr('app_listening');

  String get login => tr('app_login');

  String get loginAsStaff => tr('app_login_as_staff');

  String get licenceLogin => tr('app_licence_login');

  String get licenceLoginHint => tr('app_licence_login_hint');

  String get longPressTableHint => tr('app_long_press_table_hint');

  String get markBillRequested => tr('app_mark_bill_requested');

  String get masterData => tr('app_master_data');

  String get mealSessions => tr('app_meal_sessions');

  String get memberList => tr('ui_member_list');

  String get memberPaymentReport => tr('app_member_payment_report');

  String get memberSaved => tr('app_member_saved');

  String get memberUpdated => tr('app_member_updated');

  String get mess => tr('app_mess');

  String get moduleLocked => tr('app_module_locked');

  String get month => tr('app_month');

  String get monthWise => tr('ui_month_wise');

  String get more => tr('app_more');

  String get moreActions => tr('app_more_actions');

  String get moveItems => tr('app_move_items');

  String get moveItemsTable => tr('app_move_items_table');

  String get nameRequired => tr('app_name_required');

  String get mobileRequired => tr('app_mobile_required');

  String get mobileNumber => tr('app_mobile_number');

  String get mobileNumberHint => tr('app_mobile_number_hint');

  String get staffLogin => tr('app_staff_login');

  String get staffLoginHint => tr('app_staff_login_hint');

  String get staffOwnerHint => tr('app_staff_owner_hint');

  String get staffNoLicenceHint => tr('app_staff_no_licence_hint');

  String get appPin => tr('app_app_pin');

  String get staffPinHint => tr('app_staff_pin_hint');

  String get staffPinRequired => tr('app_staff_pin_required');

  String get internetRequiredFooter => tr('app_internet_required_footer');

  String get dataSafeTerms => tr('app_data_safe_terms');

  String get tokenPrintNameMobileRequired =>
      tr('app_token_print_name_mobile_required');

  String get newParcel => tr('app_new_parcel');

  String get newUserTrial => tr('app_new_user_trial');

  String get newVersionAvailable => tr('toast_new_version_available');

  String get noBillsPeriod => tr('empty_sub_sales');

  String get noCategoriesFound =>
      tr('ui_no_category_found_please_add_new_categor');

  String get noCombosFound => tr('empty_sub_combos');

  String get noData => tr('ui_no_data_found');

  String get noDataFound => tr('ui_no_data_found');

  String get noExpenseFound => tr('ui_no_expense_found');

  String get noInventoryFound => tr('ui_no_inventory_found');

  String get noItems => tr('app_no_items');

  String get noMessMembers => tr('app_no_mess_members');

  String get noPaymentsYet => tr('empty_sub_member_payments');

  String get noPreviousBill => tr('app_no_previous_bill');

  String get noProductsCatalog => tr('app_no_products_catalog');

  String get noProductsCategory => tr('app_no_products_category');

  String get noProductsYet => tr('app_no_products_yet');

  String get noQrTokensToday => tr('app_no_qr_tokens_today');

  String get noTableBills => tr('app_no_table_bills');

  String get noTablesHint => tr('app_no_tables_hint');

  String get noTablesYet => tr('app_no_tables_yet');

  String get notifications => tr('app_notifications');

  String get noTokensToday => tr('app_no_tokens_today');

  String get openBilling => tr('app_open_billing');

  String get openSalesList => tr('app_open_sales_list');

  String get openTableFirst => tr('app_open_table_first');

  String get operationalReports => tr('ui_operational_reports');

  String get packingCharge => tr('app_packing_charge');

  String get packingLabel => tr('ui_packing');

  String get packingType => tr('ui_packing_type');

  String get paper2Inch => tr('app_paper2_inch');

  String get paper3Inch => tr('app_paper3_inch');

  String get paperMessCoupon => tr('ui_mess_coupon');

  String get payment => tr('ui_payment_mode');

  String get paymentMode => tr('ui_payment_mode');

  String get paymentUpdated => tr('app_payment_updated');

  String get paymentDisplayTitle => tr('app_payment_display_title');
  String get paymentDisplayStatus => tr('app_payment_display_status');
  String get paymentDisplayDevice => tr('app_payment_display_device');
  String get paymentDisplayLocalUrl => tr('app_payment_display_local_url');
  String get paymentDisplayConnect => tr('app_payment_display_connect');
  String get paymentDisplayShowPairingQr =>
      tr('app_payment_display_show_pairing_qr');
  String get paymentDisplayReconnect => tr('app_payment_display_reconnect');
  String get paymentDisplayDisconnect => tr('app_payment_display_disconnect');
  String get paymentDisplayStartServer =>
      tr('app_payment_display_start_server');
  String get paymentDisplayStopServer => tr('app_payment_display_stop_server');
  String get paymentDisplayAutoNewBills =>
      tr('app_payment_display_auto_new_bills');
  String get paymentDisplayAutoNewBillsHint =>
      tr('app_payment_display_auto_new_bills_hint');
  String get paymentDisplayQrDuration => tr('app_payment_display_qr_duration');
  String get paymentDisplayMinutes => tr('app_payment_display_minutes');
  String get paymentDisplayUpiSettings =>
      tr('app_payment_display_upi_settings');
  String get paymentDisplayPairHint => tr('app_payment_display_pair_hint');
  String get paymentDisplayCopyUrl => tr('app_payment_display_copy_url');
  String get paymentDisplayUrlCopied => tr('app_payment_display_url_copied');
  String get paymentDisplayConnected => tr('app_payment_display_connected');
  String get paymentDisplayWaitingPair =>
      tr('app_payment_display_waiting_pair');
  String get paymentDisplayStarting => tr('app_payment_display_starting');
  String get paymentDisplayStopped => tr('app_payment_display_stopped');
  String get paymentDisplayError => tr('app_payment_display_error');
  String get paymentDisplayNotConnected =>
      tr('app_payment_display_not_connected');
  String get paymentDisplayUpiNotConfigured =>
      tr('app_payment_display_upi_not_configured');
  String get paymentDisplayOpenPaymentSettings =>
      tr('app_payment_display_open_payment_settings');
  String get paymentDisplayInvalidAmount =>
      tr('app_payment_display_invalid_amount');
  String get paymentDisplayInvoiceInvalid =>
      tr('app_payment_display_invoice_invalid');
  String get paymentDisplayShowQr => tr('app_payment_display_show_qr');
  String get paymentDisplayQrSent => tr('app_payment_display_qr_sent');
  String get paymentDisplayAlreadyPaidConfirm =>
      tr('app_payment_display_already_paid_confirm');
  String get paymentDisplayAndroidOnly =>
      tr('app_payment_display_android_only');
  String get paymentDisplayKeepAppOpenHint =>
      tr('app_payment_display_keep_app_open_hint');
  String get paymentDisplaySubtitle => tr('app_payment_display_subtitle');

  String get pendingSync => tr('app_pending_sync');

  String get pleaseWait => tr('app_please_wait');

  String get portions => tr('app_portions');

  String get posLabel => 'POS';

  String get print => tr('app_print');

  String get printed => tr('app_printed');

  String get printerDetails => tr('app_printer_details');

  String get printShare => tr('app_print_share');

  String get proceedToPayment => tr('app_proceed_to_payment');

  String get product => tr('ui_product_2');

  String get productList => tr('ui_product_list');

  String get productMenu => tr('ui_product_menu');

  String get productPrice => tr('ui_product_price_2');

  String get productQuantity => tr('ui_product_quantity');

  String get products => tr('app_products');

  String get productWiseReport => tr('ui_product_wise_report');

  String get qrSaved => tr('app_qr_saved');

  String get quantity => tr('ui_quantity');

  String get refund => tr('app_refund');

  String get refundBill => tr('app_refund_bill');

  String get refundBillConfirm => tr('app_refund_bill_confirm');

  String get refunded => tr('app_refunded');

  String get refundWiseReport => tr('refund_wise_report');

  String get regular => tr('app_regular');

  String get removeItem => tr('app_remove_item');

  String get reportDetail => tr('ui_report_detail');

  String get reports => tr('app_reports');

  String get restart => tr('app_restart');

  String get resumeTable => tr('app_resume_table');

  String get reviewOrder => tr('app_review_order');

  String get saleReports => tr('ui_sale_reports');

  String get salesAndAnalytics => tr('ui_sales_and_reports');

  String get salesDashboard => tr('app_sales_dashboard');

  String get salesList => tr('app_sales_list');

  String get salesOverview => tr('app_sales_overview');

  String get saleWiseReport => tr('ui_sale_wise_report');

  String get save => tr('ui_save_details');

  String get saveAndPrint => tr('app_save_and_print');

  String get saveAndShare => tr('app_save_and_share');

  String get saveWithoutPrint => tr('app_save_without_print');

  String get scanMessToken => tr('app_scan_mess_token');

  String get selectPortion => tr('app_select_portion');

  String get sendKot => tr('app_send_kot');

  String get settings => tr('app_settings');

  String get settleBill => tr('app_settle_bill');

  String get settlementNotMatched => tr('ui_settlement_not_matched');

  String get shareApp => tr('app_share_app');

  String get shareBillImage => tr('app_share_bill_image');

  String get shareBillText => tr('app_share_bill_text');

  String get shared => tr('app_shared');

  String get shopDetails => tr('app_shop_details');

  String get signInSubtitle => tr('app_sign_in_subtitle');

  String get splitBill => tr('app_split_bill');

  String get splitJoined => tr('app_split_joined');

  String get splitJoinedTables => tr('app_split_joined_tables');

  String get startFreeTrial => tr('app_start_free_trial');

  String get store => tr('app_store');

  String get subcategories => tr('app_subcategories');

  String get subtotal => tr('ui_sub_total');

  String get support => tr('app_support');

  String get sync => tr('app_sync');

  String get synced => tr('app_synced');

  String get syncFinished => tr('app_sync_finished');

  String get syncPendingBills => tr('app_sync_pending_bills');

  String get syncTables => tr('app_sync_tables');

  String get tableActions => tr('app_table_actions');

  String get tableAvailable => tr('app_table_available');

  String get tableBill => tr('app_table_bill');

  String get tableBlocked => tr('app_table_blocked');

  String get tableDetailsSaved => tr('app_table_details_saved');

  String get tableHold => tr('app_table_hold');

  String get tableList => tr('app_table_list');

  String get tableMaster => tr('app_table_master');

  String get tableNo => tr('app_table_no');

  String get tableReserved => tr('app_table_reserved');

  String get tableRunning => tr('app_table_running');

  String get takeAway => tr('app_take_away');

  String get testPrint => tr('app_test_print');

  String get thisBranchOnly => tr('ui_this_branch_only');

  String get today => tr('app_today');

  String get todaySales => tr('app_today_sales');

  String get tokenNotFound => tr('app_token_not_found');

  String get totalSettlement => tr('ui_total_settlement');

  String get transferTable => tr('app_transfer_table');

  String get unitPrice => tr('app_unit_price');

  String get updateApp => tr('app_update_app');

  String get updateAppHint => tr('app_update_app_hint');

  String get updateBeforeContinue => tr('app_update_before_continue');

  String get updateDownloaded => tr('app_update_downloaded');

  String get upi => 'UPI';

  String get upiAmount => tr('ui_upi_amount');

  String get uploadToCloud => tr('app_upload_to_cloud');

  String get viewAll => tr('ui_view_all');

  String get viewBill => tr('app_view_bill');

  String get voidAction => tr('app_void_action');

  String get voidBill => tr('app_void_bill');

  String get voidBillConfirm => tr('app_void_bill_confirm');

  String get voided => tr('app_voided');

  String get voidedCannotEdit => tr('app_voided_cannot_edit');

  String get voidPendingBillConfirm => tr('app_void_pending_bill_confirm');

  String get waiterName => tr('app_waiter_name');

  String get walkInToken => tr('app_walk_in_token');

  String get welcomeBack => tr('app_welcome_back');

  String get yearWise => tr('app_year_wise');
}
