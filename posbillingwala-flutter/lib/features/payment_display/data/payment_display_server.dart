import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';

import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/logging/app_logger.dart';
import 'package:pos_billingwala_v2/features/payment_display/data/payment_display_local_ip.dart';
import 'package:pos_billingwala_v2/features/payment_display/domain/payment_display_models.dart';

typedef PaymentDisplayClientCallback = void Function(int clientCount);
typedef PaymentDisplayErrorCallback = void Function(String message);

/* Local HTTP + WebSocket server for browser payment display (no internet). */
class PaymentDisplayServer {
  PaymentDisplayServer({
    this.onClientCountChanged,
    this.onError,
  });

  static const preferredPorts = [8080, 8081, 8765, 9090, 18080];
  static const pairingTokenTtl = Duration(minutes: 30);
  static const assetRoot = 'assets/payment_display';

  final PaymentDisplayClientCallback? onClientCountChanged;
  final PaymentDisplayErrorCallback? onError;

  HttpServer? _server;
  String? _localIp;
  int? _port;
  String? _pairingToken;
  DateTime? _pairingTokenExpiresAt;
  final _clients = <_DisplayClient>[];
  PaymentDisplayBillPayload? _activeBill;
  bool _running = false;

  bool get isRunning => _running && _server != null;
  String? get localIp => _localIp;
  int? get port => _port;
  int get connectedClientCount =>
      _clients.where((c) => c.authenticated).length;
  PaymentDisplayBillPayload? get activeBill => _activeBill;

  String? get pairingUrl {
    if (_localIp == null || _port == null || _pairingToken == null) return null;
    if (_pairingTokenExpiresAt != null &&
        DateTime.now().isAfter(_pairingTokenExpiresAt!)) {
      return null;
    }
    return 'http://$_localIp:$_port/display?token=$_pairingToken';
  }

  String? get displayBaseUrl {
    if (_localIp == null || _port == null) return null;
    return 'http://$_localIp:$_port/display';
  }

  Future<bool> start({int? preferredPort}) async {
    if (isRunning) return true;
    _running = false;
    _localIp = await PaymentDisplayLocalIp.detect();
    if (_localIp == null) {
      onError?.call('Could not detect local IP. Enable hotspot or Wi-Fi.');
      AppLogger.warning('PAYMENT_DISPLAY_ERROR local_ip_missing');
      return false;
    }

    final ports = preferredPort == null
        ? preferredPorts
        : [preferredPort, ...preferredPorts.where((p) => p != preferredPort)];

    Object? lastError;
    for (final port in ports) {
      try {
        final server = await HttpServer.bind(InternetAddress.anyIPv4, port);
        _server = server;
        _port = port;
        _running = true;
        _rotatePairingToken();
        server.listen(_handleRequest, onError: (Object e, StackTrace st) {
          AppLogger.error('PAYMENT_DISPLAY_ERROR server_listen', e, st);
          onError?.call('Display server error');
        });
        AppLogger.info(
          'PAYMENT_DISPLAY_SERVER_STARTED port=$port ip=$_localIp',
        );
        return true;
      } catch (e) {
        lastError = e;
      }
    }

    AppLogger.error('PAYMENT_DISPLAY_ERROR bind_failed', lastError);
    onError?.call('Could not start display server. Port unavailable.');
    return false;
  }

  Future<void> stop() async {
    for (final client in List<_DisplayClient>.from(_clients)) {
      await client.close();
    }
    _clients.clear();
    await _server?.close(force: true);
    _server = null;
    _running = false;
    _port = null;
    _pairingToken = null;
    _pairingTokenExpiresAt = null;
    onClientCountChanged?.call(0);
    AppLogger.info('PAYMENT_DISPLAY_SERVER_STOPPED');
  }

  Future<void> refreshNetworkIdentity() async {
    final ip = await PaymentDisplayLocalIp.detect();
    if (ip != null && ip != _localIp) {
      _localIp = ip;
      AppLogger.info('PAYMENT_DISPLAY_RECONNECT ip_changed=$ip');
    }
  }

  void rotatePairingToken() {
    _rotatePairingToken();
    AppLogger.info('PAYMENT_DISPLAY_PAIRING_STARTED');
  }

  void setActiveBill(PaymentDisplayBillPayload? bill) {
    _activeBill = bill;
  }

  Future<void> broadcastBillUpdated(PaymentDisplayBillPayload bill) async {
    _activeBill = bill;
    await _broadcast(
      PaymentDisplayMessage(
        type: 'BILL_UPDATED',
        payload: bill.toWireJson(),
      ),
    );
    AppLogger.info(
      'PAYMENT_DISPLAY_BILL_SENT bill=${bill.billNumber} amount=${bill.amount}',
    );
  }

  Future<void> broadcastBillCleared({String reason = 'expired'}) async {
    _activeBill = null;
    await _broadcast(
      PaymentDisplayMessage(
        type: 'BILL_CLEARED',
        payload: {'reason': reason},
      ),
    );
    AppLogger.info('PAYMENT_DISPLAY_BILL_EXPIRED reason=$reason');
  }

  Future<void> disconnectAllClients() async {
    for (final client in List<_DisplayClient>.from(_clients)) {
      await client.close();
    }
    _clients.clear();
    onClientCountChanged?.call(0);
    AppLogger.info('PAYMENT_DISPLAY_DISCONNECTED all');
  }

  void _rotatePairingToken() {
    final random = Random.secure();
    final bytes = List<int>.generate(24, (_) => random.nextInt(256));
    _pairingToken = base64UrlEncode(bytes).replaceAll('=', '');
    _pairingTokenExpiresAt = DateTime.now().add(pairingTokenTtl);
  }

  bool _tokenValid(String? token) {
    if (token == null || token.isEmpty || _pairingToken == null) return false;
    if (_pairingTokenExpiresAt != null &&
        DateTime.now().isAfter(_pairingTokenExpiresAt!)) {
      return false;
    }
    return token == _pairingToken;
  }

  Future<void> _handleRequest(HttpRequest request) async {
    try {
      final path = request.uri.path;
      if (WebSocketTransformer.isUpgradeRequest(request) &&
          (path == '/display/ws' || path == '/ws')) {
        await _handleWebSocket(request);
        return;
      }

      switch (path) {
        case '/display':
        case '/display/':
        case '/':
          await _serveDisplayPage(request);
          return;
        case '/display/status':
          await _serveStatus(request);
          return;
        default:
          if (path.startsWith('/display/assets/')) {
            await _serveAsset(request, path.substring('/display/assets/'.length));
            return;
          }
          if (path.startsWith('/assets/')) {
            await _serveAsset(request, path.substring('/assets/'.length));
            return;
          }
          request.response.statusCode = HttpStatus.notFound;
          await request.response.close();
      }
    } catch (e, st) {
      AppLogger.error('PAYMENT_DISPLAY_ERROR request', e, st);
      try {
        request.response.statusCode = HttpStatus.internalServerError;
        await request.response.close();
      } catch (_) {}
    }
  }

  Future<void> _serveDisplayPage(HttpRequest request) async {
    final token = request.uri.queryParameters['token'];
    if (!_tokenValid(token)) {
      request.response.statusCode = HttpStatus.unauthorized;
      request.response.headers.contentType = ContentType.html;
      request.response.write(
        '<!DOCTYPE html><html><body style="font-family:sans-serif;text-align:center;padding:40px">'
        '<h2>Payment display</h2><p>Invalid or expired pairing link.</p>'
        '<p>Open Settings → Payment Display on the POS and scan again.</p>'
        '</body></html>',
      );
      await request.response.close();
      return;
    }
    await _serveAsset(request, 'index.html', asHtmlRoot: true);
  }

  Future<void> _serveStatus(HttpRequest request) async {
    final token = request.uri.queryParameters['token'];
    if (!_tokenValid(token)) {
      request.response.statusCode = HttpStatus.unauthorized;
      await request.response.close();
      return;
    }
    final bill = _activeBill;
    final validBill = bill != null && !bill.isExpired ? bill : null;
    request.response.headers.contentType = ContentType.json;
    request.response.write(
      jsonEncode({
        'connectedClients': connectedClientCount,
        'hasBill': validBill != null,
        'bill': validBill?.toWireJson(),
      }),
    );
    await request.response.close();
  }

  Future<void> _serveAsset(
    HttpRequest request,
    String relative, {
    bool asHtmlRoot = false,
  }) async {
    final safe = _sanitizeAssetPath(relative);
    if (safe == null) {
      request.response.statusCode = HttpStatus.forbidden;
      await request.response.close();
      return;
    }
    final assetPath = '$assetRoot/$safe';
    try {
      final data = await rootBundle.load(assetPath);
      final bytes = data.buffer.asUint8List();
      request.response.headers.set(
        HttpHeaders.cacheControlHeader,
        'no-store, max-age=0',
      );
      request.response.headers.contentType = _contentTypeFor(safe, asHtmlRoot);
      request.response.add(bytes);
      await request.response.close();
    } catch (e) {
      request.response.statusCode = HttpStatus.notFound;
      await request.response.close();
    }
  }

  String? _sanitizeAssetPath(String relative) {
    var path = relative.replaceAll('\\', '/');
    if (path.startsWith('/')) path = path.substring(1);
    if (path.contains('..') || path.contains(':')) return null;
    if (path.isEmpty) return 'index.html';
    const allowed = {
      'index.html',
      'css/display.css',
      'js/display.js',
    };
    if (!allowed.contains(path)) return null;
    return path;
  }

  ContentType _contentTypeFor(String path, bool asHtmlRoot) {
    if (asHtmlRoot || path.endsWith('.html')) {
      return ContentType('text', 'html', charset: 'utf-8');
    }
    if (path.endsWith('.css')) {
      return ContentType('text', 'css', charset: 'utf-8');
    }
    if (path.endsWith('.js')) {
      return ContentType('application', 'javascript', charset: 'utf-8');
    }
    return ContentType.binary;
  }

  Future<void> _handleWebSocket(HttpRequest request) async {
    final token = request.uri.queryParameters['token'];
    if (!_tokenValid(token)) {
      request.response.statusCode = HttpStatus.unauthorized;
      await request.response.close();
      AppLogger.warning('PAYMENT_DISPLAY_ERROR ws_rejected_token');
      return;
    }

    final socket = await WebSocketTransformer.upgrade(request);
    final client = _DisplayClient(socket: socket, authenticated: true);
    _clients.add(client);
    onClientCountChanged?.call(connectedClientCount);
    AppLogger.info('PAYMENT_DISPLAY_CONNECTED clients=$connectedClientCount');
    AppLogger.info('PAYMENT_DISPLAY_PAIRED');

    unawaited(
      client.send(
        const PaymentDisplayMessage(type: 'DISPLAY_CONNECTED', payload: {}),
      ),
    );

    final bill = _activeBill;
    if (bill != null && !bill.isExpired) {
      unawaited(
        client.send(
          PaymentDisplayMessage(
            type: 'BILL_UPDATED',
            payload: bill.toWireJson(),
          ),
        ),
      );
    } else {
      if (bill != null && bill.isExpired) {
        _activeBill = null;
      }
      unawaited(
        client.send(
          const PaymentDisplayMessage(
            type: 'BILL_CLEARED',
            payload: {'reason': 'none'},
          ),
        ),
      );
    }

    socket.listen(
      (dynamic data) {
        _onClientMessage(client, data);
      },
      onDone: () {
        _clients.remove(client);
        onClientCountChanged?.call(connectedClientCount);
        AppLogger.info(
          'PAYMENT_DISPLAY_DISCONNECTED clients=$connectedClientCount',
        );
      },
      onError: (Object e, StackTrace st) {
        AppLogger.error('PAYMENT_DISPLAY_ERROR ws_client', e, st);
        _clients.remove(client);
        onClientCountChanged?.call(connectedClientCount);
      },
      cancelOnError: true,
    );
  }

  void _onClientMessage(_DisplayClient client, dynamic data) {
    try {
      if (data is! String) return;
      final decoded = jsonDecode(data);
      if (decoded is! Map) return;
      final type = '${decoded['type'] ?? ''}';
      if (type == 'DISPLAY_PING') {
        unawaited(
          client.send(
            PaymentDisplayMessage(
              type: 'DISPLAY_PONG',
              payload: {
                'serverTime': DateTime.now().toIso8601String(),
              },
            ),
          ),
        );
      }
    } catch (e, st) {
      AppLogger.error('PAYMENT_DISPLAY_ERROR ws_message', e, st);
    }
  }

  Future<void> _broadcast(PaymentDisplayMessage message) async {
    final encoded = message.encode();
    for (final client in List<_DisplayClient>.from(_clients)) {
      if (!client.authenticated) continue;
      try {
        client.socket.add(encoded);
      } catch (_) {
        _clients.remove(client);
      }
    }
    onClientCountChanged?.call(connectedClientCount);
  }
}

class _DisplayClient {
  _DisplayClient({required this.socket, required this.authenticated});

  final WebSocket socket;
  final bool authenticated;

  Future<void> send(PaymentDisplayMessage message) async {
    try {
      socket.add(message.encode());
    } catch (_) {}
  }

  Future<void> close() async {
    try {
      await socket.close();
    } catch (_) {}
  }
}
