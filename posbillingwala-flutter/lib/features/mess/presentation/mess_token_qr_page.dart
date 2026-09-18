import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/print/domain/bluetooth_printer_hub.dart';
import 'package:pos_billingwala_v2/features/print/domain/print_providers.dart';
import 'package:pos_billingwala_v2/features/print/domain/printer_settings.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';
import 'package:qr_flutter/qr_flutter.dart';

String messTokenDigits(String? value) =>
    (value ?? '').replaceAll(RegExp(r'\D'), '');

bool messTokenHasRequiredIdentity(String? name, String? mobile) {
  return (name ?? '').trim().isNotEmpty && messTokenDigits(mobile).length == 10;
}

class MessTokenQrPage extends ConsumerWidget {
  const MessTokenQrPage({
    super.key,
    required this.title,
    required this.payload,
    required this.subtitle,
    this.tokenCode,
    this.messType,
    this.memberMobile,
  });

  final String title;
  final String payload;
  final String subtitle;
  final String? tokenCode;
  final String? messType;
  final String? memberMobile;

  Future<void> messTokenQrPagePrint(BuildContext context, WidgetRef ref) async {
    if (!messTokenHasRequiredIdentity(subtitle, memberMobile)) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(AppStrings.of(ref).tokenPrintNameMobileRequired),
        ),
      );
      return;
    }
    final width = ref.read(printerSettingsProvider).charsPerLine;
    final time = DateFormat('dd-MM-yyyy HH:mm').format(DateTime.now());
    final mobile = messTokenDigits(memberMobile);
    final buf = StringBuffer()
      ..writeln(messTokenQrPageCenter('MESS TOKEN', width))
      ..writeln('-' * width)
      ..writeln(subtitle.trim())
      ..writeln(mobile)
      ..writeln(messType ?? 'Meal')
      ..writeln('Code: ${tokenCode ?? '-'}')
      ..writeln(time)
      ..writeln('-' * width)
      ..writeln(payload);
    final result = await ref
        .read(printServiceProvider)
        .printRawText(
          buf.toString(),
          channel: PrinterChannelKind.kot,
          label: 'Mess token',
        );
    if (!context.mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(result.message ?? 'Print done')));
  }

  String messTokenQrPageCenter(String text, int width) {
    if (text.length >= width) return text;
    final pad = (width - text.length) ~/ 2;
    return '${' ' * pad}$text';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          IconButton(
            tooltip: 'Print token',
            onPressed: () => messTokenQrPagePrint(context, ref),
            icon: const Icon(Icons.print_rounded),
          ),
          IconButton(
            tooltip: 'Copy payload',
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: payload));
              if (!context.mounted) return;
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('QR payload copied')),
              );
            },
            icon: const Icon(Icons.copy_rounded),
          ),
        ],
      ),
      body: ResponsiveScrollShell(
        dashboard: true,
        child: ListView(
          padding: EdgeInsets.all(
            AppBreakpoints.pagePaddingFor(context.widthClass) + 8,
          ),
          children: [
            Center(
              child: AppModuleIcon(
                icon: Icons.qr_code_2_rounded,
                color: AppColors.teal,
                size: 70,
              ),
            ),
            const SizedBox(height: 12),
            AppCard(
              accentColor: AppColors.primary,
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  const AppModuleIcon(
                    icon: Icons.qr_code_2_rounded,
                    color: AppColors.primary,
                    size: 58,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    subtitle,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  if ((memberMobile ?? '').trim().isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(
                      messTokenDigits(memberMobile),
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                  const SizedBox(height: 20),
                  QrImageView(
                    data: payload,
                    size: 240,
                    backgroundColor: Colors.white,
                    eyeStyle: const QrEyeStyle(
                      eyeShape: QrEyeShape.square,
                      color: AppColors.primary,
                    ),
                    dataModuleStyle: const QrDataModuleStyle(
                      dataModuleShape: QrDataModuleShape.square,
                      color: AppColors.primary,
                    ),
                  ),
                  if (tokenCode != null) ...[
                    const SizedBox(height: 16),
                    SelectableText(
                      tokenCode!,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                  const SizedBox(height: 12),
                  Text(
                    'Show this QR at the mess counter to verify the meal token.',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            AppButton(
              label: 'Print token',
              icon: Icons.print_rounded,
              expanded: false,
              onPressed: () => messTokenQrPagePrint(context, ref),
            ),
            const SizedBox(height: 8),
            AppButton(
              label: 'Done',
              onPressed: () => Navigator.of(context).pop(),
              variant: AppButtonVariant.outlined,
              expanded: false,
            ),
          ],
        ),
      ),
    );
  }
}
