import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/database/database_provider.dart';
import 'package:pos_billingwala_v2/core/network/online_guard.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/company/data/company_api.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';

/* Company / shop profile only (Android Shop Details parity). */
class CompanySettingsPage extends ConsumerStatefulWidget {
  const CompanySettingsPage({super.key});

  @override
  ConsumerState<CompanySettingsPage> createState() =>
      CompanySettingsPageState();
}

class CompanySettingsPageState extends ConsumerState<CompanySettingsPage> {
  late final TextEditingController companySettingsPageCompanyName;
  late final TextEditingController companySettingsPageShopName1;
  late final TextEditingController companySettingsPageShopName2;
  late final TextEditingController companySettingsPageCashierName;
  late final TextEditingController companySettingsPageCompanyMobile;
  late final TextEditingController companySettingsPageCompanyAddress;
  late final TextEditingController companySettingsPageAddressLine1;
  late final TextEditingController companySettingsPageAddressLine2;
  late final TextEditingController companySettingsPageAddressLine3;
  late final TextEditingController companySettingsPagePhoneNo1;
  late final TextEditingController companySettingsPagePhoneNo2;
  late final TextEditingController companySettingsPageGstNumber;
  late final TextEditingController companySettingsPagePanNumber;
  late final TextEditingController companySettingsPageCompanyFssis;
  late final TextEditingController companySettingsPageShopCgst;
  late final TextEditingController companySettingsPageShopSgst;
  late final TextEditingController companySettingsPageCurrencyName;
  late final TextEditingController companySettingsPageCountryName;
  late final TextEditingController companySettingsPageStateName;
  late final TextEditingController companySettingsPageNoOfTable;
  late final TextEditingController companySettingsPagePaymentLogo;
  late final TextEditingController companySettingsPageOpeningMinutes;
  late final TextEditingController companySettingsPageClosingMinutes;
  bool gstEnabled = false;
  bool useTable = true;
  bool busy = false;
  String logoPath = '';

  @override
  void initState() {
    super.initState();
    final shop = ref.read(authControllerProvider).session?.shopName ?? '';
    final profile = ref.read(shopReceiptProfileProvider);
    companySettingsPageCompanyName = TextEditingController(text: shop);
    companySettingsPageShopName1 = TextEditingController();
    companySettingsPageShopName2 = TextEditingController();
    companySettingsPageCashierName = TextEditingController();
    companySettingsPageCompanyMobile = TextEditingController();
    companySettingsPageCompanyAddress = TextEditingController();
    companySettingsPageAddressLine1 = TextEditingController();
    companySettingsPageAddressLine2 = TextEditingController();
    companySettingsPageAddressLine3 = TextEditingController();
    companySettingsPagePhoneNo1 = TextEditingController();
    companySettingsPagePhoneNo2 = TextEditingController();
    companySettingsPageGstNumber = TextEditingController();
    companySettingsPagePanNumber = TextEditingController();
    companySettingsPageCompanyFssis = TextEditingController();
    companySettingsPageShopCgst = TextEditingController(text: '0');
    companySettingsPageShopSgst = TextEditingController(text: '0');
    companySettingsPageCurrencyName = TextEditingController(text: 'INR');
    companySettingsPageCountryName = TextEditingController(text: 'India');
    companySettingsPageStateName = TextEditingController();
    companySettingsPageNoOfTable = TextEditingController();
    companySettingsPagePaymentLogo = TextEditingController();
    companySettingsPageOpeningMinutes = TextEditingController();
    companySettingsPageClosingMinutes = TextEditingController();
    logoPath = profile.logoLocalPath;
    Future.microtask(load);
  }

  @override
  void dispose() {
    companySettingsPageCompanyName.dispose();
    companySettingsPageShopName1.dispose();
    companySettingsPageShopName2.dispose();
    companySettingsPageCashierName.dispose();
    companySettingsPageCompanyMobile.dispose();
    companySettingsPageCompanyAddress.dispose();
    companySettingsPageAddressLine1.dispose();
    companySettingsPageAddressLine2.dispose();
    companySettingsPageAddressLine3.dispose();
    companySettingsPagePhoneNo1.dispose();
    companySettingsPagePhoneNo2.dispose();
    companySettingsPageGstNumber.dispose();
    companySettingsPagePanNumber.dispose();
    companySettingsPageCompanyFssis.dispose();
    companySettingsPageShopCgst.dispose();
    companySettingsPageShopSgst.dispose();
    companySettingsPageCurrencyName.dispose();
    companySettingsPageCountryName.dispose();
    companySettingsPageStateName.dispose();
    companySettingsPageNoOfTable.dispose();
    companySettingsPagePaymentLogo.dispose();
    companySettingsPageOpeningMinutes.dispose();
    companySettingsPageClosingMinutes.dispose();
    super.dispose();
  }

  CompanyDto buildDto() {
    final shop1 = companySettingsPageShopName1.text.trim();
    final phone1 = companySettingsPagePhoneNo1.text.trim();
    final address = [
      companySettingsPageAddressLine1.text.trim(),
      companySettingsPageAddressLine2.text.trim(),
      companySettingsPageAddressLine3.text.trim(),
    ].where((e) => e.isNotEmpty).join(', ');
    return CompanyDto(
      companyName: shop1.isNotEmpty ? shop1 : companySettingsPageCompanyName.text.trim(),
      shopName1: shop1,
      shopName2: companySettingsPageShopName2.text.trim(),
      cashierName: companySettingsPageCashierName.text.trim(),
      companyMobile: phone1.isNotEmpty ? phone1 : companySettingsPageCompanyMobile.text.trim(),
      companyAddress:
          address.isNotEmpty ? address : companySettingsPageCompanyAddress.text.trim(),
      addressLine1: companySettingsPageAddressLine1.text.trim(),
      addressLine2: companySettingsPageAddressLine2.text.trim(),
      addressLine3: companySettingsPageAddressLine3.text.trim(),
      phoneNo1: phone1,
      phoneNo2: companySettingsPagePhoneNo2.text.trim(),
      gstStatus: gstEnabled ? '1' : '0',
      gstNumber: companySettingsPageGstNumber.text.trim(),
      panNumber: companySettingsPagePanNumber.text.trim(),
      companyFssis: companySettingsPageCompanyFssis.text.trim(),
      shopCgst: companySettingsPageShopCgst.text.trim(),
      shopSgst: companySettingsPageShopSgst.text.trim(),
      currencyName: companySettingsPageCurrencyName.text.trim(),
      countryName: companySettingsPageCountryName.text.trim(),
      stateName: companySettingsPageStateName.text.trim(),
      noOfTable: companySettingsPageNoOfTable.text.trim(),
      tableStatus: useTable ? '1' : '0',
      paymentLogo: companySettingsPagePaymentLogo.text.trim(),
      openingMinutes: companySettingsPageOpeningMinutes.text.trim(),
      closingMinutes: companySettingsPageClosingMinutes.text.trim(),
    );
  }

  void applyDto(CompanyDto c) {
    companySettingsPageCompanyName.text = c.companyName;
    companySettingsPageShopName1.text = c.shopName1 ?? '';
    companySettingsPageShopName2.text = c.shopName2 ?? '';
    companySettingsPageCashierName.text = c.cashierName ?? '';
    companySettingsPageCompanyMobile.text = c.companyMobile ?? '';
    companySettingsPageCompanyAddress.text = c.companyAddress ?? '';
    companySettingsPageAddressLine1.text = c.addressLine1 ?? '';
    companySettingsPageAddressLine2.text = c.addressLine2 ?? '';
    companySettingsPageAddressLine3.text = c.addressLine3 ?? '';
    companySettingsPagePhoneNo1.text = c.phoneNo1 ?? '';
    companySettingsPagePhoneNo2.text = c.phoneNo2 ?? '';
    gstEnabled = c.gstStatus == '1' || c.gstStatus?.toLowerCase() == 'true';
    companySettingsPageGstNumber.text = c.gstNumber ?? '';
    companySettingsPagePanNumber.text = c.panNumber ?? '';
    companySettingsPageCompanyFssis.text = c.companyFssis ?? '';
    companySettingsPageShopCgst.text = c.shopCgst ?? '0';
    companySettingsPageShopSgst.text = c.shopSgst ?? '0';
    companySettingsPageCurrencyName.text = c.currencyName ?? 'INR';
    companySettingsPageCountryName.text = c.countryName ?? 'India';
    companySettingsPageStateName.text = c.stateName ?? '';
    companySettingsPageNoOfTable.text = c.noOfTable ?? '';
    useTable = c.tableStatus == null ||
        c.tableStatus == '1' ||
        c.tableStatus?.toLowerCase() == 'true';
    companySettingsPagePaymentLogo.text = c.paymentLogo ?? '';
    companySettingsPageOpeningMinutes.text = c.openingMinutes ?? '';
    companySettingsPageClosingMinutes.text = c.closingMinutes ?? '';
  }

  Future<void> load() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    setState(() => busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      final local = await db.getLocalCompany();
      if (local != null && mounted) {
        setState(() => applyDto(companyRowToDto(local)));
      } else {
        final profile = ref.read(shopReceiptProfileProvider);
        if (mounted && profile.companyName.isNotEmpty) {
          setState(() {
            companySettingsPageCompanyName.text = profile.companyName;
            companySettingsPageShopName1.text = profile.shopName1;
            companySettingsPageAddressLine1.text = profile.addressLine1;
            companySettingsPagePhoneNo1.text = profile.phoneNo1;
            companySettingsPageGstNumber.text = profile.gstNumber;
            logoPath = profile.logoLocalPath;
          });
        }
      }

      if (!await ensureOnline()) {
        final profile = ref.read(shopReceiptProfileProvider);
        if (mounted) setState(() => logoPath = profile.logoLocalPath);
        return;
      }

      final api = CompanyApi(ref.read(apiClientProvider));
      final companies = await api.getCompanyList(userId);
      if (companies.isNotEmpty && mounted) {
        setState(() => applyDto(companies.first));
        await db.upsertLocalCompany(companies.first);
        await ref
            .read(shopReceiptProfileProvider.notifier)
            .saveFromCompany(companies.first);
        final profile = ref.read(shopReceiptProfileProvider);
        setState(() => logoPath = profile.logoLocalPath);
      }
    } catch (_) {
      /* Keep local / profile fields. */
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  CompanyDto companyRowToDto(Company c) {
    return CompanyDto(
      companyId: c.companyId,
      companyName: c.companyName ?? '',
      cashierName: c.cashierName,
      companyMobile: c.companyMobile,
      companyAddress: c.companyAddress,
      shopName1: c.shopName1,
      shopName2: c.shopName2,
      addressLine1: c.addressLine1,
      addressLine2: c.addressLine2,
      addressLine3: c.addressLine3,
      phoneNo1: c.phoneNo1,
      phoneNo2: c.phoneNo2,
      currencyName: c.currencyName,
      countryName: c.countryName,
      stateName: c.stateName,
      tableStatus: c.tableStatus,
      noOfTable: c.noOfTable,
      gstStatus: c.gstStatus,
      gstNumber: c.gstNumber,
      shopCgst: c.shopCgst,
      shopSgst: c.shopSgst,
      panNumber: c.panNumber,
      companyFssis: c.companyFssis,
      companyLogo: c.companyLogo,
      paymentLogo: c.paymentLogo,
      openingMinutes: c.openingMinutes,
      closingMinutes: c.closingMinutes,
      companyStatus: c.companyStatus,
    );
  }

  Future<void> pickLogo(ImageSource source) async {
    final picked = await ImagePicker().pickImage(
      source: source,
      maxWidth: 800,
      maxHeight: 800,
      imageQuality: 85,
    );
    if (picked == null) return;
    final docs = await getApplicationDocumentsDirectory();
    final dest = File('${docs.path}/shop_logo.jpg');
    await File(picked.path).copy(dest.path);
    await ref.read(shopReceiptProfileProvider.notifier).saveLogoPath(dest.path);
    if (!mounted) return;
    setState(() => logoPath = dest.path);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Shop logo saved for bills')),
    );
  }

  Future<void> companySettingsPageSave() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please login first')),
      );
      return;
    }
    if (AppPlatform.requiresNetwork && !await ensureOnline()) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(kOnlineRequiredMessage)),
      );
      return;
    }
    setState(() => busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      final dto = buildDto();
      var ok = false;
      if (await isDeviceOnline()) {
        try {
          ok = await CompanyApi(ref.read(apiClientProvider)).insertCompanyDetail(
            userId: userId,
            company: dto,
          );
        } catch (_) {
          ok = false;
        }
      }
      if (AppPlatform.requiresNetwork && !ok) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text(kWebApiSaveFailedMessage)),
        );
        return;
      }
      await db.upsertLocalCompany(dto);
      await ref.read(shopReceiptProfileProvider.notifier).saveFromCompany(dto);
      if (logoPath.isNotEmpty) {
        await ref
            .read(shopReceiptProfileProvider.notifier)
            .saveLogoPath(logoPath);
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            ok
                ? 'Shop details saved'
                : 'Saved offline — will sync when online',
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Widget field(
    TextEditingController c,
    String label, {
    TextInputType? keyboardType,
    int maxLines = 1,
  }) {
    return TextField(
      controller: c,
      keyboardType: keyboardType,
      maxLines: maxLines,
      minLines: maxLines > 1 ? maxLines : null,
      style: const TextStyle(
        fontFamily: AppFonts.family,
        fontSize: 14.5,
        color: AppColors.navy,
        fontWeight: FontWeight.w500,
      ),
      decoration: InputDecoration(
        labelText: label,
        floatingLabelBehavior: FloatingLabelBehavior.auto,
        filled: true,
        fillColor: Colors.white,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        labelStyle: TextStyle(
          fontFamily: AppFonts.family,
          color: AppColors.navy.withValues(alpha: .45),
          fontWeight: FontWeight.w400,
          fontSize: 14,
        ),
        floatingLabelStyle: const TextStyle(
          fontFamily: AppFonts.family,
          color: AppColors.primary,
          fontWeight: FontWeight.w500,
          fontSize: 13,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
          borderSide: BorderSide(color: AppColors.border.withValues(alpha: .9)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
          borderSide: BorderSide(color: AppColors.border.withValues(alpha: .9)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.4),
        ),
      ),
    );
  }

  Widget section({
    required String title,
    required List<Widget> children,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        MasterSectionLabel(title),
        const SizedBox(height: 10),
        MasterCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              for (var i = 0; i < children.length; i++) ...[
                if (i > 0) const SizedBox(height: 12),
                children[i],
              ],
            ],
          ),
        ),
      ],
    );
  }

  Widget toggleRow({
    required String label,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Row(
      children: [
        Expanded(
          child: Text(
            label,
            style: const TextStyle(
              fontFamily: AppFonts.family,
              fontWeight: FontWeight.w600,
              fontSize: 14.5,
              color: AppColors.navy,
            ),
          ),
        ),
        Switch.adaptive(
          value: value,
          activeThumbColor: Colors.white,
          activeTrackColor: AppColors.green,
          onChanged: onChanged,
        ),
      ],
    );
  }

  Widget currencyDropdown() {
    const defaults = [
      'INR',
      'USD',
      'EUR',
      'GBP',
      'AED',
      'SAR',
      'QAR',
      'AUD',
      'CAD',
      'SGD',
      'NPR',
      'BDT',
      'LKR',
    ];
    final current =
        companySettingsPageCurrencyName.text.trim().isEmpty ? 'INR' : companySettingsPageCurrencyName.text.trim();
    final options = {
      ...defaults,
      if (current.isNotEmpty) current,
    }.toList();

    return StringDropdownField(
      label: 'Invoice Currency',
      value: current,
      enableSearch: true,
      options: options,
      onChanged: (v) {
        if (v != null) setState(() => companySettingsPageCurrencyName.text = v);
      },
    );
  }

  Widget logoCard() {
    final logoFile = logoPath.isNotEmpty ? File(logoPath) : null;
    final hasLogo = logoFile != null && logoFile.existsSync();

    return section(
      title: 'Branding',
      children: [
        AspectRatio(
          aspectRatio: 2.4,
          child: Stack(
            fit: StackFit.expand,
            children: [
              Material(
                color: Colors.white,
                borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
                child: InkWell(
                  onTap: busy ? null : () => pickLogo(ImageSource.gallery),
                  borderRadius: BorderRadius.circular(MasterUi.fieldRadius),
                  child: Ink(
                    decoration: BoxDecoration(
                      borderRadius:
                          BorderRadius.circular(MasterUi.fieldRadius),
                      border: Border.all(
                        color: AppColors.border.withValues(alpha: .9),
                      ),
                    ),
                    child: ClipRRect(
                      borderRadius:
                          BorderRadius.circular(MasterUi.fieldRadius),
                      child: hasLogo
                          ? Image.file(logoFile, fit: BoxFit.contain)
                          : Image.asset(
                              AppAssets.yourLogoHere,
                              fit: BoxFit.contain,
                            ),
                    ),
                  ),
                ),
              ),
              Positioned(
                right: 10,
                bottom: 10,
                child: Material(
                  color: AppColors.primary,
                  shape: const CircleBorder(),
                  elevation: 2,
                  child: InkWell(
                    customBorder: const CircleBorder(),
                    onTap:
                        busy ? null : () => pickLogo(ImageSource.gallery),
                    child: const SizedBox(
                      width: 40,
                      height: 40,
                      child: Icon(
                        Icons.edit_rounded,
                        color: Colors.white,
                        size: 18,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        Text(
          'Shown on printed bills when logo is enabled.',
          style: TextStyle(
            fontFamily: AppFonts.family,
            fontSize: 12,
            color: AppColors.navy.withValues(alpha: .48),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: MasterUi.bg,
      appBar: AppBar(
        toolbarHeight: 72,
        titleSpacing: 0,
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              'Shop Details',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w700,
                fontSize: 18,
                color: Colors.white,
                height: 1.2,
              ),
            ),
            SizedBox(height: 2),
            Text(
              'Manage your shop information',
              style: TextStyle(
                fontFamily: AppFonts.family,
                fontWeight: FontWeight.w400,
                fontSize: 12.5,
                color: Colors.white70,
                height: 1.2,
              ),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: Stack(
              children: [
                ResponsiveScrollShell(
                  dashboard: true,
                  child: ListView(
                    padding: EdgeInsets.fromLTRB(
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      16,
                      AppBreakpoints.pagePaddingFor(context.widthClass),
                      28,
                    ),
                    children: [
                    logoCard(),
                    const SizedBox(height: 18),
                    section(
                      title: 'Shop Identity',
                      children: [
                        field(companySettingsPageShopName1, 'Shop Name 1'),
                        field(companySettingsPageShopName2, 'Shop Name 2'),
                        field(companySettingsPageAddressLine1, 'Address Line 1'),
                        field(companySettingsPageAddressLine2, 'Address Line 2'),
                        field(companySettingsPageAddressLine3, 'Address Line 3'),
                      ],
                    ),
                    const SizedBox(height: 18),
                    section(
                      title: 'Contact',
                      children: [
                        field(
                          companySettingsPagePhoneNo1,
                          'Phone No. 1',
                          keyboardType: TextInputType.phone,
                        ),
                        field(
                          companySettingsPagePhoneNo2,
                          'Phone No. 2',
                          keyboardType: TextInputType.phone,
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    section(
                      title: 'Operations',
                      children: [
                        currencyDropdown(),
                        toggleRow(
                          label: 'Use Table',
                          value: useTable,
                          onChanged: (v) => setState(() => useTable = v),
                        ),
                        if (useTable)
                          field(
                            companySettingsPageNoOfTable,
                            'No of Table',
                            keyboardType: TextInputType.number,
                          ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    section(
                      title: 'Tax & Compliance',
                      children: [
                        field(companySettingsPageCountryName, 'Country Name'),
                        field(companySettingsPageStateName, 'State Name'),
                        toggleRow(
                          label: 'GST',
                          value: gstEnabled,
                          onChanged: (v) => setState(() => gstEnabled = v),
                        ),
                        field(companySettingsPageGstNumber, 'GST Number'),
                        Row(
                          children: [
                            Expanded(
                              child: field(
                                companySettingsPageShopCgst,
                                'Shop CGST',
                                keyboardType:
                                    const TextInputType.numberWithOptions(
                                  decimal: true,
                                ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: field(
                                companySettingsPageShopSgst,
                                'Shop SGST',
                                keyboardType:
                                    const TextInputType.numberWithOptions(
                                  decimal: true,
                                ),
                              ),
                            ),
                          ],
                        ),
                        field(companySettingsPagePanNumber, 'PAN Number'),
                        field(companySettingsPageCompanyFssis, 'shop FSSAI Number'),
                      ],
                    ),
                    const SizedBox(height: 18),
                    section(
                      title: 'Payment UPI',
                      children: [
                        field(
                          companySettingsPagePaymentLogo,
                          'UPI ID (e.g. shopname@upi)',
                          keyboardType: TextInputType.emailAddress,
                        ),
                        Text(
                          'When Payment QR is enabled in printer settings, a QR for the bill amount is generated from this UPI ID',
                          style: TextStyle(
                            fontFamily: AppFonts.family,
                            fontSize: 12,
                            height: 1.35,
                            color: AppColors.navy.withValues(alpha: .48),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                ),
                if (busy)
                  const Positioned.fill(
                    child: ColoredBox(
                      color: Color(0x66F3F7FC),
                      child: Center(child: CircularProgressIndicator()),
                    ),
                  ),
              ],
            ),
          ),
          SafeArea(
            top: false,
            child: Container(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
              decoration: BoxDecoration(
                color: MasterUi.bg,
                border: Border(
                  top: BorderSide(
                    color: AppColors.border.withValues(alpha: .7),
                  ),
                ),
              ),
              child: AppButton(
                label: 'UPDATE DETAILS',
                icon: Icons.save_rounded,
                isLoading: busy,
                onPressed: companySettingsPageSave,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
