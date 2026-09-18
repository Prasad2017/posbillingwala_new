import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/company/data/company_api.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:shared_preferences/shared_preferences.dart';

/* Matches `dialog_business_hours.xml`: opening + closing time values only. */
class BusinessHoursPage extends ConsumerStatefulWidget {
  const BusinessHoursPage({super.key});

  @override
  ConsumerState<BusinessHoursPage> createState() => BusinessHoursPageState();
}

class BusinessHoursPageState extends ConsumerState<BusinessHoursPage> {
  static const openKey = 'businessOpenMinutes';
  static const closeKey = 'businessCloseMinutes';

  TimeOfDay businessHoursPageOpen = const TimeOfDay(hour: 9, minute: 0);
  TimeOfDay businessHoursPageClose = const TimeOfDay(hour: 22, minute: 0);
  bool busy = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(load);
  }

  int toMinutes(TimeOfDay t) => t.hour * 60 + t.minute;

  TimeOfDay fromMinutes(int minutes) {
    final m = minutes.clamp(0, 24 * 60 - 1);
    return TimeOfDay(hour: m ~/ 60, minute: m % 60);
  }

  TimeOfDay? parseStored(String? raw) {
    if (raw == null || raw.trim().isEmpty) return null;
    final asInt = int.tryParse(raw.trim());
    if (asInt != null) return fromMinutes(asInt);
    final parts = raw.trim().split(':');
    if (parts.length >= 2) {
      final h = int.tryParse(parts[0]);
      final m = int.tryParse(parts[1]);
      if (h != null && m != null) {
        return TimeOfDay(hour: h.clamp(0, 23), minute: m.clamp(0, 59));
      }
    }
    return null;
  }

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final openPref = prefs.getInt(openKey);
    final closePref = prefs.getInt(closeKey);
    if (openPref != null) businessHoursPageOpen = fromMinutes(openPref);
    if (closePref != null) businessHoursPageClose = fromMinutes(closePref);

    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId != null && userId.isNotEmpty) {
      try {
        final companies = await CompanyApi(
          ref.read(apiClientProvider),
        ).getCompanyList(userId);
        if (companies.isNotEmpty) {
          final c = companies.first;
          final open = parseStored(c.openingMinutes);
          final close = parseStored(c.closingMinutes);
          if (open != null) businessHoursPageOpen = open;
          if (close != null) businessHoursPageClose = close;
        }
      } catch (_) {
        /* Keep prefs / defaults. */
      }
    }
    if (mounted) setState(() {});
  }

  Future<void> pickOpen() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: businessHoursPageOpen,
    );
    if (picked != null) setState(() => businessHoursPageOpen = picked);
  }

  Future<void> pickClose() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: businessHoursPageClose,
    );
    if (picked != null) setState(() => businessHoursPageClose = picked);
  }

  String fmt(TimeOfDay t) =>
      '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';

  Future<void> save() async {
    setState(() => busy = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt(openKey, toMinutes(businessHoursPageOpen));
      await prefs.setInt(closeKey, toMinutes(businessHoursPageClose));

      final userId = ref.read(authControllerProvider).session?.userId;
      if (userId != null && userId.isNotEmpty) {
        try {
          final api = CompanyApi(ref.read(apiClientProvider));
          final companies = await api.getCompanyList(userId);
          final base = companies.isNotEmpty
              ? companies.first
              : const CompanyDto();
          await api.insertCompanyDetail(
            userId: userId,
            company: CompanyDto(
              companyId: base.companyId,
              companyName: base.companyName,
              companyLogo: base.companyLogo,
              paymentLogo: base.paymentLogo,
              cashierName: base.cashierName,
              companyMobile: base.companyMobile,
              companyAddress: base.companyAddress,
              shopName1: base.shopName1,
              shopName2: base.shopName2,
              addressLine1: base.addressLine1,
              addressLine2: base.addressLine2,
              addressLine3: base.addressLine3,
              phoneNo1: base.phoneNo1,
              phoneNo2: base.phoneNo2,
              currencyName: base.currencyName,
              tableStatus: base.tableStatus,
              noOfTable: base.noOfTable,
              countryName: base.countryName,
              stateName: base.stateName,
              gstStatus: base.gstStatus,
              gstNumber: base.gstNumber,
              panNumber: base.panNumber,
              companyFssis: base.companyFssis,
              openingMinutes: fmt(businessHoursPageOpen),
              closingMinutes: fmt(businessHoursPageClose),
              companyStatus: base.companyStatus,
            ),
          );
        } catch (_) {
          /* Local prefs still saved. */
        }
      }

      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Business hours saved')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Business Hours'),
        actions: [
          TextButton(
            onPressed: busy ? null : save,
            child: const Text('Save', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
          padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass),
          ),
          children: [
            ListTile(
              title: const Text('Opening time'),
              trailing: Text(
                fmt(businessHoursPageOpen),
                style: const TextStyle(
                  fontWeight: FontWeight.w700,
                  fontSize: 16,
                ),
              ),
              onTap: pickOpen,
            ),
            const Divider(height: 1),
            ListTile(
              title: const Text('Closing time'),
              trailing: Text(
                fmt(businessHoursPageClose),
                style: const TextStyle(
                  fontWeight: FontWeight.w700,
                  fontSize: 16,
                ),
              ),
              onTap: pickClose,
            ),
            const SizedBox(height: 24),
            AppButton(label: 'Save', isLoading: busy, onPressed: save),
          ],
        ),
      ),
    );
  }
}
