import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pos_billingwala_v2/core/database/app_database.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_providers.dart';
import 'package:pos_billingwala_v2/features/mess/domain/mess_slip_builder.dart';
import 'package:pos_billingwala_v2/features/mess/presentation/mess_slip_preview.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_service.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';

String messTokenDigits(String? value) =>
    (value ?? '').replaceAll(RegExp(r'\D'), '');

bool messTokenHasRequiredIdentity(String? name, String? mobile) {
  return (name ?? '').trim().isNotEmpty && messTokenDigits(mobile).length == 10;
}

class MessTokenQrPage extends ConsumerStatefulWidget {
  const MessTokenQrPage({
    super.key,
    required this.title,
    required this.payload,
    required this.subtitle,
    this.tokenCode,
    this.messType,
    this.memberMobile,
    this.member,
    this.commitAfterPrint = false,
  });

  final String title;
  final String payload;
  final String subtitle;
  final String? tokenCode;
  final String? messType;
  final String? memberMobile;
  final MessMember? member;
  /* When true, persist token+invoice only after successful print (Android). */
  final bool commitAfterPrint;

  @override
  ConsumerState<MessTokenQrPage> createState() => MessTokenQrPageState();
}

class MessTokenQrPageState extends ConsumerState<MessTokenQrPage> {
  bool busy = false;
  bool committed = false;

  Future<void> messTokenQrPagePrint() async {
    if (!messTokenHasRequiredIdentity(widget.subtitle, widget.memberMobile)) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(AppStrings.of(ref).tokenPrintNameMobileRequired),
        ),
      );
      return;
    }
    setState(() => busy = true);
    try {
      final profile = ref.read(shopReceiptProfileProvider);
      final mobile = messTokenDigits(widget.memberMobile);
      final layout = MessSlipBuilder.qrTokenLayout(
        profile: profile,
        memberName: widget.subtitle,
        memberMobile: mobile,
        messType: widget.messType ?? 'Meal',
        tokenCode: widget.tokenCode ?? '',
        qrPayload: widget.payload,
      );
      final result = await ref.read(printServiceProvider).printMessSlip(
        layout.toPlainText(
          width: ref.read(printerSettingsProvider).charsPerLine,
        ),
        layout: layout,
        qrPayload: widget.payload,
        channel: PrinterChannelKind.bill,
        label: 'Mess QR token',
      );
      final ok =
          result.outcome != PrintOutcome.failed &&
          result.outcome != PrintOutcome.previewOnly;
      if (ok &&
          widget.commitAfterPrint &&
          !committed &&
          widget.member != null &&
          (widget.tokenCode ?? '').isNotEmpty) {
        await ref.read(messControllerProvider.notifier).commitMemberToken(
          member: widget.member!,
          tokenCode: widget.tokenCode!,
          messType: widget.messType ?? 'Lunch',
        );
        committed = true;
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result.message ?? 'Print done')),
      );
      if (ok && widget.commitAfterPrint) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profile = ref.watch(shopReceiptProfileProvider);
    final mobile = messTokenDigits(widget.memberMobile);
    final layout = messQrTokenPreviewLayout(
      profile: profile,
      memberName: widget.subtitle,
      memberMobile: mobile,
      messType: widget.messType ?? 'Meal',
      tokenCode: widget.tokenCode ?? '',
      qrPayload: widget.payload,
    );

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        actions: [
          IconButton(
            tooltip: 'Print token',
            onPressed: busy ? null : messTokenQrPagePrint,
            icon: const Icon(Icons.print_rounded),
          ),
          IconButton(
            tooltip: 'Copy payload',
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: widget.payload));
              if (!context.mounted) return;
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('QR payload copied')),
              );
            },
            icon: const Icon(Icons.copy_rounded),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ResponsiveScrollShell(
              dashboard: true,
              child: ListView(
                padding: EdgeInsets.all(
                  AppBreakpoints.pagePaddingFor(context.widthClass) + 8,
                ),
                children: [
                  Text(
                    'Preview uses bill printer paper size and logo setting.',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.black54,
                    ),
                  ),
                  const SizedBox(height: 8),
                  MessSlipPreview(layout: layout),
                ],
              ),
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: EdgeInsets.fromLTRB(
                AppBreakpoints.pagePaddingFor(context.widthClass),
                8,
                AppBreakpoints.pagePaddingFor(context.widthClass),
                12,
              ),
              child: AppButton(
                label: 'Print QR Token',
                icon: Icons.print_rounded,
                isLoading: busy,
                onPressed: busy ? null : messTokenQrPagePrint,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
