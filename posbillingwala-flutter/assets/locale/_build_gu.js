/**
 * Builds complete Gujarati locale (gu.json) from en.json + translation parts.
 * Run: node _build_gu.js
 */
const fs = require('fs');
const path = require('path');

const dir = __dirname;
const en = JSON.parse(fs.readFileSync(path.join(dir, 'en.json'), 'utf8'));

// Part 1: app_* / empty_* / home_* / print_* / retry / cancel / reports (from inline)
const part1 = {
  app_about: 'વિશે',
  app_about_us: 'અમારા વિશે',
  app_account: 'એકાઉન્ટ',
  app_active: 'સક્રિય',
  app_add: 'ઉમેરો',
  app_add_a_member_first: 'પહેલા મેસ સભ્ય ઉમેરો',
  app_add_inventory: 'ઇન્વેન્ટરી ઉમેરો',
  app_add_products: 'પ્રોડક્ટ્સ ઉમેરો',
  app_all_areas: 'બધા',
  app_all_invoices_cleared: 'બધા ઇન્વૉઇસ સાફ થયા',
  app_all_payments: 'બધા પેમેન્ટ',
  app_already_paid_month: 'આ મહિનાનું પેમેન્ટ પહેલેથી થઈ ગયું છે',
  app_attach_optional: 'જોડાણ ઉમેરો (વૈકલ્પિક)',
  app_bill_details: 'બિલ વિગતો',
  app_bill_header: 'બિલ હેડર',
  app_bill_not_found: 'બિલ મળ્યું નહીં',
  app_bill_refunded: 'બિલ રિફંડ તરીકે ચિહ્નિત',
  app_bill_requested: 'બિલ વિનંતી તરીકે ચિહ્નિત કરો',
  app_bill_saved: 'બિલ સાચવાયું',
  app_bill_updated_pending: 'બિલ સાચવાયું',
  app_bill_voided: 'બિલ સ્થાનિક રીતે રદ થયું',
  app_billing_catalog: 'બિલિંગ અને કેટલોગ',
  app_bills_and_lines: 'બિલ અને પ્રોડક્ટ લાઇન્સ',
  app_business_hours: 'વ્યવસાયના કલાકો',
  app_cancel_token: 'ટોકન રદ કરો',
  app_cart_empty_hint: 'વર્તમાન બિલમાં ઉમેરવા માટે પ્રોડક્ટ્સ પર ટૅપ કરો.',
  app_cart_empty_pay: 'કાર્ટ ખાલી છે. પેમેન્ટ પહેલાં પ્રોડક્ટ્સ ઉમેરો.',
  app_cart_is_empty: 'કાર્ટ ખાલી છે',
  app_catalog: 'કેટલોગ',
  app_categories: 'શ્રેણીઓ',
  app_category_list: 'શ્રેણી યાદી',
  app_category_saved: 'શ્રેણી સાચવાઈ',
  app_clear_cart: 'કાર્ટ સાફ કરો',
  app_clear_cart_confirm: 'આ બિલમાંથી બધી વસ્તુઓ દૂર કરીએ?',
  app_close: 'બંધ કરો',
  app_cloud_app: 'ક્લાઉડ અને એપ',
  app_combo: 'કૉમ્બો',
  app_combo_master: 'કૉમ્બો માસ્ટર',
  app_combos: 'કૉમ્બો',
  app_customer: 'ગ્રાહક',
  app_customer_address: 'સરનામું',
  app_customer_email: 'ઈમેઇલ',
  app_customer_mobile: 'મોબાઇલ',
  app_customer_name: 'નામ',
  app_customer_name_field: 'ગ્રાહકનું નામ',
  app_delete_line: 'લાઇન કાઢી નાખો',
  app_dine_in: 'ડાઇન-ઇન',
  app_dine_in_tables: 'ડાઇન-ઇન ટેબલ',
  app_duplicate_print: 'ડુપ્લિકેટ પ્રિન્ટ',
  app_duplicate_print_last_bill: 'છેલ્લું બિલ ડુપ્લિકેટ પ્રિન્ટ કરો',
  app_edit_bill: 'બિલ સંપાદિત કરો',
  app_edit_customer_discount_payment: 'ગ્રાહક / ડિસ્કાઉન્ટ / પેમેન્ટ સંપાદિત કરો',
  app_edit_customer_payment: 'ગ્રાહક / પેમેન્ટ સંપાદિત કરો',
  app_edit_invoice: 'ઇન્વૉઇસ સંપાદિત કરો',
  app_edit_item: 'આઇટમ સંપાદિત કરો',
  app_edit_line: 'લાઇન સંપાદિત કરો',
  app_enter_category_name: 'શ્રેણીનું નામ દાખલ કરો',
  app_enter_valid_price: 'માન્ય કિંમત દાખલ કરો',
  app_expenses: 'ખર્ચ',
  app_fast_billing: 'ફાસ્ટ બિલિંગ',
  app_forgot_licence: 'લાયસન્સ કી ભૂલી ગયા?',
  app_grand_total: 'કુલ રકમ',
  app_guest_count: 'મહેમાનોની સંખ્યા',
  app_guests_waiter: 'મહેમાનો / વેઇટર',
  app_help_support: 'મદદ અને સપોર્ટ',
  app_hold_table: 'ટેબલ હોલ્ડ કરો',
  app_home: 'હોમ',
  app_incorrect_report_pin: 'ખોટો રિપોર્ટ PIN',
  app_institute_pays: 'સંસ્થા ચૂકવે છે',
  app_inventory: 'ઇન્વેન્ટરી',
  app_invoice_details: 'ઇન્વૉઇસ વિગતો',
  app_invoice_not_found: 'ઇન્વૉઇસ મળ્યું નહીં',
  app_invoice_preview: 'ઇન્વૉઇસ પ્રીવ્યૂ',
  app_invoice_report: 'ઇન્વૉઇસ રિપોર્ટ',
  app_invoice_table_list_report: 'ઇન્વૉઇસ ટેબલ યાદી રિપોર્ટ',
  app_item_added_pending: 'આઇટમ સાચવાઈ',
  app_items: 'આઇટમ્સ',
  app_join_table: 'બીજા ટેબલ સાથે જોડો',
  app_kot_copied: 'KOT કૉપિ થયું',
  app_kot_printed: 'KOT પ્રિન્ટ થયું',
  app_kot_up_to_date: 'KOT અપ ટુ ડેટ છે',
  app_language: 'ભાષા',
  app_language_applied: 'ભાષા અપડેટ થઈ',
  app_licence_key: 'લાયસન્સ કી',
  app_licence_key_hint: 'તમારી દુકાનની લાયસન્સ કી દાખલ કરો',
  app_licence_required: 'લાયસન્સ કી જરૂરી છે',
  app_line_removed: 'લાઇન દૂર થઈ',
  app_listening: 'સાંભળી રહ્યા છીએ…',
  app_login: 'લૉગિન',
  app_long_press_table_hint: 'વધુ ક્રિયાઓ માટે ટેબલ પર લાંબા સમય સુધી દબાવો',
  app_mark_bill_requested: 'બિલ વિનંતી તરીકે ચિહ્નિત કરો',
  app_master_data: 'માસ્ટર ડેટા',
  app_meal_sessions: 'ભોજન સત્રો',
  app_member_payment_report: 'ઇન્વૉઇસ સભ્ય પેમેન્ટ રિપોર્ટ',
  app_member_saved: 'સભ્ય સાચવાયો',
  app_member_updated: 'સભ્ય સાચવાયો',
  app_mess: 'મેસ',
  app_module_locked: 'આ મોડ્યુલ તમારા લાયસન્સમાં સક્ષમ નથી. સપોર્ટનો સંપર્ક કરો.',
  app_month: 'મહિનો',
  app_more: 'વધુ…',
  app_more_actions: 'વધુ ક્રિયાઓ',
  app_move_items: 'આઇટમ્સ બીજા ટેબલ પર ખસેડો',
  app_move_items_table: 'આઇટમ્સ બીજા ટેબલ પર ખસેડો',
  app_name_required: 'નામ જરૂરી છે',
  app_mobile_required: 'મોબાઇલ નંબર જરૂરી છે',
  app_token_print_name_mobile_required: 'આ ટોકન પ્રિન્ટ કરવા માટે સભ્યનું નામ અને મોબાઇલ નંબર જરૂરી છે',
  app_new_parcel: 'નવું પાર્સલ',
  app_new_user_trial: 'નવા વપરાશકાર? મફત એકાઉન્ટ બનાવો',
  app_no_items: 'કોઈ આઇટમ નથી',
  app_no_mess_members: 'કોઈ મેસ સભ્ય મળ્યા નથી',
  app_no_previous_bill: 'ફરીથી પ્રિન્ટ કરવા માટે અગાઉનું બિલ નથી',
  app_no_products_catalog: 'કેટલોગમાં કોઈ પ્રોડક્ટ નથી',
  app_no_products_category: 'આ શ્રેણીમાં કોઈ પ્રોડક્ટ નથી',
  app_no_products_yet: 'હજુ કોઈ પ્રોડક્ટ નથી',
  app_no_qr_tokens_today: 'આજે કોઈ QR મીલ ટોકન નથી',
  app_no_table_bills: 'આ ફિલ્ટર માટે કોઈ ટેબલ બિલ નથી',
  app_no_tables_hint: 'ક્લાઉડથી ટેબલ સિંક કરો, અથવા ડિફૉલ્ટ આપમેળે બનશે.',
  app_no_tables_yet: 'હજુ કોઈ ટેબલ નથી',
  app_no_tokens_today: 'આજે કોઈ ટોકન જારી થયા નથી',
  app_notifications: 'સૂચનાઓ',
  app_open_billing: 'બિલિંગ ખોલો',
  app_open_sales_list: 'વેચાણ યાદી ખોલો',
  app_open_table_first: 'પહેલા ટેબલ ખોલો',
  app_packing_charge: 'પેકિંગ ચાર્જ',
  app_paper2_inch: '2-ઇંચ',
  app_paper3_inch: '3-ઇંચ',
  app_payment_updated: 'પેમેન્ટ સાચવાયું',
  app_pending_sync: 'બાકી સિંક',
  app_please_wait: 'કૃપા કરીને રાહ જુઓ…',
  app_portions: 'પોર્શન',
  app_print: 'પ્રિન્ટ',
  app_print_share: 'પ્રિન્ટ / શેર',
  app_printed: 'પ્રિન્ટ થયું',
  app_printer_details: 'પ્રિન્ટર વિગતો',
  app_proceed_to_payment: 'પેમેન્ટ તરફ આગળ વધો',
  app_products: 'પ્રોડક્ટ્સ',
  app_qr_saved: 'QR સાચવાયું',
  app_refund: 'રિફંડ',
  app_refund_bill: 'બિલ રિફંડ',
  app_refund_bill_confirm: 'આ બિલને રિફંડ તરીકે ચિહ્નિત કરીએ? તે રિફંડ રિપોર્ટમાં દેખાશે અને સિંક પર ફરીથી અપલોડ થશે.',
  app_refunded: 'રિફંડ થયું',
  app_regular: 'નિયમિત',
  app_remove_item: 'આઇટમ દૂર કરો',
  app_reports: 'રિપોર્ટ્સ',
  app_restart: 'ફરી શરૂ કરો',
  app_resume_table: 'ટેબલ ફરી શરૂ કરો',
  app_review_order: 'બિલિંગ પહેલાં તમારો ઓર્ડર તપાસો',
  app_sales_dashboard: 'વેચાણ ડેશબોર્ડ',
  app_sales_list: 'વેચાણ યાદી',
  app_sales_overview: 'વેચાણ ઝાંખી',
  app_save_and_print: 'સાચવો અને પ્રિન્ટ કરો',
  app_save_and_share: 'સાચવો અને શેર કરો',
  app_save_without_print: 'પ્રિન્ટ વગર સાચવો',
  app_scan_mess_token: 'મેસ ટોકન સ્કેન કરો',
  app_select_portion: 'પોર્શન પસંદ કરો',
  app_send_kot: 'KOT મોકલો',
  app_settings: 'સેટિંગ્સ',
  app_settle_bill: 'બિલ સેટલ કરો',
  app_share_app: 'એપ શેર કરો',
  app_share_bill_image: 'બિલ ઇમેજ શેર કરો',
  app_share_bill_text: 'બિલ ટેક્સ્ટ શેર કરો',
  app_shared: 'શેર થયું',
  app_shop_details: 'દુકાન વિગતો',
  app_sign_in_subtitle: 'તમારા Billingwala એકાઉન્ટમાં સાઇન ઇન કરો.',
  app_split_bill: 'બિલ વહેંચો',
  app_split_joined: 'જોડાયેલા ટેબલ અલગ કરો',
  app_split_joined_tables: 'જોડાયેલા ટેબલ અલગ કરો',
  app_start_free_trial: 'મફત ટ્રાયલ શરૂ કરો',
  app_store: 'સ્ટોર',
  app_subcategories: 'પેટા-શ્રેણીઓ',
  app_support: 'સપોર્ટ',
  app_sync: 'સિંક',
  app_sync_finished: 'સિંક પૂર્ણ',
  app_sync_pending_bills: 'પહેલા બાકી બિલ(ો) સિંક કરો.',
  app_sync_tables: 'ટેબલ સિંક કરો',
  app_synced: 'સિંક થયું',
  app_table_actions: 'ટેબલ ક્રિયાઓ',
  app_table_available: 'ઉપલબ્ધ',
  app_table_bill: 'બિલ',
  app_table_blocked: 'બ્લોક થયેલ',
  app_table_details_saved: 'ટેબલ વિગતો સાચવાઈ',
  app_table_hold: 'હોલ્ડ',
  app_table_list: 'ટેબલ યાદી',
  app_table_master: 'ટેબલ માસ્ટર',
  app_table_no: 'ટેબલ',
  app_table_reserved: 'રિઝર્વ્ડ',
  app_table_running: 'ચાલુ',
  app_take_away: 'ટેક અવે',
  app_test_print: 'ટેસ્ટ પ્રિન્ટ',
  app_today: 'આજે',
  app_today_sales: 'આજનું વેચાણ',
  app_token_not_found: 'ટોકન મળ્યું નહીં',
  app_transfer_table: 'બીજા ટેબલ પર સ્થાનાંતરિત કરો',
  app_unit_price: 'યુનિટ કિંમત',
  app_update_app: 'એપ અપડેટ કરો',
  app_update_app_hint: 'એપ છોડ્યા વગર Play Storeથી ઇન્સ્ટૉલ કરો',
  app_update_before_continue: 'ચાલુ રાખવા માટે કૃપા કરીને Billingwala અપડેટ કરો. પહેલા તમારો ડેટા સર્વર પર અપલોડ કરો. ડેટા ખોવાઈ જવા માટે અમે જવાબદાર નથી.',
  app_update_downloaded: 'અપડેટ ડાઉનલોડ થયું છે. ઇન્સ્ટૉલ કરવા માટે ફરી શરૂ કરો.',
  app_upload_to_cloud: 'ક્લાઉડ પર અપલોડ કરો',
  app_view_bill: 'બિલ જુઓ',
  app_void_action: 'રદ',
  app_void_bill: 'બિલ રદ કરો',
  app_void_bill_confirm: 'આ બિલ સ્થાનિક રીતે રદ કરીએ? તે વેચાણમાંથી દૂર થશે અને ક્લાઉડ ડિલીટ માટે કતારમાં જશે.',
  app_void_pending_bill_confirm: 'આ બાકી બિલને રદ તરીકે ચિહ્નિત કરીએ?',
  app_voided: 'રદ થયું',
  app_voided_cannot_edit: 'રદ / રિફંડ થયેલા બિલ સંપાદિત કરી શકાતા નથી',
  app_waiter_name: 'વેઇટરનું નામ',
  app_walk_in_token: 'વૉક-ઇન મેસ ટોકન',
  app_welcome_back: 'ફરી સ્વાગત છે',
  app_year_wise: 'વર્ષ મુજબ',
  cancel: 'રદ કરો',
  discount_wise_report: 'ડિસ્કાઉન્ટ મુજબનો રિપોર્ટ',
  empty_sub_categories: 'પહેલા માસ્ટર ડેટામાંથી શ્રેણીઓ ઉમેરો.',
  empty_sub_combos: 'માસ્ટર ડેટામાંથી કૉમ્બો મીલ બનાવો.',
  empty_sub_dine_in: 'કોઈ ટેબલ મળ્યા નથી. દુકાન વિગતોમાં ટેબલની સંખ્યા સેટ કરો અને ટેબલ માસ્ટરમાં ટેબલ ઉમેરો.',
  empty_sub_expenses: 'હજુ કોઈ ખર્ચ નોંધાયો નથી. એક બનાવવા માટે ઉમેરો પર ટૅપ કરો.',
  empty_sub_inventory: 'હજુ કોઈ ઇન્વેન્ટરી આઇટમ નથી. જથ્થો ટ્રૅક કરવા સ્ટોક ઉમેરો.',
  empty_sub_invoices: 'હજુ કોઈ ઇન્વૉઇસ નથી. અહીં જોવા માટે વેચાણ પૂર્ણ કરો.',
  empty_sub_member_payments: 'આ સભ્ય માટે કોઈ પેમેન્ટ ઇતિહાસ નથી.',
  empty_sub_members: 'હજુ કોઈ મેસ સભ્ય નથી. શરૂ કરવા એક સભ્ય ઉમેરો.',
  empty_sub_mess_invoices: 'પસંદ કરેલા સમયગાળા માટે કોઈ મેસ ઇન્વૉઇસ મળ્યા નથી.',
  empty_sub_parcels: 'કોઈ ખુલ્લા પાર્સલ નથી. બિલિંગ શરૂ કરવા નવું પાર્સલ પર ટૅપ કરો.',
  empty_sub_payment_history: 'આ સભ્ય માટે કોઈ પેમેન્ટ મળ્યા નથી.',
  empty_sub_portions: 'હાફ / ફુલ જેવા પોર્શન કદ ઉમેરો.',
  empty_sub_products: 'મેનૂ બનાવવા માસ્ટર ડેટામાંથી પ્રોડક્ટ્સ ઉમેરો.',
  empty_sub_reports: 'પસંદ કરેલા ફિલ્ટર માટે કોઈ રેકોર્ડ મળ્યા નથી.',
  empty_sub_sales: 'પસંદ કરેલા સમયગાળા માટે કોઈ વેચાણ મળ્યું નથી.',
  empty_sub_subcategories: 'શ્રેણી હેઠળ પેટા-શ્રેણીઓ ઉમેરો.',
  empty_sub_support_tickets: 'હજુ કોઈ સપોર્ટ ટિકિટ નથી.',
  empty_sub_table_master_areas: 'હજુ કોઈ વિસ્તાર નથી. હૉલ, AC, નોન-AC અથવા ગાર્ડન ઉમેરો.',
  empty_sub_table_master_tables: 'હજુ કોઈ ટેબલ નથી. વિસ્તાર અને પ્રકાર હેઠળ ટેબલ ઉમેરો.',
  empty_sub_table_master_types: 'હજુ કોઈ સીટિંગ પ્રકાર નથી. 2 સીટર, 4 સીટર વગેરે ઉમેરો.',
  home_printer_bill: 'પ્રિન્ટર',
  home_printer_bill_line: 'પ્રિન્ટર: %1$s',
  home_printer_kot: 'KOT',
  home_printer_kot_line: 'KOT: %1$s',
  home_printer_status_connected: 'કનેક્ટેડ',
  home_printer_status_connecting: 'કનેક્ટ થઈ રહ્યું છે…',
  home_printer_status_hint: 'પ્રિન્ટર કનેક્શન સ્થિતિ. પ્રિન્ટર સેટિંગ્સ ખોલવા ટૅપ કરો.',
  home_printer_status_not_set: 'સેટ નથી',
  home_printer_status_offline: 'ઑફલાઇન',
  print_powered_by: 'Powered by Billingwala',
  print_powered_by_website: 'www.posbillingwala.com',
  refund_wise_report: 'રિફંડ મુજબનો રિપોર્ટ',
  retry: 'ફરી પ્રયાસ કરો',
};

const part2 = JSON.parse(fs.readFileSync(path.join(dir, '_gu_part2.json'), 'utf8'));
const part3 = JSON.parse(fs.readFileSync(path.join(dir, '_gu_part3.json'), 'utf8'));

const translations = { ...part1, ...part2, ...part3 };

// Special keys with mojibake Devanagari — keep same key + parenthetical suffix from en,
// translate the English label portion to Gujarati.
const specialByPrefix = [
  ['ui_about_us_', 'અમારા વિશે'],
  ['ui_change_app_login_pbpin_', 'એપ લૉગિન PB-PIN બદલો'],
  ['ui_expense_management_', 'ખર્ચ મેનેજમેન્ટ'],
  ['ui_fetch_data_from_cloud_', 'ક્લાઉડથી ડેટા મેળવો'],
  ['ui_fetch_data_', 'ડેટા મેળવો'],
  ['ui_inventory_management_', 'ઇન્વેન્ટરી મેનેજમેન્ટ'],
  ['ui_invoice_details_', 'ઇન્વૉઇસ વિગતો'],
  ['ui_logout_', 'લૉગઆઉટ'],
  ['ui_offline_data_synchronize_with_cloud_', 'ઑફલાઇન ડેટા ક્લાઉડ સાથે સિંક'],
  ['ui_printer_details_', 'પ્રિન્ટર વિગતો'],
  ['ui_reports_', 'રિપોર્ટ્સ'],
  ['ui_share_app_', 'એપ શેર કરો'],
  ['ui_shop_details_', 'દુકાન વિગતો'],
  ['ui_synchronize_', 'સિંક્રનાઇઝ'],
  ['ui_update_app_', 'એપ અપડેટ કરો'],
];

for (const key of Object.keys(en)) {
  if (/[^\x00-\x7F]/.test(key)) {
    const enVal = en[key];
    const parenIdx = enVal.indexOf('(');
    const suffix = parenIdx >= 0 ? enVal.slice(parenIdx) : '';
    const newlinePrefix = enVal.startsWith('Fetch Data\n') || enVal.startsWith('Synchronize\n');
    let guLabel = null;
    for (const [prefix, label] of specialByPrefix) {
      if (key.startsWith(prefix)) {
        guLabel = label;
        break;
      }
    }
    if (!guLabel) {
      console.error('Unhandled special key:', key);
      process.exit(1);
    }
    if (key.startsWith('ui_fetch_data_') && !key.startsWith('ui_fetch_data_from_cloud_')) {
      translations[key] = `${guLabel}\n${suffix}`;
    } else if (key.startsWith('ui_synchronize_')) {
      translations[key] = `${guLabel}\n${suffix}`;
    } else {
      translations[key] = suffix ? `${guLabel} ${suffix}` : guLabel;
    }
  }
}

const missing = [];
const extra = [];
const gu = {};

for (const key of Object.keys(en)) {
  if (!(key in translations)) {
    missing.push(key);
  } else {
    gu[key] = translations[key];
  }
}
for (const key of Object.keys(translations)) {
  if (!(key in en)) extra.push(key);
}

if (missing.length || extra.length) {
  console.error('MISSING', missing.length, missing.slice(0, 30));
  console.error('EXTRA', extra.length, extra.slice(0, 30));
  process.exit(1);
}

const out = path.join(dir, 'gu.json');
fs.writeFileSync(out, JSON.stringify(gu, null, 2) + '\n', 'utf8');
console.log('Wrote', out);
console.log('Key count:', Object.keys(gu).length);
console.log('Matches en:', Object.keys(gu).length === Object.keys(en).length);
