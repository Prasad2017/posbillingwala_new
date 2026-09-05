<?php
/**
 * Public customer Mess Token page.
 * QR opens: https://posbillingwala.com/androidApp/mess_q.php?t={public_token}
 */
include_once('config.php');
require_once __DIR__ . '/mess_common_helpers.php';

mysqli_query($con, 'set names utf8mb4');
mess_common_ensure_schema($con);

$t = isset($_GET['t']) ? trim((string) $_GET['t']) : '';
$wantsJson = isset($_GET['format']) && $_GET['format'] === 'json';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$ip = mess_client_ip();
if (!mess_rate_limit_allow($con, 'page:' . $ip, 60, 60)) {
    if ($wantsJson) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(array('success' => false, 'message' => 'Please try again in a moment.'));
    } else {
        header('Content-Type: text/html; charset=utf-8');
        echo '<!DOCTYPE html><html><body style="font-family:sans-serif;text-align:center;padding:40px"><p>Please try again in a moment.</p></body></html>';
    }
    mysqli_close($con);
    exit;
}

$qr = ($t !== '') ? mess_get_qr_by_public_token($con, $t) : null;
$pageState = 'ok';
$messLabel = 'Mess Token';
$errorMessage = '';

if ($qr === null) {
    $pageState = 'invalid';
    $errorMessage = 'This QR code is not valid.';
    mess_audit($con, null, 'qr_scan_invalid', $t, null, null, null);
} elseif (strtoupper($qr['status']) !== 'ACTIVE') {
    $pageState = 'inactive';
    $errorMessage = 'This QR code is currently inactive.';
    mess_audit($con, (int) $qr['userId'], 'qr_scan_inactive', $t, null, null, null);
} else {
    if (!empty($qr['mess_label'])) {
        $messLabel = $qr['mess_label'];
    }
    mess_audit($con, (int) $qr['userId'], 'qr_scanned', $t, null, null, null);
}

if ($wantsJson) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array(
        'success' => $pageState === 'ok',
        'state' => $pageState,
        'message' => $errorMessage,
        'messLabel' => $messLabel,
    ));
    mysqli_close($con);
    exit;
}

header('Content-Type: text/html; charset=utf-8');
$tEsc = htmlspecialchars($t, ENT_QUOTES, 'UTF-8');
$messEsc = htmlspecialchars($messLabel, ENT_QUOTES, 'UTF-8');
$errEsc = htmlspecialchars($errorMessage, ENT_QUOTES, 'UTF-8');
?><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0f3d2e">
<title>Billingwala — Mess Token</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;background:linear-gradient(165deg,#0f3d2e 0%,#1a5c45 45%,#e8f2ed 45%);min-height:100vh;color:#122}
.wrap{max-width:420px;margin:0 auto;padding:28px 18px 40px}
.brand{text-align:center;color:#fff;margin-bottom:22px}
.brand h1{font-size:1.35rem;font-weight:700;letter-spacing:.04em}
.brand p{opacity:.9;margin-top:4px;font-size:.95rem}
.card{background:#fff;border-radius:16px;padding:22px 18px;box-shadow:0 10px 30px rgba(0,0,0,.12)}
label{display:block;font-size:.85rem;font-weight:600;color:#345;margin-bottom:8px}
input{width:100%;font-size:1.15rem;padding:14px 12px;border:1.5px solid #c9d6cf;border-radius:10px;outline:none}
input:focus{border-color:#1a5c45}
button{width:100%;margin-top:14px;padding:14px;border:0;border-radius:10px;background:#1a5c45;color:#fff;font-size:1.05rem;font-weight:700;cursor:pointer}
button:disabled{opacity:.6}
.msg{margin-top:14px;text-align:center;font-size:.95rem;color:#555;min-height:1.2em}
.err{color:#a33}
.ok{color:#1a5c45}
.result{text-align:center;padding:8px 0}
.result .check{font-size:2.2rem;color:#1a5c45;margin-bottom:8px}
.result .token{font-size:2rem;font-weight:800;letter-spacing:.04em;margin:10px 0}
.result .meta{color:#456;line-height:1.5;font-size:.95rem}
.hidden{display:none}
</style>
</head>
<body>
<div class="wrap">
  <div class="brand">
    <h1>BILLINGWALA</h1>
    <p><?php echo $messEsc; ?></p>
  </div>
  <div class="card">
<?php if ($pageState !== 'ok'): ?>
    <div class="result">
      <p class="err"><?php echo $errEsc; ?></p>
    </div>
<?php else: ?>
    <div id="formBox">
      <label for="reg">Mobile Number</label>
      <input id="reg" type="tel" inputmode="numeric" autocomplete="tel" placeholder="e.g. 9325987443" maxlength="15">
      <button id="btn" type="button">GET TOKEN</button>
      <p id="msg" class="msg"></p>
    </div>
    <div id="resultBox" class="result hidden"></div>
<?php endif; ?>
  </div>
</div>
<?php if ($pageState === 'ok'): ?>
<script>
(function(){
  var t = <?php echo json_encode($t); ?>;
  var btn = document.getElementById('btn');
  var reg = document.getElementById('reg');
  var msg = document.getElementById('msg');
  var formBox = document.getElementById('formBox');
  var resultBox = document.getElementById('resultBox');
  var busy = false;

  function showResult(data){
    formBox.classList.add('hidden');
    resultBox.classList.remove('hidden');
    var already = data.alreadyGenerated;
    var html = '<div class="check">✓</div>';
    html += '<div>' + (already ? 'TOKEN ALREADY GENERATED' : 'TOKEN GENERATED') + '</div>';
    html += '<div class="token">' + (data.tokenNumber || '') + '</div>';
    html += '<div class="meta">' + (data.mealSession || '') + '<br>Mobile: ' + (data.registrationNo || '') + '</div>';
    if (already) {
      html += '<p class="msg ok" style="margin-top:12px">You already have today\'s token.</p>';
    } else {
      html += '<p class="msg ok" style="margin-top:12px">Token sent to counter.<br>Please collect your token from the counter.<br>Thank You!</p>';
    }
    resultBox.innerHTML = html;
  }

  btn.addEventListener('click', function(){
    if (busy) return;
    var v = (reg.value || '').trim();
    if (!v) {
      msg.className = 'msg err';
      msg.textContent = 'Please enter your mobile number.';
      return;
    }
    busy = true;
    btn.disabled = true;
    msg.className = 'msg';
    msg.textContent = 'Generating…';

    var body = new FormData();
    body.append('t', t);
    body.append('registrationNo', v);

    fetch('mess_q_token.php', { method: 'POST', body: body, credentials: 'omit' })
      .then(function(r){ return r.json(); })
      .then(function(data){
        if (data && data.success) {
          showResult(data);
        } else {
          msg.className = 'msg err';
          msg.textContent = (data && data.message) ? data.message : 'Unable to generate your token right now. Please try again.';
          busy = false;
          btn.disabled = false;
        }
      })
      .catch(function(){
        msg.className = 'msg err';
        msg.textContent = 'Unable to generate your token right now. Please try again.';
        busy = false;
        btn.disabled = false;
      });
  });

  reg.addEventListener('keydown', function(e){
    if (e.key === 'Enter') btn.click();
  });
})();
</script>
<?php endif; ?>
</body>
</html>
<?php
mysqli_close($con);
