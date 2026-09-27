import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/purchase_providers.dart';
import 'package:pos_billingwala_v2/features/purchase/domain/vendor.dart';

class VendorFormPage extends ConsumerStatefulWidget {
  const VendorFormPage({super.key, this.vendorId});

  final String? vendorId;

  @override
  ConsumerState<VendorFormPage> createState() => VendorFormPageState();
}

class VendorFormPageState extends ConsumerState<VendorFormPage> {
  final name = TextEditingController();
  final gstin = TextEditingController();
  final contact = TextEditingController();
  final mobile = TextEditingController();
  final email = TextEditingController();
  final address = TextEditingController();
  final terms = TextEditingController(text: 'Net 30');
  final credit = TextEditingController(text: '0');
  final opening = TextEditingController(text: '0');
  final bank = TextEditingController();
  var status = 'ACTIVE';
  var busy = false;
  var loaded = false;

  bool get isEdit => widget.vendorId != null && widget.vendorId!.isNotEmpty;

  @override
  void initState() {
    super.initState();
    Future.microtask(_hydrate);
  }

  @override
  void dispose() {
    name.dispose();
    gstin.dispose();
    contact.dispose();
    mobile.dispose();
    email.dispose();
    address.dispose();
    terms.dispose();
    credit.dispose();
    opening.dispose();
    bank.dispose();
    super.dispose();
  }

  Future<void> _hydrate() async {
    if (!isEdit) {
      setState(() => loaded = true);
      return;
    }
    final list = await ref.read(vendorsProvider.future);
    Vendor? match;
    for (final v in list) {
      if (v.id == widget.vendorId) {
        match = v;
        break;
      }
    }
    if (!mounted) return;
    if (match != null) {
      name.text = match.name;
      gstin.text = match.gstin;
      contact.text = match.contactName;
      mobile.text = match.mobile;
      email.text = match.email;
      address.text = match.address;
      terms.text = match.paymentTerms;
      credit.text = match.creditLimit.toStringAsFixed(0);
      opening.text = match.openingBalance.toStringAsFixed(0);
      bank.text = match.bankDetails;
      status = match.status;
    }
    setState(() => loaded = true);
  }

  Future<void> save() async {
    setState(() => busy = true);
    try {
      await ref.read(purchaseControllerProvider.notifier).saveVendor(
            id: widget.vendorId,
            name: name.text,
            gstin: gstin.text,
            contactName: contact.text,
            mobile: mobile.text,
            email: email.text,
            address: address.text,
            paymentTerms: terms.text,
            creditLimit: double.tryParse(credit.text.trim()) ?? 0,
            openingBalance: double.tryParse(opening.text.trim()) ?? 0,
            bankDetails: bank.text,
            status: status,
          );
      if (!mounted) return;
      context.pop();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e'.replaceFirst('Bad state: ', ''))),
      );
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!loaded) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(title: Text(isEdit ? 'Edit vendor' : 'Add vendor')),
      body: ResponsivePageBody(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AppTextField(controller: name, label: 'Vendor name *'),
            const SizedBox(height: 12),
            ResponsiveFormColumns(
              children: [
                AppTextField(controller: gstin, label: 'GSTIN'),
                AppTextField(
                  controller: mobile,
                  label: 'Mobile',
                  keyboardType: TextInputType.phone,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                AppTextField(controller: contact, label: 'Contact person'),
                AppTextField(
                  controller: email,
                  label: 'Email',
                  keyboardType: TextInputType.emailAddress,
                ),
                AppTextField(controller: terms, label: 'Payment terms'),
                AppTextField(
                  controller: credit,
                  label: 'Credit limit',
                  keyboardType: TextInputType.number,
                ),
                AppTextField(
                  controller: opening,
                  label: 'Opening balance',
                  keyboardType: TextInputType.number,
                ),
              ],
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: address,
              label: 'Address',
              maxLines: 2,
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: bank,
              label: 'Bank details',
              maxLines: 2,
            ),
            const SizedBox(height: 12),
            AppDropdownFormField<String>(
              label: 'Status',
              value: status,
              items: const ['ACTIVE', 'INACTIVE'],
              itemLabel: (v) => v == 'ACTIVE' ? 'Active' : 'Inactive',
              onChanged: (v) {
                if (v != null) setState(() => status = v);
              },
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: busy ? null : save,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(52),
                backgroundColor: AppColors.primary,
              ),
              child: Text(busy ? 'Saving…' : 'Save vendor'),
            ),
          ],
        ),
      ),
    );
  }
}
