(function () {
  'use strict';

  var token = new URLSearchParams(window.location.search).get('token') || '';
  var app = document.getElementById('app');
  var waitingView = document.getElementById('waitingView');
  var billView = document.getElementById('billView');
  var shopNameEl = document.getElementById('shopName');
  var billNumberEl = document.getElementById('billNumber');
  var amountEl = document.getElementById('amount');
  var qrSvgEl = document.getElementById('qrSvg');
  var expiresEl = document.getElementById('expiresIn');
  var connStatusEl = document.getElementById('connStatus');

  var socket = null;
  var expiresAtMs = null;
  var countdownTimer = null;
  var reconnectAttempt = 0;
  var reconnectTimer = null;
  var pingTimer = null;

  function formatMoney(amount) {
    var n = Number(amount);
    if (!isFinite(n)) n = 0;
    return '₹' + n.toFixed(2);
  }

  function formatRemaining(ms) {
    var total = Math.max(0, Math.ceil(ms / 1000));
    var m = Math.floor(total / 60);
    var s = total % 60;
    return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
  }

  function showWaiting(message) {
    expiresAtMs = null;
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
    waitingView.classList.remove('hidden');
    billView.classList.add('hidden');
    app.classList.add('waiting');
    app.classList.remove('billing');
    if (message) connStatusEl.textContent = message;
  }

  function tickCountdown() {
    if (expiresAtMs == null) return;
    var left = expiresAtMs - Date.now();
    if (left <= 0) {
      showWaiting('Waiting for next bill');
      connStatusEl.textContent = 'Connected';
      return;
    }
    expiresEl.textContent = 'Expires in ' + formatRemaining(left);
  }

  function showBill(payload) {
    if (!payload || !payload.qrSvg || !payload.expiresAt) {
      showWaiting('Waiting for next bill');
      return;
    }
    var exp = Date.parse(payload.expiresAt);
    if (!isFinite(exp) || exp <= Date.now()) {
      showWaiting('Waiting for next bill');
      connStatusEl.textContent = 'Connected';
      return;
    }

    expiresAtMs = exp;
    shopNameEl.textContent = payload.shopName || payload.payeeName || 'Store';
    billNumberEl.textContent = 'Bill #' + (payload.billNumber || '');
    amountEl.textContent = formatMoney(payload.amount);
    qrSvgEl.innerHTML = payload.qrSvg;
    waitingView.classList.add('hidden');
    billView.classList.remove('hidden');
    app.classList.remove('waiting');
    app.classList.add('billing');
    tickCountdown();
    if (countdownTimer) clearInterval(countdownTimer);
    countdownTimer = setInterval(tickCountdown, 250);
  }

  function handleMessage(raw) {
    var msg;
    try {
      msg = JSON.parse(raw);
    } catch (e) {
      return;
    }
    var type = msg && msg.type;
    var payload = (msg && msg.payload) || {};
    if (type === 'BILL_UPDATED') {
      showBill(payload);
      return;
    }
    if (type === 'BILL_CLEARED') {
      showWaiting('Waiting for next bill');
      connStatusEl.textContent = 'Connected';
      return;
    }
    if (type === 'DISPLAY_CONNECTED') {
      connStatusEl.textContent = 'Connected';
      return;
    }
    if (type === 'DISPLAY_PONG') {
      return;
    }
    if (type === 'ERROR') {
      connStatusEl.textContent = 'Connection issue';
    }
  }

  function wsUrl() {
    var proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    return proto + '//' + location.host + '/display/ws?token=' + encodeURIComponent(token);
  }

  function scheduleReconnect() {
    if (reconnectTimer) return;
    var delay = Math.min(15000, 500 * Math.pow(2, reconnectAttempt));
    reconnectAttempt += 1;
    connStatusEl.textContent = 'Reconnecting…';
    reconnectTimer = setTimeout(function () {
      reconnectTimer = null;
      connect();
    }, delay);
  }

  function startPing() {
    if (pingTimer) clearInterval(pingTimer);
    pingTimer = setInterval(function () {
      if (!socket || socket.readyState !== 1) return;
      try {
        socket.send(JSON.stringify({ type: 'DISPLAY_PING' }));
      } catch (e) {}
    }, 15000);
  }

  function connect() {
    if (!token) {
      showWaiting('Invalid pairing link');
      connStatusEl.textContent = 'Open Payment Display settings on POS and scan again';
      return;
    }
    try {
      if (socket) {
        try { socket.close(); } catch (e) {}
      }
      socket = new WebSocket(wsUrl());
      socket.onopen = function () {
        reconnectAttempt = 0;
        connStatusEl.textContent = 'Connected';
        startPing();
      };
      socket.onmessage = function (ev) {
        handleMessage(ev.data);
      };
      socket.onclose = function () {
        if (pingTimer) clearInterval(pingTimer);
        scheduleReconnect();
      };
      socket.onerror = function () {
        try { socket.close(); } catch (e) {}
      };
    } catch (e) {
      scheduleReconnect();
    }
  }

  document.addEventListener('visibilitychange', function () {
    if (!document.hidden) tickCountdown();
  });

  showWaiting('Connecting…');
  connect();
})();
