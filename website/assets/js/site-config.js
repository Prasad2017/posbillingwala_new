/**
 * Website → Admin API base URL.
 * Admin runs on subdomain: https://admin.posbillingwala.com (no /adminpanel/ folder).
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

  window.PBW_WEBSITE_API = PROD_API;
  window.PBW_WEBSITE_API_CANDIDATES = [PROD_API];
})();
