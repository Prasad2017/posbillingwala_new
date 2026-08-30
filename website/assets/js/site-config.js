/**
 * Website → Admin API base URL.
 * Admin runs on subdomain: https://admin.posbillingwala.com (no /adminpanel/ folder).
 * Override before this script: window.PBW_WEBSITE_API = 'https://admin.posbillingwala.com/api/website';
 */
(function () {
  var PROD_API = "https://admin.posbillingwala.com/api/website";
  var host = (window.location.hostname || "").toLowerCase();
  var isProd = host === "posbillingwala.com" || host === "www.posbillingwala.com";

  if (window.PBW_WEBSITE_API && String(window.PBW_WEBSITE_API).indexOf("/adminpanel") !== -1) {
    window.PBW_WEBSITE_API = PROD_API;
  }

  if (window.PBW_WEBSITE_API) {
    window.PBW_WEBSITE_API_CANDIDATES = [String(window.PBW_WEBSITE_API).replace(/\/$/, "")];
    return;
  }

  window.PBW_WEBSITE_API = PROD_API;
  window.PBW_WEBSITE_API_CANDIDATES = [PROD_API];
})();
