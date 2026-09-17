import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:pos_billingwala_v2/core/constants/app_assets.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/support/data/support_api.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';

class CreateSupportTicketPage extends ConsumerStatefulWidget {
  const CreateSupportTicketPage({super.key});

  @override
  ConsumerState<CreateSupportTicketPage> createState() =>
      CreateSupportTicketPageState();
}

class CreateSupportTicketPageState
    extends ConsumerState<CreateSupportTicketPage> {
  final createSupportTicketPageSubject = TextEditingController();
  final createSupportTicketPageDescription = TextEditingController();
  String createSupportTicketPageCategory = 'Billing';
  String? createSupportTicketPageAttachmentPath;
  bool submitting = false;
  bool createSupportTicketPageOnline = true;

  static const categories = [
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
      if (mounted) setState(() => createSupportTicketPageOnline = online);
    });
  }

  @override
  void dispose() {
    createSupportTicketPageSubject.dispose();
    createSupportTicketPageDescription.dispose();
    super.dispose();
  }

  Future<void> pickAttachment() async {
    final picked = await ImagePicker().pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
    );
    if (picked == null) return;
    setState(() => createSupportTicketPageAttachmentPath = picked.path);
  }

  Future<void> submit() async {
    if (submitting) return;
    final subject = createSupportTicketPageSubject.text.trim();
    final description = createSupportTicketPageDescription.text.trim();
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

    setState(() => submitting = true);
    try {
      final api = SupportApi(ref.read(apiClientProvider));
      final result = await api.createSupportTicket(
        userId: userId,
        category: createSupportTicketPageCategory,
        subject: subject,
        description: description,
        attachmentPath: createSupportTicketPageAttachmentPath,
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
      if (mounted) setState(() => submitting = false);
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
          SupportOnlineBanner(online: createSupportTicketPageOnline),
          const SizedBox(height: 16),
          StringDropdownField(
            label: 'Category',
            value: createSupportTicketPageCategory,
            enableSearch: false,
            options: categories,
            onChanged: (v) {
              if (v != null) setState(() => createSupportTicketPageCategory = v);
            },
          ),
          const SizedBox(height: 14),
          AppTextField(
            controller: createSupportTicketPageSubject,
            label: 'Subject',
            hint: 'Enter subject',
            prefixIcon: Icons.edit_outlined,
          ),
          const SizedBox(height: 14),
          AppTextField(
            controller: createSupportTicketPageDescription,
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
              onTap: pickAttachment,
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
                            createSupportTicketPageAttachmentPath == null
                                ? 'Upload screenshots or documents'
                                : createSupportTicketPageAttachmentPath!
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
            isLoading: submitting,
            onPressed: submitting ? null : submit,
          ),
        ],
      ),
      ),
    );
  }
}
