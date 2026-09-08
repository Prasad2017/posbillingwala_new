<?php
/**
 * Public customer Mess Token page.
 * QR opens: https://posbillingwala.com/androidApp/mess_q.php?t={public_token}
 */
include_once('config.php');
require_once __DIR__ . '/mess_common_helpers.php';
require_once __DIR__ . '/company_store_fields.php';

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
$shopName = '';
$shopAddress = '';
$shopPhone = '';
$logoSrc = '';

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

$userIdForShop = ($qr !== null && isset($qr['userId'])) ? (int) $qr['userId'] : 0;
if ($userIdForShop > 0) {
    $company = db_stmt_fetch_one(
        $con,
        'SELECT * FROM `companys` WHERE `licenseId` = ? LIMIT 1',
        'i',
        $userIdForShop
    );
    if (is_array($company)) {
        $fields = company_structured_fields($company);
        $shopName = company_first_non_empty($fields['shopName1'], $messLabel);
        $addrParts = array();
        foreach (array('addressLine1', 'addressLine2', 'addressLine3') as $addrKey) {
            if ($fields[$addrKey] !== '') {
                $addrParts[] = $fields[$addrKey];
            }
        }
        $shopAddress = implode(', ', $addrParts);
        $shopPhone = company_first_non_empty($fields['phoneNo1'], $fields['phoneNo2']);

        $rawLogo = isset($company['companyLogo']) ? trim((string) $company['companyLogo']) : '';
        if ($rawLogo !== '') {
            if (stripos($rawLogo, 'data:image/') === 0) {
                $logoSrc = $rawLogo;
            } else {
                $mime = 'image/jpeg';
                if (strpos($rawLogo, 'iVBOR') === 0) {
                    $mime = 'image/png';
                } elseif (strpos($rawLogo, 'R0lGOD') === 0) {
                    $mime = 'image/gif';
                } elseif (strpos($rawLogo, 'UklGR') === 0) {
                    $mime = 'image/webp';
                }
                $logoSrc = 'data:' . $mime . ';base64,' . $rawLogo;
            }
        }
    }
}
if ($shopName === '') {
    $shopName = $messLabel;
}

if ($wantsJson) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(array(
        'success' => $pageState === 'ok',
        'state' => $pageState,
        'message' => $errorMessage,
        'messLabel' => $messLabel,
        'shopName' => $shopName,
        'shopAddress' => $shopAddress,
        'shopPhone' => $shopPhone,
        'hasLogo' => ($logoSrc !== ''),
    ));
    mysqli_close($con);
    exit;
}

header('Content-Type: text/html; charset=utf-8');
$shopNameEsc = htmlspecialchars($shopName, ENT_QUOTES, 'UTF-8');
$shopAddressEsc = htmlspecialchars($shopAddress, ENT_QUOTES, 'UTF-8');
$shopPhoneEsc = htmlspecialchars($shopPhone, ENT_QUOTES, 'UTF-8');
$errEsc = htmlspecialchars($errorMessage, ENT_QUOTES, 'UTF-8');
$pageTitle = htmlspecialchars($shopName !== '' ? $shopName . ' — Mess Token' : 'Mess Token', ENT_QUOTES, 'UTF-8');
?><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0f3d2e">
<title><?php echo $pageTitle; ?></title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;background:linear-gradient(165deg,#0f3d2e 0%,#1a5c45 45%,#e8f2ed 45%);min-height:100vh;color:#122}
.wrap{max-width:420px;margin:0 auto;padding:28px 18px 24px;display:flex;flex-direction:column;min-height:100vh}
.brand{text-align:center;color:#fff;margin-bottom:22px}
.brand .logo{width:88px;height:88px;object-fit:contain;border-radius:14px;background:#fff;padding:6px;margin:0 auto 14px;display:block;box-shadow:0 4px 14px rgba(0,0,0,.18)}
.brand h1{font-size:1.35rem;font-weight:700;letter-spacing:.02em;line-height:1.25}
.brand .addr,.brand .phone{opacity:.92;margin-top:8px;font-size:.95rem;line-height:1.45}
.brand .phone{margin-top:4px;font-weight:600}
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
.powered{margin-top:auto;padding-top:28px;text-align:center;color:#345;font-size:.85rem;line-height:1.5}
.powered .by{opacity:.85}
.powered a{color:#0f3d2e;font-weight:600;text-decoration:none}
</style>
</head>
<body>
<div class="wrap">
  <div class="brand">
<?php if ($logoSrc !== ''): ?>
    <img class="logo" src="<?php echo htmlspecialchars($logoSrc, ENT_QUOTES, 'UTF-8'); ?>" alt="<?php echo $shopNameEsc; ?>">
<?php endif; ?>
    <h1><?php echo $shopNameEsc; ?></h1>
<?php if ($shopAddressEsc !== ''): ?>
    <p class="addr"><?php echo $shopAddressEsc; ?></p>
<?php endif; ?>
<?php if ($shopPhoneEsc !== ''): ?>
    <p class="phone"><?php echo $shopPhoneEsc; ?></p>
<?php endif; ?>
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
  <div class="powered">
    <div class="by">powered by POS Billingwala</div>
    <a href="https://www.posbillingwala.com" target="_blank" rel="noopener noreferrer">www.posbillingwala.com</a>
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
