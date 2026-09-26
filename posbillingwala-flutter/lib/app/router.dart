import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/logging/screen_context.dart';
import 'package:pos_billingwala_v2/core/logging/screen_route_observer.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/web_app_shell.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/login_page.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/mpin_page.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/register_page.dart';
import 'package:pos_billingwala_v2/features/auth/presentation/splash_page.dart';
import 'package:pos_billingwala_v2/features/expense/presentation/add_expense_page.dart';
import 'package:pos_billingwala_v2/features/expense/presentation/expense_page.dart';
import 'package:pos_billingwala_v2/features/home/presentation/home_page.dart';
import 'package:pos_billingwala_v2/features/inventory/presentation/add_inventory_page.dart';
import 'package:pos_billingwala_v2/features/inventory/presentation/inventory_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/categories_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/combo_form_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/combos_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/masters_hub_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/masters_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/portion_masters_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/product_form_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/product_portions_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/products_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/subcategories_page.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/table_master_page.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_payment_args.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_meal_sessions_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_meal_tokens_today_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_members_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_payments_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_qr_management_page.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_token_scan_page.dart';
import 'package:pos_billingwala_v2/features/notifications/presentation/notifications_page.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/payment_page.dart';
import 'package:pos_billingwala_v2/features/pos/presentation/pos_page.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/store_printer.dart';
import 'package:pos_billingwala_v2/features/print/presentation/bill_print_preview_page.dart';
import 'package:pos_billingwala_v2/features/print/presentation/device_list_page.dart';
import 'package:pos_billingwala_v2/features/print/presentation/print_queue_page.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_form_page.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_list_page.dart';
import 'package:pos_billingwala_v2/features/print/presentation/printer_routing_page.dart';
import 'package:pos_billingwala_v2/features/print/presentation/test_invoice_preview_page.dart';
import 'package:pos_billingwala_v2/features/print/presentation/test_mess_preview_page.dart';
import 'package:pos_billingwala_v2/features/reports/domain/reports_providers.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/edit_invoice_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/expense_report_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/invoice_add_products_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/invoice_detail_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/mess_invoice_report_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/mess_member_report_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/mess_payment_report_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/operational_report_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/product_wise_report_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/reports_hub_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/reports_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/sales_dashboard_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/sales_list_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/sales_overview_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/staff_wise_report_page.dart';
import 'package:pos_billingwala_v2/features/reports/presentation/table_list_report_page.dart';
import 'package:pos_billingwala_v2/features/payment_display/presentation/payment_display_settings_page.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/about_page.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/business_hours_page.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/change_pin_page.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/company_settings_page.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/settings_hub_page.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/settings_page.dart';
import 'package:pos_billingwala_v2/features/settings/presentation/share_app_page.dart';
import 'package:pos_billingwala_v2/features/staff/domain/permission_controller.dart';
import 'package:pos_billingwala_v2/features/staff/presentation/salary_page.dart';
import 'package:pos_billingwala_v2/features/staff/presentation/staff_detail_page.dart';
import 'package:pos_billingwala_v2/features/staff/presentation/staff_form_page.dart';
import 'package:pos_billingwala_v2/features/staff/presentation/staff_list_page.dart';
import 'package:pos_billingwala_v2/features/staff/presentation/staff_login_page.dart';
import 'package:pos_billingwala_v2/features/support/presentation/create_support_ticket_page.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_page.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_ticket_detail_page.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_tickets_page.dart';
import 'package:pos_billingwala_v2/features/sync/presentation/fetch_result_page.dart';
import 'package:pos_billingwala_v2/features/sync/presentation/sync_page.dart';
import 'package:pos_billingwala_v2/features/tables/presentation/split_bill_page.dart';
import 'package:pos_billingwala_v2/features/tables/presentation/tables_page.dart';
import 'package:pos_billingwala_v2/features/takeaway/presentation/takeaway_page.dart';

/* Global navigator key for FCM deep links and context-free navigation. */
final rootNavigatorKey = GlobalKey<NavigatorState>();

class GoRouterRefresh extends ChangeNotifier {
  void ping() => notifyListeners();
}

final routerProvider = Provider<GoRouter>((ref) {
  final refresh = GoRouterRefresh();
  ref.listen<AuthState>(authControllerProvider, (_, _) => refresh.ping());
  ref.listen(permissionControllerProvider, (_, _) => refresh.ping());
  ref.onDispose(refresh.dispose);

  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: '/splash',
    refreshListenable: refresh,
    debugLogDiagnostics: false,
    observers: [ScreenRouteObserver()],
    redirect: (context, state) {
      ScreenContext.update(
        name: state.name ?? state.matchedLocation,
        path: state.matchedLocation,
      );
      final auth = ref.read(authControllerProvider);
      final loc = state.matchedLocation;

      final isSplash = loc == '/splash';
      final isLogin = loc == '/login';
      final isMpin = loc == '/mpin';
      final isRegister = loc == '/register';
      final isStaffLogin = loc == '/staff-login';
      final isAuthRoute =
          isLogin || isMpin || isRegister || isSplash || isStaffLogin;

      switch (auth.status) {
        case AuthStatus.unknown:
          return isSplash ? null : '/splash';
        case AuthStatus.unauthenticated:
          if (isLogin || isRegister) return null;
          return '/login';
        case AuthStatus.needsMpin:
          if (isMpin) return null;
          /* Keep registration/login reachable for licence recovery or account changes. */
          if (isLogin || isRegister) return null;
          return '/mpin';
        case AuthStatus.needsStaffLogin:
          /* Optional staff route only — licence flow uses owner MPIN. */
          if (isStaffLogin) return null;
          if (isLogin || isRegister || isMpin) return null;
          return '/mpin';
        case AuthStatus.authenticated:
          if (isAuthRoute) return '/';
          return staffRoutePermissionRedirect(ref, loc);
      }
    },
    routes: [
      GoRoute(
        path: '/splash',
        name: 'splash',
        builder: (context, state) => const SplashPage(),
      ),
      GoRoute(
        path: '/login',
        name: 'login',
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: '/mpin',
        name: 'mpin',
        builder: (context, state) => const MpinPage(),
      ),
      GoRoute(
        path: '/register',
        name: 'register',
        builder: (context, state) => const RegisterPage(),
      ),
      GoRoute(
        path: '/staff-login',
        name: 'staff-login',
        builder: (context, state) => const StaffLoginPage(),
      ),
      ShellRoute(
        builder: (context, state, child) {
          if (!AppPlatform.useDesktopShell) return child;
          return WebAppShell(child: child);
        },
        routes: [
          GoRoute(
            path: '/',
            name: 'home',
            builder: (context, state) => const HomePage(),
          ),
          GoRoute(
            path: '/pos',
            name: 'pos',
            builder: (context, state) =>
                const PosPage(resetSessionOnOpen: true),
          ),
          GoRoute(
            path: '/pos/payment',
            name: 'payment',
            builder: (context, state) => const PaymentPage(),
          ),
          GoRoute(
            path: '/tables',
            name: 'tables',
            builder: (context, state) => const TablesPage(),
          ),
          GoRoute(
            path: '/tables/billing',
            name: 'tables-billing',
            builder: (context, state) => const PosPage(),
          ),
          GoRoute(
            path: '/tables/payment',
            name: 'tables-payment',
            builder: (context, state) => const PaymentPage(),
          ),
          GoRoute(
            path: '/tables/split-bill',
            name: 'tables-split-bill',
            builder: (context, state) {
              final table = state.uri.queryParameters['table'] ?? '';
              final sessionId =
                  int.tryParse(state.uri.queryParameters['sessionId'] ?? '') ??
                  0;
              return SplitBillPage(tableNumber: table, sessionId: sessionId);
            },
          ),
          GoRoute(
            path: '/takeaway',
            name: 'takeaway',
            builder: (context, state) => const TakeawayPage(),
          ),
          GoRoute(
            path: '/takeaway/billing',
            name: 'takeaway-billing',
            builder: (context, state) => PosPage(
              openCartOnStart: state.uri.queryParameters['cart'] == '1',
            ),
          ),
          GoRoute(
            path: '/takeaway/payment',
            name: 'takeaway-payment',
            builder: (context, state) => const PaymentPage(),
          ),
          GoRoute(
            path: '/mess',
            name: 'mess',
            builder: (context, state) => const MessPage(),
          ),
          GoRoute(
            path: '/mess/members',
            name: 'mess-members',
            builder: (context, state) => const MessMembersPage(),
          ),
          GoRoute(
            path: '/mess/qr',
            name: 'mess-qr',
            builder: (context, state) => const MessQrManagementPage(),
          ),
          GoRoute(
            path: '/mess/meal-sessions',
            name: 'mess-meal-sessions',
            builder: (context, state) => const MessMealSessionsPage(),
          ),
          GoRoute(
            path: '/mess/meal-tokens-today',
            name: 'mess-meal-tokens-today',
            builder: (context, state) => const MessMealTokensTodayPage(),
          ),
          GoRoute(
            path: '/mess/payments',
            name: 'mess-payments',
            builder: (context, state) {
              if (state.extra is MessPaymentsArgs) {
                final args = state.extra as MessPaymentsArgs;
                return MessPaymentsPage(
                  member: args.member,
                  mode: args.mode,
                );
              }
              final member = state.extra is MessMember
                  ? state.extra as MessMember
                  : null;
              return MessPaymentsPage(member: member);
            },
          ),
          GoRoute(
            path: '/mess/scan',
            name: 'mess-scan',
            builder: (context, state) => const MessTokenScanPage(),
          ),
          GoRoute(
            path: '/masters',
            name: 'masters',
            builder: (context, state) => const MastersHubPage(),
          ),
          GoRoute(
            path: '/masters/catalog',
            name: 'masters-catalog',
            builder: (context, state) {
              final tab = state.uri.queryParameters['tab'];
              return MastersPage(initialTab: tab == 'combos' ? 1 : 0);
            },
          ),
          GoRoute(
            path: '/masters/categories',
            name: 'masters-categories',
            builder: (context, state) => const CategoriesPage(),
          ),
          GoRoute(
            path: '/masters/products',
            name: 'masters-products',
            builder: (context, state) => const ProductsPage(),
          ),
          GoRoute(
            path: '/masters/products/form',
            name: 'masters-products-form',
            builder: (context, state) {
              final id = int.tryParse(state.uri.queryParameters['id'] ?? '');
              return ProductFormPage(productId: id);
            },
          ),
          GoRoute(
            path: '/masters/products/portions',
            name: 'masters-products-portions',
            builder: (context, state) {
              final id =
                  int.tryParse(state.uri.queryParameters['id'] ?? '') ?? 0;
              return ProductPortionsPage(productId: id);
            },
          ),
          GoRoute(
            path: '/masters/subcategories',
            name: 'masters-subcategories',
            builder: (context, state) => const SubcategoriesPage(),
          ),
          GoRoute(
            path: '/masters/tables',
            name: 'masters-tables',
            builder: (context, state) => const TableMasterPage(),
          ),
          GoRoute(
            path: '/masters/combos',
            name: 'masters-combos',
            builder: (context, state) => const CombosPage(),
          ),
          GoRoute(
            path: '/masters/combos/form',
            name: 'masters-combos-form',
            builder: (context, state) {
              final id = int.tryParse(state.uri.queryParameters['id'] ?? '');
              return ComboFormPage(comboId: id);
            },
          ),
          GoRoute(
            path: '/masters/portion-masters',
            name: 'masters-portion-masters',
            builder: (context, state) => const PortionMastersPage(),
          ),
          GoRoute(
            path: '/inventory',
            name: 'inventory',
            builder: (context, state) {
              final tab = state.uri.queryParameters['tab'];
              return InventoryPage(initialTab: tab == 'expenses' ? 1 : 0);
            },
          ),
          GoRoute(
            path: '/inventory/add',
            name: 'inventory-add',
            builder: (context, state) {
              final mode = state.uri.queryParameters['mode'];
              return AddInventoryPage(
                mode: mode == 'waste'
                    ? StockMovementMode.waste
                    : StockMovementMode.purchase,
              );
            },
          ),
          GoRoute(
            path: '/inventory/waste',
            name: 'inventory-waste',
            builder: (context, state) =>
                const AddInventoryPage(mode: StockMovementMode.waste),
          ),
          GoRoute(
            path: '/expenses',
            name: 'expenses',
            builder: (context, state) => const ExpensePage(),
          ),
          GoRoute(
            path: '/expenses/add',
            name: 'expenses-add',
            builder: (context, state) => const AddExpensePage(),
          ),
          GoRoute(
            path: '/reports',
            name: 'reports',
            builder: (context, state) => const ReportsHubPage(),
          ),
          GoRoute(
            path: '/reports/overview',
            name: 'reports-overview',
            builder: (context, state) => const SalesOverviewPage(),
          ),
          GoRoute(
            path: '/reports/sales-list',
            name: 'reports-sales-list',
            builder: (context, state) => const SalesListPage(),
          ),
          GoRoute(
            path: '/reports/dashboard',
            name: 'reports-dashboard',
            builder: (context, state) => const SalesDashboardPage(),
          ),
          GoRoute(
            path: '/reports/invoices',
            name: 'reports-invoices',
            builder: (context, state) => const ReportsPage(),
          ),
          GoRoute(
            path: '/reports/payment-mode',
            name: 'reports-payment-mode',
            builder: (context, state) => const OperationalReportPage(
              titleKey: 'ui_invoice_payment_mode_report',
              paymentBreakdown: true,
            ),
          ),
          GoRoute(
            path: '/reports/sale',
            name: 'reports-sale',
            builder: (context, state) => const OperationalReportPage(
              titleKey: 'ui_sale_reports',
              typeFilter: ReportInvoiceTypeFilter.pos,
            ),
          ),
          GoRoute(
            path: '/reports/table',
            name: 'reports-table',
            builder: (context, state) => const OperationalReportPage(
              titleKey: 'ui_invoice_table_report',
              typeFilter: ReportInvoiceTypeFilter.table,
            ),
          ),
          GoRoute(
            path: '/reports/table-list',
            name: 'reports-table-list',
            builder: (context, state) {
              final table = state.uri.queryParameters['table'];
              return TableListReportPage(initialTableNumber: table);
            },
          ),
          GoRoute(
            path: '/reports/takeaway',
            name: 'reports-takeaway',
            builder: (context, state) => const OperationalReportPage(
              titleKey: 'ui_invoice_take_away_report',
              typeFilter: ReportInvoiceTypeFilter.takeaway,
            ),
          ),
          GoRoute(
            path: '/reports/discount',
            name: 'reports-discount',
            builder: (context, state) => const OperationalReportPage(
              titleKey: 'discount_wise_report',
              typeFilter: ReportInvoiceTypeFilter.discountOnly,
            ),
          ),
          GoRoute(
            path: '/reports/mess',
            name: 'reports-mess',
            builder: (context, state) => const MessInvoiceReportPage(),
          ),
          GoRoute(
            path: '/reports/refund',
            name: 'reports-refund',
            builder: (context, state) => const OperationalReportPage(
              titleKey: 'refund_wise_report',
              typeFilter: ReportInvoiceTypeFilter.refundOnly,
            ),
          ),
          GoRoute(
            path: '/reports/mess-members',
            name: 'reports-mess-members',
            builder: (context, state) => const MessMemberReportPage(),
          ),
          GoRoute(
            path: '/reports/mess-payments',
            name: 'reports-mess-payments',
            builder: (context, state) => const MessPaymentReportPage(),
          ),
          GoRoute(
            path: '/reports/expense',
            name: 'reports-expense',
            builder: (context, state) => const ExpenseReportPage(),
          ),
          GoRoute(
            path: '/reports/staff-wise',
            name: 'reports-staff-wise',
            builder: (context, state) => const StaffWiseReportPage(),
          ),
          GoRoute(
            path: '/reports/products',
            name: 'reports-products',
            builder: (context, state) {
              final type = state.uri.queryParameters['type'] ?? 'all';
              return ProductWiseReportPage(initialType: type);
            },
          ),
          GoRoute(
            path: '/reports/invoice/:invoiceId/add-products',
            name: 'invoice-add-products',
            builder: (context, state) {
              final invoiceId = int.parse(state.pathParameters['invoiceId']!);
              return InvoiceAddProductsPage(invoiceId: invoiceId);
            },
          ),
          GoRoute(
            path: '/reports/invoice/:invoiceId/edit',
            name: 'invoice-edit',
            builder: (context, state) {
              final invoiceId = int.parse(state.pathParameters['invoiceId']!);
              return EditInvoicePage(invoiceId: invoiceId);
            },
          ),
          GoRoute(
            path: '/reports/invoice/:invoiceId',
            name: 'invoice-detail',
            builder: (context, state) {
              final invoiceId = int.parse(state.pathParameters['invoiceId']!);
              return InvoiceDetailPage(invoiceId: invoiceId);
            },
          ),
          GoRoute(
            path: '/print/bill/:invoiceId',
            name: 'print-bill',
            builder: (context, state) {
              final invoiceId = int.parse(state.pathParameters['invoiceId']!);
              final duplicate = state.uri.queryParameters['duplicate'] == '1';
              return BillPrintPreviewPage(
                invoiceId: invoiceId,
                duplicate: duplicate,
              );
            },
          ),
          GoRoute(
            path: '/sync',
            name: 'sync',
            builder: (context, state) {
              final mode = state.uri.queryParameters['mode'];
              return SyncPage(initialMode: mode);
            },
          ),
          GoRoute(
            path: '/sync/fetch-result',
            name: 'sync-fetch-result',
            builder: (context, state) => const FetchResultPage(),
          ),
          GoRoute(
            path: '/notifications',
            name: 'notifications',
            builder: (context, state) => const NotificationsPage(),
          ),
          GoRoute(
            path: '/settings',
            name: 'settings',
            builder: (context, state) => const SettingsHubPage(),
          ),
          GoRoute(
            path: '/settings/company',
            name: 'settings-company',
            builder: (context, state) => const CompanySettingsPage(),
          ),
          GoRoute(
            path: '/settings/devices',
            name: 'settings-devices',
            builder: (context, state) => const SettingsPage(),
          ),
          GoRoute(
            path: '/settings/business-hours',
            name: 'settings-business-hours',
            builder: (context, state) => const BusinessHoursPage(),
          ),
          GoRoute(
            path: '/settings/payment-display',
            name: 'settings-payment-display',
            builder: (context, state) => const PaymentDisplaySettingsPage(),
          ),
          GoRoute(
            path: '/settings/about',
            name: 'settings-about',
            builder: (context, state) => const AboutPage(),
          ),
          GoRoute(
            path: '/settings/share',
            name: 'settings-share',
            builder: (context, state) => const ShareAppPage(),
          ),
          GoRoute(
            path: '/settings/change-pin',
            name: 'settings-change-pin',
            builder: (context, state) => const ChangePinPage(),
          ),
          GoRoute(
            path: '/settings/users',
            name: 'settings-users',
            builder: (context, state) => const StaffListPage(),
          ),
          GoRoute(
            path: '/settings/salary',
            name: 'settings-salary',
            builder: (context, state) => const SalaryPage(),
          ),
          GoRoute(
            path: '/settings/users/add',
            name: 'settings-users-add',
            builder: (context, state) => const StaffFormPage(),
          ),
          GoRoute(
            path: '/settings/users/:id/edit',
            name: 'settings-users-edit',
            builder: (context, state) =>
                StaffFormPage(staffId: state.pathParameters['id']),
          ),
          GoRoute(
            path: '/settings/users/:id',
            name: 'settings-users-detail',
            builder: (context, state) =>
                StaffDetailPage(staffId: state.pathParameters['id']!),
          ),
          GoRoute(
            path: '/settings/printers',
            name: 'settings-printers',
            builder: (context, state) => const PrinterListPage(),
          ),
          GoRoute(
            path: '/settings/printers/add',
            name: 'settings-printers-add',
            builder: (context, state) => const PrinterFormPage(),
          ),
          GoRoute(
            path: '/settings/printers/edit',
            name: 'settings-printers-edit',
            builder: (context, state) => PrinterFormPage(
              existing: state.extra is StorePrinter
                  ? state.extra as StorePrinter
                  : null,
            ),
          ),
          GoRoute(
            path: '/settings/printer-routing',
            name: 'settings-printer-routing',
            builder: (context, state) => const PrinterRoutingPage(),
          ),
          GoRoute(
            path: '/settings/print-queue',
            name: 'settings-print-queue',
            builder: (context, state) => const PrintQueuePage(),
          ),
          GoRoute(
            path: '/settings/pos-devices',
            name: 'settings-pos-devices',
            builder: (context, state) => const DeviceListPage(),
          ),
          GoRoute(
            path: '/settings/test-print',
            name: 'test-print-preview',
            builder: (context, state) {
              final mode = state.uri.queryParameters['mode'] ?? 'invoice';
              if (mode == 'mess-qr') {
                return const TestMessPreviewPage(
                  kind: TestMessPreviewKind.qrToken,
                );
              }
              if (mode == 'mess-coupon') {
                return const TestMessPreviewPage(
                  kind: TestMessPreviewKind.coupon,
                );
              }
              if (mode == 'mess-common-qr') {
                return const TestMessPreviewPage(
                  kind: TestMessPreviewKind.commonQr,
                );
              }
              final channel = mode == 'kot'
                  ? PrinterChannelKind.kot
                  : PrinterChannelKind.bill;
              return TestInvoicePreviewPage(channel: channel);
            },
          ),
          GoRoute(
            path: '/support',
            name: 'support',
            builder: (context, state) => const SupportPage(),
          ),
          GoRoute(
            path: '/support/create',
            name: 'support-create',
            builder: (context, state) => const CreateSupportTicketPage(),
          ),
          GoRoute(
            path: '/support/tickets',
            name: 'support-tickets',
            builder: (context, state) => const SupportTicketsPage(),
          ),
          GoRoute(
            path: '/support/:ticketId',
            name: 'support-ticket',
            builder: (context, state) {
              final ticketId = state.pathParameters['ticketId'] ?? '';
              return SupportTicketDetailPage(ticketId: ticketId);
            },
          ),
        ],
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      appBar: AppBar(title: const Text('Not found')),
      body: Center(child: Text(state.error?.toString() ?? 'Page not found')),
    ),
  );
});

String? staffRoutePermissionRedirect(Ref ref, String loc) {
  const prefixes = <String, String>{
    '/pos/payment': 'bill.create',
    '/tables/payment': 'bill.create',
    '/takeaway/payment': 'bill.create',
    '/pos': 'billing.create',
    '/tables': 'table.view',
    '/takeaway': 'takeaway.view',
    '/mess': 'mess.view',
    '/reports': 'report.view',
    '/inventory': 'inventory.view',
    '/expenses': 'expense.view',
    '/masters': 'product.view',
    '/settings/users': 'user.view',
    '/settings/salary': 'user.view',
    '/settings/printers': 'printer.view',
    '/settings/pos-devices': 'device.view',
  };
  for (final entry in prefixes.entries) {
    if (loc == entry.key || loc.startsWith('${entry.key}/')) {
      if (!ref.read(permissionControllerProvider).allows(entry.value)) {
        return '/';
      }
      return null;
    }
  }
  return null;
}
