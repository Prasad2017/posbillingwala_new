import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_typography.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/auth/domain/auth_controller.dart';
import 'package:pos_billingwala_v2/features/support/data/support_api.dart';
import 'package:pos_billingwala_v2/features/support/data/support_dtos.dart';
import 'package:pos_billingwala_v2/features/support/presentation/support_widgets.dart';

class SupportTicketDetailPage extends ConsumerStatefulWidget {
  const SupportTicketDetailPage({super.key, required this.ticketId});

  final String ticketId;

  @override
  ConsumerState<SupportTicketDetailPage> createState() =>
      SupportTicketDetailPageState();
}

class SupportTicketDetailPageState
    extends ConsumerState<SupportTicketDetailPage> {
  SupportTicketDetailsDto? supportTicketDetailPageDetails;
  bool loading = false;
  bool sending = false;
  bool supportTicketDetailPageOnline = true;
  bool oldestFirst = true;
  String? error;
  final replyController = TextEditingController();
  final scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => reload());
  }

  @override
  void dispose() {
    replyController.dispose();
    scrollController.dispose();
    super.dispose();
  }

  Future<void> reload() async {
    final online = await checkOnline();
    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) {
      setState(() {
        supportTicketDetailPageOnline = online;
        error = 'Please login first';
      });
      return;
    }
    setState(() {
      supportTicketDetailPageOnline = online;
      loading = true;
      error = null;
    });
    try {
      final api = SupportApi(ref.read(apiClientProvider));
      final details = await api.getTicketDetails(userId, widget.ticketId);
      if (!mounted) return;
      setState(() {
        supportTicketDetailPageDetails = details;
        loading = false;
      });
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!scrollController.hasClients) return;
        if (oldestFirst) {
          scrollController.jumpTo(0);
        } else {
          scrollController.jumpTo(scrollController.position.maxScrollExtent);
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        error = '$e';
        loading = false;
      });
    }
  }

  Future<void> sendReply() async {
    final details = supportTicketDetailPageDetails;
    if (details == null || details.isClosed || sending) return;
    final message = replyController.text.trim();
    if (message.isEmpty) return;

    final userId = ref.read(authControllerProvider).session?.userId;
    if (userId == null || userId.isEmpty) return;

    setState(() => sending = true);
    try {
      final api = SupportApi(ref.read(apiClientProvider));
      final result = await api.replyTicket(
        userId: userId,
        ticketId: widget.ticketId,
        message: message,
      );
      if (!mounted) return;
      if (result.isSuccess) {
        replyController.clear();
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(result.message ?? 'Reply sent')));
        await reload();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message ?? 'Failed to send reply')),
        );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => sending = false);
    }
  }

  List<ChatLine> get supportTicketDetailPageLines {
    final details = supportTicketDetailPageDetails;
    if (details == null) return const [];
    final lines = <ChatLine>[];
    if (details.description.isNotEmpty) {
      lines.add(
        ChatLine(
          isSupport: false,
          message: details.description,
          createdAt: details.createdAt,
          senderLabel: 'You',
        ),
      );
    }
    for (final m in details.messages) {
      lines.add(
        ChatLine(
          isSupport: !m.isFromUser,
          message: m.message,
          createdAt: m.createdAt,
          senderLabel: m.isFromUser
              ? 'You'
              : (m.sender.isEmpty ? 'Support team' : m.sender),
        ),
      );
    }
    if (!oldestFirst) {
      return lines.reversed.toList();
    }
    return lines;
  }

  @override
  Widget build(BuildContext context) {
    final details = supportTicketDetailPageDetails;
    final closed = details?.isClosed ?? false;
    final statusColor = ticketStatusColor(details?.status ?? '');

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text('Ticket Details'),
        actions: [
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert_rounded, color: Colors.white),
            onSelected: (v) {
              if (v == 'refresh') reload();
              if (v == 'call') callSupport(context);
              if (v == 'new') context.push('/support/create');
            },
            itemBuilder: (context) => const [
              PopupMenuItem(value: 'refresh', child: Text('Refresh')),
              PopupMenuItem(value: 'call', child: Text('Call support')),
              PopupMenuItem(value: 'new', child: Text('Open new ticket')),
            ],
          ),
        ],
      ),
      body: error != null && details == null
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(error!, textAlign: TextAlign.center),
                    const SizedBox(height: 12),
                    AppButton(label: 'Retry', onPressed: reload),
                  ],
                ),
              ),
            )
          : Column(
              children: [
                Expanded(
                  child: loading && details == null
                      ? const Center(child: CircularProgressIndicator())
                      : ResponsiveScrollShell(
                          dashboard: true,
                          child: ListView(
                            controller: scrollController,
                            padding: EdgeInsets.fromLTRB(
                              AppBreakpoints.pagePaddingFor(context.widthClass),
                              16,
                              AppBreakpoints.pagePaddingFor(context.widthClass),
                              16,
                            ),
                            children: [
                              SupportOnlineBanner(
                                online: supportTicketDetailPageOnline,
                                title: 'How support tickets work',
                              ),
                              if (closed) ...[
                                const SizedBox(height: 12),
                                Container(
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: AppColors.danger.withValues(
                                      alpha: 0.08,
                                    ),
                                    borderRadius: BorderRadius.circular(14),
                                    border: Border.all(
                                      color: AppColors.danger.withValues(
                                        alpha: 0.35,
                                      ),
                                    ),
                                  ),
                                  child: Row(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      const Icon(
                                        Icons.lock_rounded,
                                        color: AppColors.danger,
                                        size: 20,
                                      ),
                                      const SizedBox(width: 10),
                                      Expanded(
                                        child: Text(
                                          'Status: Closed — this ticket is closed. Open a new ticket if you need more help.',
                                          style:
                                              AppTypography.bodySmall(
                                                color: AppColors.danger,
                                              ).copyWith(
                                                fontWeight: FontWeight.w600,
                                              ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                              if (details != null) ...[
                                const SizedBox(height: 12),
                                Container(
                                  padding: const EdgeInsets.all(14),
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    borderRadius: BorderRadius.circular(16),
                                    border: Border.all(color: AppColors.border),
                                  ),
                                  child: Row(
                                    children: [
                                      Container(
                                        width: 42,
                                        height: 42,
                                        decoration: BoxDecoration(
                                          color: AppColors.primaryLight,
                                          borderRadius: BorderRadius.circular(
                                            12,
                                          ),
                                        ),
                                        alignment: Alignment.center,
                                        child: const Icon(
                                          Icons.confirmation_number_outlined,
                                          color: AppColors.primary,
                                          size: 22,
                                        ),
                                      ),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              (details.ticketNo ?? '')
                                                      .isNotEmpty
                                                  ? details.ticketNo!
                                                  : 'Ticket',
                                              style: AppTypography.cardTitle(),
                                            ),
                                            if (details.subject.isNotEmpty) ...[
                                              const SizedBox(height: 2),
                                              Text(
                                                details.subject,
                                                style:
                                                    AppTypography.bodySmall(),
                                              ),
                                            ],
                                          ],
                                        ),
                                      ),
                                      if (details.status.isNotEmpty)
                                        Container(
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 10,
                                            vertical: 5,
                                          ),
                                          decoration: BoxDecoration(
                                            color: statusColor.withValues(
                                              alpha: 0.12,
                                            ),
                                            borderRadius: BorderRadius.circular(
                                              999,
                                            ),
                                          ),
                                          child: Text(
                                            details.status,
                                            style: TextStyle(
                                              color: statusColor,
                                              fontWeight: FontWeight.w700,
                                              fontSize: 12,
                                            ),
                                          ),
                                        ),
                                    ],
                                  ),
                                ),
                              ],
                              const SizedBox(height: 18),
                              Row(
                                children: [
                                  Text(
                                    'Conversation',
                                    style: AppTypography.sectionTitle()
                                        .copyWith(fontSize: 16),
                                  ),
                                  const Spacer(),
                                  InkWell(
                                    onTap: () => setState(
                                      () => oldestFirst = !oldestFirst,
                                    ),
                                    borderRadius: BorderRadius.circular(8),
                                    child: Padding(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 4,
                                        vertical: 4,
                                      ),
                                      child: Row(
                                        children: [
                                          Text(
                                            oldestFirst
                                                ? 'Oldest first'
                                                : 'Newest first',
                                            style: AppTypography.bodySmall(),
                                          ),
                                          const SizedBox(width: 4),
                                          const Icon(
                                            Icons.filter_list_rounded,
                                            size: 16,
                                            color: AppColors.textSecondary,
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 10),
                              if (supportTicketDetailPageLines.isEmpty)
                                const Padding(
                                  padding: EdgeInsets.symmetric(vertical: 32),
                                  child: Center(child: Text('No messages yet')),
                                )
                              else
                                ...supportTicketDetailPageLines
                                    .asMap()
                                    .entries
                                    .map((entry) {
                                      final index = entry.key;
                                      final line = entry.value;
                                      final isLast =
                                          index ==
                                          supportTicketDetailPageLines.length -
                                              1;
                                      return MessageBubble(
                                        line: line,
                                        showConnector: !isLast,
                                      );
                                    }),
                            ],
                          ),
                        ),
                ),
                SafeArea(
                  top: false,
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      border: Border(top: BorderSide(color: AppColors.border)),
                    ),
                    child: closed
                        ? Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Container(
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: AppColors.surface,
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(
                                      Icons.lock_outline_rounded,
                                      size: 18,
                                      color: AppColors.textSecondary,
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: Text(
                                        'Reply is disabled. This ticket is closed. Open a new ticket if you need more help.',
                                        style: AppTypography.bodySmall(),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 10),
                              AppButton(
                                label: 'Open New Ticket',
                                icon: Icons.add_rounded,
                                variant: AppButtonVariant.outlined,
                                onPressed: () =>
                                    context.push('/support/create'),
                              ),
                            ],
                          )
                        : Column(
                            children: [
                              AppTextField(
                                controller: replyController,
                                enabled: !sending,
                                minLines: 1,
                                maxLines: 4,
                                hint: 'Type a reply…',
                                textCapitalization:
                                    TextCapitalization.sentences,
                                onSubmitted: (_) => sendReply(),
                              ),
                              const SizedBox(height: 8),
                              Row(
                                children: [
                                  Expanded(
                                    child: AppButton(
                                      label: 'Refresh',
                                      variant: AppButtonVariant.outlined,
                                      onPressed: loading ? null : reload,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: AppButton(
                                      label: 'Send',
                                      isLoading: sending,
                                      onPressed: sending ? null : sendReply,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                  ),
                ),
              ],
            ),
    );
  }
}

class ChatLine {
  const ChatLine({
    required this.isSupport,
    required this.message,
    required this.senderLabel,
    this.createdAt,
  });

  final bool isSupport;
  final String message;
  final String senderLabel;
  final String? createdAt;
}

class MessageBubble extends StatelessWidget {
  const MessageBubble({
    super.key,
    required this.line,
    required this.showConnector,
  });

  final ChatLine line;
  final bool showConnector;

  @override
  Widget build(BuildContext context) {
    final initial = line.senderLabel.trim().isEmpty
        ? '?'
        : line.senderLabel.trim()[0].toUpperCase();
    final avatarColor = line.isSupport ? AppColors.green : AppColors.primary;

    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 36,
            child: Column(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: avatarColor.withValues(alpha: 0.14),
                    shape: BoxShape.circle,
                  ),
                  child: Text(
                    initial,
                    style: TextStyle(
                      color: avatarColor,
                      fontWeight: FontWeight.w800,
                      fontSize: 14,
                    ),
                  ),
                ),
                if (showConnector)
                  Expanded(
                    child: Container(
                      width: 2,
                      margin: const EdgeInsets.symmetric(vertical: 4),
                      color: AppColors.border,
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16, top: 2),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    line.isSupport &&
                            line.senderLabel.toLowerCase() == 'support'
                        ? 'Support team'
                        : line.senderLabel,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 13,
                      color: AppColors.navy,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    line.message,
                    style: const TextStyle(
                      fontSize: 13,
                      height: 1.35,
                      color: AppColors.navy,
                    ),
                  ),
                  if ((line.createdAt ?? '').isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(
                      line.createdAt!,
                      style: TextStyle(
                        fontSize: 11,
                        color: Theme.of(context).hintColor,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
