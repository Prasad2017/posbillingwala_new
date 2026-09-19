import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/constants/app_fonts.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/utils/app_platform.dart';
import 'package:pos_billingwala_v2/core/widgets/widgets.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/display_connection_manager.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_models.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_settings.dart';
import 'package:pos_billingwala_v2/features/print/domain/shop_receipt_profile.dart';
import 'package:pos_billingwala_v2/language/app_strings.dart';
import 'package:qr_flutter/qr_flutter.dart';

class PaymentDisplaySettingsPage extends ConsumerWidget {
  const PaymentDisplaySettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final strings = AppStrings.of(ref);
    final state = ref.watch(displayConnectionManagerProvider);
    final manager = ref.read(displayConnectionManagerProvider.notifier);
    final shop = ref.watch(shopReceiptProfileProvider);
    final padding = AppBreakpoints.pagePaddingFor(context.widthClass);

    return Scaffold(
      appBar: AppBar(title: Text(strings.paymentDisplayTitle)),
      body: AppPlatform.isWeb
          ? Center(
              child: Padding(
                padding: EdgeInsets.all(padding),
                child: Text(
                  strings.paymentDisplayAndroidOnly,
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : ListView(
              padding: EdgeInsets.all(padding),
              children: [
                AppCard(
                  accentColor: _statusColor(state.status),
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        strings.paymentDisplayTitle,
                        style: const TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w800,
                          fontSize: 18,
                        ),
                      ),
                      const SizedBox(height: 12),
                      _StatusRow(
                        label: strings.paymentDisplayStatus,
                        value: _statusLabel(strings, state),
                        color: _statusColor(state.status),
                      ),
                      const SizedBox(height: 8),
                      _StatusRow(
                        label: strings.paymentDisplayDevice,
                        value: state.connectedClients > 0
                            ? 'Chrome / Android'
                            : '—',
                      ),
                      const SizedBox(height: 8),
                      SelectableText(
                        '${strings.paymentDisplayLocalUrl}\n'
                        '${state.localUrl.isEmpty ? '—' : state.localUrl}',
                        style: TextStyle(
                          fontFamily: AppFonts.family,
                          fontSize: 13,
                          color: AppColors.navy.withValues(alpha: .75),
                        ),
                      ),
                      if (state.errorMessage != null) ...[
                        const SizedBox(height: 10),
                        Text(
                          state.errorMessage!,
                          style: const TextStyle(color: AppColors.danger),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                AppCard(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        strings.paymentDisplayConnect,
                        style: const TextStyle(
                          fontFamily: AppFonts.family,
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        strings.paymentDisplayPairHint,
                        style: TextStyle(
                          color: AppColors.navy.withValues(alpha: .65),
                        ),
                      ),
                      const SizedBox(height: 16),
                      if (state.pairingUrl.isNotEmpty) ...[
                        Center(
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: AppColors.border),
                            ),
                            child: QrImageView(
                              data: state.pairingUrl,
                              size: 220,
                              backgroundColor: Colors.white,
                            ),
                          ),
                        ),
                        const SizedBox(height: 10),
                        SelectableText(
                          state.pairingUrl,
                          textAlign: TextAlign.center,
                          style: const TextStyle(fontSize: 12),
                        ),
                        const SizedBox(height: 8),
                        TextButton.icon(
                          onPressed: () async {
                            await Clipboard.setData(
                              ClipboardData(text: state.pairingUrl),
                            );
                            if (!context.mounted) return;
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(strings.paymentDisplayUrlCopied),
                              ),
                            );
                          },
                          icon: const Icon(Icons.copy_rounded),
                          label: Text(strings.paymentDisplayCopyUrl),
                        ),
                        const SizedBox(height: 8),
                      ],
                      AppButton(
                        label: strings.paymentDisplayShowPairingQr,
                        icon: Icons.qr_code_2_rounded,
                        onPressed: () => manager.showPairingQr(),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: AppButton(
                              label: strings.paymentDisplayReconnect,
                              variant: AppButtonVariant.outlined,
                              onPressed: () => manager.reconnect(),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: AppButton(
                              label: strings.paymentDisplayDisconnect,
                              variant: AppButtonVariant.outlined,
                              onPressed: () async {
                                await manager.disconnectDisplays();
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      TextButton(
                        onPressed: () async {
                          if (state.serverRunning) {
                            await manager.stopServer();
                          } else {
                            await manager.startServer();
                          }
                        },
                        child: Text(
                          state.serverRunning
                              ? strings.paymentDisplayStopServer
                              : strings.paymentDisplayStartServer,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                AppCard(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Column(
                    children: [
                      SwitchListTile(
                        title: Text(strings.paymentDisplayAutoNewBills),
                        subtitle: Text(strings.paymentDisplayAutoNewBillsHint),
                        value: state.autoDisplayNewBills,
                        onChanged: (v) => manager.setAutoDisplayNewBills(v),
                      ),
                      ListTile(
                        title: Text(strings.paymentDisplayQrDuration),
                        subtitle: Text(
                          '${(state.qrDurationSeconds / 60).round()} ${strings.paymentDisplayMinutes}',
                        ),
                        trailing: const Icon(Icons.timer_outlined),
                        onTap: () async {
                          final picked = await showDialog<int>(
                            context: context,
                            builder: (ctx) => SimpleDialog(
                              title: Text(strings.paymentDisplayQrDuration),
                              children: [
                                for (final mins in const [5])
                                  SimpleDialogOption(
                                    onPressed: () =>
                                        Navigator.pop(ctx, mins * 60),
                                    child: Text(
                                      '$mins ${strings.paymentDisplayMinutes}',
                                    ),
                                  ),
                              ],
                            ),
                          );
                          if (picked != null) {
                            await manager.setQrDurationSeconds(picked);
                          }
                        },
                      ),
                      const Divider(height: 1),
                      ListTile(
                        title: Text(strings.paymentDisplayUpiSettings),
                        subtitle: Text(
                          shop.hasUpiId
                              ? PaymentDisplayBillPayload.maskUpi(shop.upiId)
                              : strings.paymentDisplayUpiNotConfigured,
                        ),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => context.push('/settings/company'),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  strings.paymentDisplayKeepAppOpenHint,
                  style: TextStyle(
                    color: AppColors.navy.withValues(alpha: .55),
                    fontSize: 12.5,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Default duration: '
                  '${PaymentDisplaySettings.defaultQrDurationSeconds ~/ 60} minutes',
                  style: TextStyle(
                    color: AppColors.navy.withValues(alpha: .45),
                    fontSize: 12,
                  ),
                ),
              ],
            ),
    );
  }

  Color _statusColor(PaymentDisplayConnectionStatus status) {
    switch (status) {
      case PaymentDisplayConnectionStatus.connected:
        return AppColors.green;
      case PaymentDisplayConnectionStatus.waitingForPair:
      case PaymentDisplayConnectionStatus.starting:
        return AppColors.orange;
      case PaymentDisplayConnectionStatus.error:
        return AppColors.danger;
      case PaymentDisplayConnectionStatus.stopped:
        return AppColors.primary;
    }
  }

  String _statusLabel(AppStrings strings, PaymentDisplayUiState state) {
    switch (state.status) {
      case PaymentDisplayConnectionStatus.connected:
        return '${strings.paymentDisplayConnected} (${state.connectedClients})';
      case PaymentDisplayConnectionStatus.waitingForPair:
        return strings.paymentDisplayWaitingPair;
      case PaymentDisplayConnectionStatus.starting:
        return strings.paymentDisplayStarting;
      case PaymentDisplayConnectionStatus.error:
        return strings.paymentDisplayError;
      case PaymentDisplayConnectionStatus.stopped:
        return strings.paymentDisplayStopped;
    }
  }
}

class _StatusRow extends StatelessWidget {
  const _StatusRow({
    required this.label,
    required this.value,
    this.color,
  });

  final String label;
  final String value;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        if (color != null) ...[
          Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 8),
        ],
        Text(
          '$label: ',
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
        Expanded(
          child: Text(
            value,
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: color ?? AppColors.navy,
            ),
          ),
        ),
      ],
    );
  }
}
