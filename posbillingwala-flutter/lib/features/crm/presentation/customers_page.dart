import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/crm/domain/crm_providers.dart';

class CustomersPage extends ConsumerWidget {
  const CustomersPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(customersProvider);
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Customers / CRM'),
        actions: [
          IconButton(
            onPressed: () => ref.invalidate(customersProvider),
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/crm/customers/new'),
        icon: const Icon(Icons.person_add_alt_1_rounded),
        label: const Text('Add customer'),
      ),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (list) {
          if (list.isEmpty) {
            return const Center(child: Text('No customers yet'));
          }
          return ResponsiveScrollShell(
            child: ListView.separated(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                16,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                100,
              ),
              itemCount: list.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, i) {
                final c = list[i];
                return Material(
                  color: AppColors.glassSolid,
                  borderRadius: BorderRadius.circular(14),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => context.push('/crm/customers/${c.id}'),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          CircleAvatar(
                            backgroundColor: AppColors.cyan.withValues(
                              alpha: .15,
                            ),
                            child: Text(
                              c.name.isEmpty ? '?' : c.name[0].toUpperCase(),
                              style: const TextStyle(
                                fontFamily: AppFonts.family,
                                fontWeight: FontWeight.w800,
                                color: AppColors.cyan,
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  c.name,
                                  style: const TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontWeight: FontWeight.w800,
                                    color: AppColors.navy,
                                  ),
                                ),
                                Text(
                                  [
                                    if (c.mobile.isNotEmpty) c.mobile,
                                    if (c.membership.isNotEmpty) c.membership,
                                    'Pts ${c.loyaltyPoints.toStringAsFixed(0)}',
                                  ].join(' · '),
                                  style: TextStyle(
                                    fontFamily: AppFonts.family,
                                    fontSize: 12.5,
                                    color: AppColors.navy.withValues(
                                      alpha: .5,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class CustomerFormPage extends ConsumerStatefulWidget {
  const CustomerFormPage({super.key, this.customerId});

  final String? customerId;

  @override
  ConsumerState<CustomerFormPage> createState() => CustomerFormPageState();
}

class CustomerFormPageState extends ConsumerState<CustomerFormPage> {
  final name = TextEditingController();
  final mobile = TextEditingController();
  final email = TextEditingController();
  final address = TextEditingController();
  final gstin = TextEditingController();
  final credit = TextEditingController(text: '0');
  final wallet = TextEditingController(text: '0');
  final points = TextEditingController(text: '0');
  final membership = TextEditingController();
  final notes = TextEditingController();
  var busy = false;
  var loaded = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(_hydrate);
  }

  @override
  void dispose() {
    name.dispose();
    mobile.dispose();
    email.dispose();
    address.dispose();
    gstin.dispose();
    credit.dispose();
    wallet.dispose();
    points.dispose();
    membership.dispose();
    notes.dispose();
    super.dispose();
  }

  Future<void> _hydrate() async {
    final id = widget.customerId;
    if (id != null && id.isNotEmpty) {
      final list = await ref.read(customersProvider.future);
      for (final c in list) {
        if (c.id == id) {
          name.text = c.name;
          mobile.text = c.mobile;
          email.text = c.email;
          address.text = c.address;
          gstin.text = c.gstin;
          credit.text = c.creditLimit.toStringAsFixed(0);
          wallet.text = c.walletBalance.toStringAsFixed(0);
          points.text = c.loyaltyPoints.toStringAsFixed(0);
          membership.text = c.membership;
          notes.text = c.notes;
          break;
        }
      }
    }
    if (mounted) setState(() => loaded = true);
  }

  Future<void> save() async {
    setState(() => busy = true);
    try {
      await ref.read(crmControllerProvider.notifier).save(
            id: widget.customerId,
            name: name.text,
            mobile: mobile.text,
            email: email.text,
            address: address.text,
            gstin: gstin.text,
            creditLimit: double.tryParse(credit.text) ?? 0,
            walletBalance: double.tryParse(wallet.text) ?? 0,
            loyaltyPoints: double.tryParse(points.text) ?? 0,
            membership: membership.text,
            notes: notes.text,
          );
      if (mounted) context.pop();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e'.replaceFirst('Bad state: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!loaded) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.customerId == null ? 'Add customer' : 'Edit customer',
        ),
      ),
      body: ResponsivePageBody(
        child: Column(
          children: [
            AppTextField(controller: name, label: 'Name *'),
            const SizedBox(height: 12),
            ResponsiveFormColumns(
              children: [
                AppTextField(
                  controller: mobile,
                  label: 'Mobile',
                  keyboardType: TextInputType.phone,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                ),
                AppTextField(controller: email, label: 'Email'),
                AppTextField(controller: gstin, label: 'GSTIN'),
                AppTextField(controller: membership, label: 'Membership'),
                AppTextField(
                  controller: credit,
                  label: 'Credit limit',
                  keyboardType: TextInputType.number,
                ),
                AppTextField(
                  controller: wallet,
                  label: 'Wallet',
                  keyboardType: TextInputType.number,
                ),
                AppTextField(
                  controller: points,
                  label: 'Loyalty points',
                  keyboardType: TextInputType.number,
                ),
              ],
            ),
            const SizedBox(height: 12),
            AppTextField(controller: address, label: 'Address', maxLines: 2),
            const SizedBox(height: 12),
            AppTextField(controller: notes, label: 'Notes', maxLines: 2),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: busy ? null : save,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(52),
                backgroundColor: AppColors.primary,
              ),
              child: Text(busy ? 'Saving…' : 'Save customer'),
            ),
          ],
        ),
      ),
    );
  }
}
