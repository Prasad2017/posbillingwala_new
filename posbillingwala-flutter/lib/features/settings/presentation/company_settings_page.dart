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
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/company/data/company_api.dart';
import 'package:pos_billingwala_v2/features/company/data/company_dtos.dart';
import 'package:pos_billingwala_v2/features/masters/presentation/widgets/master_ui.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';

/// Company / shop profile only (Android Shop Details parity).
class CompanySettingsPage extends ConsumerStatefulWidget {
  const CompanySettingsPage({super.key});

  @override
  ConsumerState<CompanySettingsPage> createState() =>
      _CompanySettingsPageState();
}

class _CompanySettingsPageState extends ConsumerState<CompanySettingsPage> {
  late final TextEditingController _companyName;
  late final TextEditingController _shopName1;
  late final TextEditingController _shopName2;
  late final TextEditingController _cashierName;
  late final TextEditingController _companyMobile;
  late final TextEditingController _companyAddress;
  late final TextEditingController _addressLine1;
  late final TextEditingController _addressLine2;
  late final TextEditingController _addressLine3;
  late final TextEditingController _phoneNo1;
  late final TextEditingController _phoneNo2;
  late final TextEditingController _gstNumber;
  late final TextEditingController _panNumber;
  late final TextEditingController _companyFssis;
  late final TextEditingController _shopCgst;
  late final TextEditingController _shopSgst;
  late final TextEditingController _currencyName;
  late final TextEditingController _countryName;
  late final TextEditingController _stateName;
  late final TextEditingController _noOfTable;
  late final TextEditingController _paymentLogo;
  late final TextEditingController _openingMinutes;
  late final TextEditingController _closingMinutes;
  bool _gstEnabled = false;
  bool _useTable = true;
  bool _busy = false;
  String _logoPath = '';

  @override
  void initState() {
    super.initState();
    final shop = ref.read(authControllerProvider).session?.shopName ?? '';
    final profile = ref.read(shopReceiptProfileProvider);
    _companyName = TextEditingController(text: shop);
    _shopName1 = TextEditingController();
    _shopName2 = TextEditingController();
    _cashierName = TextEditingController();
    _companyMobile = TextEditingController();
    _companyAddress = TextEditingController();
    _addressLine1 = TextEditingController();
    _addressLine2 = TextEditingController();
    _addressLine3 = TextEditingController();
    _phoneNo1 = TextEditingController();
    _phoneNo2 = TextEditingController();
    _gstNumber = TextEditingController();
    _panNumber = TextEditingController();
    _companyFssis = TextEditingController();
    _shopCgst = TextEditingController(text: '0');
    _shopSgst = TextEditingController(text: '0');
    _currencyName = TextEditingController(text: 'INR');
    _countryName = TextEditingController(text: 'India');
    _stateName = TextEditingController();
    _noOfTable = TextEditingController();
    _paymentLogo = TextEditingController();
    _openingMinutes = TextEditingController();
    _closingMinutes = TextEditingController();
    _logoPath = profile.logoLocalPath;
    Future.microtask(_load);
  }

  @override
  void dispose() {
    _companyName.dispose();
    _shopName1.dispose();
    _shopName2.dispose();
    _cashierName.dispose();
    _companyMobile.dispose();
    _companyAddress.dispose();
    _addressLine1.dispose();
    _addressLine2.dispose();
    _addressLine3.dispose();
    _phoneNo1.dispose();
    _phoneNo2.dispose();
    _gstNumber.dispose();
    _panNumber.dispose();
    _companyFssis.dispose();
    _shopCgst.dispose();
    _shopSgst.dispose();
    _currencyName.dispose();
    _countryName.dispose();
    _stateName.dispose();
    _noOfTable.dispose();
    _paymentLogo.dispose();
    _openingMinutes.dispose();
    _closingMinutes.dispose();
    super.dispose();
  }

  CompanyDto _buildDto() {
    final shop1 = _shopName1.text.trim();
    final phone1 = _phoneNo1.text.trim();
    final address = [
      _addressLine1.text.trim(),
      _addressLine2.text.trim(),
      _addressLine3.text.trim(),
    ].where((e) => e.isNotEmpty).join(', ');
    return CompanyDto(
      companyName: shop1.isNotEmpty ? shop1 : _companyName.text.trim(),
      shopName1: shop1,
      shopName2: _shopName2.text.trim(),
      cashierName: _cashierName.text.trim(),
      companyMobile: phone1.isNotEmpty ? phone1 : _companyMobile.text.trim(),
      companyAddress:
          address.isNotEmpty ? address : _companyAddress.text.trim(),
      addressLine1: _addressLine1.text.trim(),
      addressLine2: _addressLine2.text.trim(),
      addressLine3: _addressLine3.text.trim(),
      phoneNo1: phone1,
      phoneNo2: _phoneNo2.text.trim(),
      gstStatus: _gstEnabled ? '1' : '0',
      gstNumber: _gstNumber.text.trim(),
      panNumber: _panNumber.text.trim(),
      companyFssis: _companyFssis.text.trim(),
      shopCgst: _shopCgst.text.trim(),
      shopSgst: _shopSgst.text.trim(),
      currencyName: _currencyName.text.trim(),
      countryName: _countryName.text.trim(),
      stateName: _stateName.text.trim(),
      noOfTable: _noOfTable.text.trim(),
      tableStatus: _useTable ? '1' : '0',
      paymentLogo: _paymentLogo.text.trim(),
      openingMinutes: _openingMinutes.text.trim(),
      closingMinutes: _closingMinutes.text.trim(),
    );
  }

  void _applyDto(CompanyDto c) {
    _companyName.text = c.companyName;
    _shopName1.text = c.shopName1 ?? '';
    _shopName2.text = c.shopName2 ?? '';
    _cashierName.text = c.cashierName ?? '';
    _companyMobile.text = c.companyMobile ?? '';
    _companyAddress.text = c.companyAddress ?? '';
    _addressLine1.text = c.addressLine1 ?? '';
    _addressLine2.text = c.addressLine2 ?? '';
    _addressLine3.text = c.addressLine3 ?? '';
    _phoneNo1.text = c.phoneNo1 ?? '';
    _phoneNo2.text = c.phoneNo2 ?? '';
    _gstEnabled = c.gstStatus == '1' || c.gstStatus?.toLowerCase() == 'true';
    _gstNumber.text = c.gstNumber ?? '';
    _panNumber.text = c.panNumber ?? '';
    _companyFssis.text = c.companyFssis ?? '';
    _shopCgst.text = c.shopCgst ?? '0';
    _shopSgst.text = c.shopSgst ?? '0';
    _currencyName.text = c.currencyName ?? 'INR';
    _countryName.text = c.countryName ?? 'India';
    _stateName.text = c.stateName ?? '';
    _noOfTable.text = c.noOfTable ?? '';
    _useTable = c.tableStatus == null ||
        c.tableStatus == '1' ||
        c.tableStatus?.toLowerCase() == 'true';
    _paymentLogo.text = c.paymentLogo ?? '';
    _openingMinutes.text = c.openingMinutes ?? '';
    _closingMinutes.text = c.closingMinutes ?? '';
  }

  Future<void> _load() async {
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;
    setState(() => _busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      final local = await db.getLocalCompany();
      if (local != null && mounted) {
        setState(() => _applyDto(_companyRowToDto(local)));
      } else {
        final profile = ref.read(shopReceiptProfileProvider);
        if (mounted && profile.companyName.isNotEmpty) {
          setState(() {
            _companyName.text = profile.companyName;
            _shopName1.text = profile.shopName1;
            _addressLine1.text = profile.addressLine1;
            _phoneNo1.text = profile.phoneNo1;
            _gstNumber.text = profile.gstNumber;
            _logoPath = profile.logoLocalPath;
          });
        }
      }

      if (!await ensureOnline()) {
        final profile = ref.read(shopReceiptProfileProvider);
        if (mounted) setState(() => _logoPath = profile.logoLocalPath);
        return;
      }

      final api = CompanyApi(ref.read(apiClientProvider));
      final companies = await api.getCompanyList(userId);
      if (companies.isNotEmpty && mounted) {
        setState(() => _applyDto(companies.first));
        await db.upsertLocalCompany(companies.first);
        await ref
            .read(shopReceiptProfileProvider.notifier)
            .saveFromCompany(companies.first);
        final profile = ref.read(shopReceiptProfileProvider);
        setState(() => _logoPath = profile.logoLocalPath);
      }
    } catch (_) {
      // Keep local / profile fields.
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  CompanyDto _companyRowToDto(Company c) {
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

  Future<void> _pickLogo(ImageSource source) async {
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
    setState(() => _logoPath = dest.path);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Shop logo saved for bills')),
    );
  }

  Future<void> _save() async {
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
    setState(() => _busy = true);
    try {
      final db = ref.read(appDatabaseProvider);
      final dto = _buildDto();
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
      await db.upsertLocalCompany(dto);
      await ref.read(shopReceiptProfileProvider.notifier).saveFromCompany(dto);
      if (_logoPath.isNotEmpty) {
        await ref
            .read(shopReceiptProfileProvider.notifier)
            .saveLogoPath(_logoPath);
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            ok
                ? 'Shop details saved'
                : (AppPlatform.supportsOfflineBilling
                    ? 'Saved offline — will sync when online'
                    : 'Could not save — check internet and retry'),
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _field(
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

  Widget _section({
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

  Widget _toggleRow({
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

  Widget _currencyDropdown() {
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
        _currencyName.text.trim().isEmpty ? 'INR' : _currencyName.text.trim();
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
        if (v != null) setState(() => _currencyName.text = v);
      },
    );
  }

  Widget _logoCard() {
    final logoFile = _logoPath.isNotEmpty ? File(_logoPath) : null;
    final hasLogo = logoFile != null && logoFile.existsSync();

    return _section(
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
                  onTap: _busy ? null : () => _pickLogo(ImageSource.gallery),
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
                        _busy ? null : () => _pickLogo(ImageSource.gallery),
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
                    _logoCard(),
                    const SizedBox(height: 18),
                    _section(
                      title: 'Shop Identity',
                      children: [
                        _field(_shopName1, 'Shop Name 1'),
                        _field(_shopName2, 'Shop Name 2'),
                        _field(_addressLine1, 'Address Line 1'),
                        _field(_addressLine2, 'Address Line 2'),
                        _field(_addressLine3, 'Address Line 3'),
                      ],
                    ),
                    const SizedBox(height: 18),
                    _section(
                      title: 'Contact',
                      children: [
                        _field(
                          _phoneNo1,
                          'Phone No. 1',
                          keyboardType: TextInputType.phone,
                        ),
                        _field(
                          _phoneNo2,
                          'Phone No. 2',
                          keyboardType: TextInputType.phone,
                        ),
                        _field(_cashierName, 'Cashier Name'),
                      ],
                    ),
                    const SizedBox(height: 18),
                    _section(
                      title: 'Operations',
                      children: [
                        _currencyDropdown(),
                        _toggleRow(
                          label: 'Use Table',
                          value: _useTable,
                          onChanged: (v) => setState(() => _useTable = v),
                        ),
                        if (_useTable)
                          _field(
                            _noOfTable,
                            'No of Table',
                            keyboardType: TextInputType.number,
                          ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    _section(
                      title: 'Tax & Compliance',
                      children: [
                        _field(_countryName, 'Country Name'),
                        _field(_stateName, 'State Name'),
                        _toggleRow(
                          label: 'GST',
                          value: _gstEnabled,
                          onChanged: (v) => setState(() => _gstEnabled = v),
                        ),
                        _field(_gstNumber, 'GST Number'),
                        Row(
                          children: [
                            Expanded(
                              child: _field(
                                _shopCgst,
                                'Shop CGST',
                                keyboardType:
                                    const TextInputType.numberWithOptions(
                                  decimal: true,
                                ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: _field(
                                _shopSgst,
                                'Shop SGST',
                                keyboardType:
                                    const TextInputType.numberWithOptions(
                                  decimal: true,
                                ),
                              ),
                            ),
                          ],
                        ),
                        _field(_panNumber, 'PAN Number'),
                        _field(_companyFssis, 'shop FSSAI Number'),
                      ],
                    ),
                    const SizedBox(height: 18),
                    _section(
                      title: 'Payment UPI',
                      children: [
                        _field(
                          _paymentLogo,
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
                if (_busy)
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
                isLoading: _busy,
                onPressed: _save,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
