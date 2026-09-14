import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgtes/widgtes.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/support/data/support_api.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';

class CreateSupportTicketPage extends ConsumerStatefulWidget {
  const CreateSupportTicketPage({super.key});

  @override
  ConsumerState<CreateSupportTicketPage> createState() =>
      _CreateSupportTicketPageState();
}

class _CreateSupportTicketPageState
    extends ConsumerState<CreateSupportTicketPage> {
  final _subject = TextEditingController();
  final _description = TextEditingController();
  String _category = 'Billing';
  String? _attachmentPath;
  bool _submitting = false;
  bool _online = true;

  static const _categories = [
    'Billing',
    'General',
    'Printer',
    'Sync',
    'Mess',
    'Account',
    'Other',
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final online = await checkOnline();
      if (mounted) setState(() => _online = online);
    });
  }

  @override
  void dispose() {
    _subject.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _pickAttachment() async {
    final picked = await ImagePicker().pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
    );
    if (picked == null) return;
    setState(() => _attachmentPath = picked.path);
  }

  Future<void> _submit() async {
    if (_submitting) return;
    final subject = _subject.text.trim();
    final description = _description.text.trim();
    if (subject.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a subject')),
      );
      return;
    }
    if (description.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please describe your issue')),
      );
      return;
    }

    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please login first')),
      );
      return;
    }

    setState(() => _submitting = true);
    try {
      final api = SupportApi(ref.read(apiClientProvider));
      final result = await api.createSupportTicket(
        userId: userId,
        category: _category,
        subject: subject,
        description: description,
        attachmentPath: _attachmentPath,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            result.isSuccess
                ? (result.message ?? 'Ticket created')
                : (result.message ?? 'Failed to create ticket'),
          ),
        ),
      );
      if (result.isSuccess) {
        context.go('/support/tickets');
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$e')),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text('Create Support Ticket'),
        actions: [
          IconButton(
            tooltip: 'Call support',
            onPressed: () => callSupport(context),
            icon: const AppSvg(
              AppAssets.svgHeadset,
              width: 22,
              height: 22,
              color: Colors.white,
            ),
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
        padding: EdgeInsets.fromLTRB(
            AppBreakpoints.pagePaddingFor(context.widthClass),
            16,
            AppBreakpoints.pagePaddingFor(context.widthClass),
            28),
        children: [
          SupportOnlineBanner(online: _online),
          const SizedBox(height: 16),
          StringDropdownField(
            label: 'Category',
            value: _category,
            enableSearch: false,
            options: _categories,
            onChanged: (v) {
              if (v != null) setState(() => _category = v);
            },
          ),
          const SizedBox(height: 14),
          AppTextField(
            controller: _subject,
            label: 'Subject',
            hint: 'Enter subject',
            prefixIcon: Icons.edit_outlined,
          ),
          const SizedBox(height: 14),
          AppTextField(
            controller: _description,
            label: 'Describe your issue',
            hint: 'Type your issue in detail...',
            maxLines: 5,
            maxLength: 1000,
            onChanged: (_) => setState(() {}),
            prefixIcon: Icons.account_tree_outlined,
          ),
          const SizedBox(height: 12),
          Material(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: _pickAttachment,
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: const BoxDecoration(
                        color: AppColors.primaryLight,
                        shape: BoxShape.circle,
                      ),
                      alignment: Alignment.center,
                      child: const Icon(
                        Icons.cloud_upload_outlined,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Add Attachment (Optional)',
                            style: AppTypography.cardTitle(),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            _attachmentPath == null
                                ? 'Upload screenshots or documents'
                                : _attachmentPath!
                                    .split(RegExp(r'[\\/]'))
                                    .last,
                            style: AppTypography.bodySmall(),
                          ),
                        ],
                      ),
                    ),
                    const Icon(
                      Icons.chevron_right_rounded,
                      color: AppColors.textSecondary,
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 20),
          AppButton(
            label: 'SUBMIT TICKET',
            icon: Icons.send_rounded,
            isLoading: _submitting,
            onPressed: _submitting ? null : _submit,
          ),
        ],
      ),
      ),
    );
  }
}
