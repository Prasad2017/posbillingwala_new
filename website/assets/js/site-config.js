/**
 * Website → Admin API base URL.
 *
 * Production prefers same-origin `/api/website` (proxied by website/api/website-proxy.php)
 * so the browser does not need a trusted cert on admin.posbillingwala.com.
 * Direct admin URL remains as fallback once SSL is fixed.
 *
 * Override before this script: window.PBW_WEBSITE_API = 'https://admin.posbillingwala.com/api/website';
 */
(function () {
  var PROD_API = "https://admin.posbillingwala.com/api/website";
  var LOCAL_API = "http://127.0.0.1:8000/api/website";
  var host = (window.location.hostname || "").toLowerCase();
  var isLocal =
    host === "localhost" ||
    host === "127.0.0.1" ||
    host === "" ||
    host.endsWith(".local");

  window.PBW_IS_LOCAL = isLocal;

  if (window.PBW_WEBSITE_API && String(window.PBW_WEBSITE_API).indexOf("/adminpanel") !== -1) {
    window.PBW_WEBSITE_API = PROD_API;
  }

  if (window.PBW_WEBSITE_API) {
    window.PBW_WEBSITE_API_CANDIDATES = [String(window.PBW_WEBSITE_API).replace(/\/$/, "")];
    return;
  }

  if (isLocal) {
    window.PBW_WEBSITE_API = LOCAL_API;
    window.PBW_WEBSITE_API_CANDIDATES = [
      LOCAL_API,
      "http://localhost:8000/api/website",
      PROD_API,
    ];
    return;
  }

  var sameOriginApi = "";
  try {
    if (window.location && window.location.origin && window.location.protocol.indexOf("http") === 0) {
      sameOriginApi = String(window.location.origin).replace(/\/$/, "") + "/api/website";
    }
  } catch (e) {}

  window.PBW_WEBSITE_API = sameOriginApi || PROD_API;
  window.PBW_WEBSITE_API_CANDIDATES = sameOriginApi
    ? [sameOriginApi, PROD_API]
    : [PROD_API];
})();
