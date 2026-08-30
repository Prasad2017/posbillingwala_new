(function () {
  var PLAY_STORE =
    "https://play.google.com/store/apps/details?id=com.pos_billingwala";

  var NAV = [
    { page: "home", href: "index.html", label: "Home" },
    { page: "products", href: "products.html", label: "Products" },
    { page: "software", href: "software.html", label: "Software" },
    { page: "pricing", href: "pricing.html", label: "Pricing" },
    { page: "dealers", href: "dealers.html", label: "Dealers" },
    { page: "customers", href: "customers.html", label: "Customers" },
    { page: "support", href: "support.html", label: "Support" },
    { page: "download", href: "download.html", label: "Download" },
    { page: "contact", href: "contact.html", label: "Contact" },
  ];

  var currentPage = document.body.getAttribute("data-pbw-page") || "home";

  function navLinks(activePage) {
    return NAV.map(function (item) {
      var cls = item.page === activePage ? ' class="is-active"' : "";
      return "<li><a href=\"" + item.href + "\"" + cls + ">" + item.label + "</a></li>";
    }).join("");
  }

  function headerHtml(activePage) {
    return (
      '<header class="site-nav" id="siteNav">' +
      '<div class="container nav-inner">' +
      '<a class="brand" href="index.html">' +
      '<img class="brand-logo" src="assets/images/app_logo.png" alt="POS Billingwala">' +
      '<span class="brand-text">POS Billingwala</span></a>' +
      '<nav class="nav-desktop" aria-label="Primary">' +
      '<ul class="nav-links nav-links--compact">' +
      navLinks(activePage) +
      "</ul></nav>" +
      '<div class="nav-actions">' +
      '<a class="btn btn-primary btn-nav" href="download.html">Download App</a>' +
      '<button class="menu-toggle" id="menuToggle" type="button" aria-label="Open menu" aria-expanded="false">' +
      "<span></span><span></span><span></span></button></div></div>" +
      '<div class="nav-panel" id="navPanel"><div class="container">' +
      '<ul class="nav-links">' +
      navLinks(activePage) +
      "</ul>" +
      '<div class="nav-panel-cta">' +
      '<a class="btn btn-primary" href="download.html">Download App</a>' +
      '<a class="btn btn-ghost-dark" href="contact.html">Get Demo</a>' +
      "</div></div></div></header>"
    );
  }

  function footerHtml() {
    return (
      '<footer class="site-footer">' +
      '<div class="container footer-grid footer-grid--wide">' +
      '<div class="footer-brand-block">' +
      '<a class="brand brand-footer" href="index.html">' +
      '<img class="brand-logo" src="assets/images/app_logo.png" alt="">' +
      '<span class="brand-text">POS Billingwala</span></a>' +
      '<p id="footerTagline">Smart Billing. Trusted Support. Better Business.</p>' +
      '<div class="footer-legal" id="footerLegal"></div></div>' +
      '<div class="footer-col"><h4>Company</h4>' +
      '<a href="about.html">About Us</a>' +
      '<a href="company.html">Company Model</a>' +
      '<a href="dealers.html">Find Dealer</a>' +
      '<a href="contact.html">Contact</a>' +
      '<a href="privacy.html">Privacy Policy</a>' +
      '<a href="terms.html">Terms &amp; Conditions</a>' +
      '<a href="refund.html">Refund &amp; Renewal</a></div>' +
      '<div class="footer-col"><h4>Solutions</h4>' +
      '<a href="products.html">Products</a>' +
      '<a href="software.html">Software</a>' +
      '<a href="pricing.html">Pricing</a>' +
      '<a href="support.html">Customer Support</a></div>' +
      '<div class="footer-col"><h4>Download</h4>' +
      '<a id="footerPlayStore" href="' +
      PLAY_STORE +
      '" target="_blank" rel="noopener noreferrer">Google Play</a>' +
      '<a href="download.html">Download App</a>' +
      '<a href="contact.html">Book a Demo</a></div></div>' +
      '<div class="container footer-bottom">' +
      '<p>© <span id="footerYear"></span> <span id="footerCompany">POS Billingwala</span>. All rights reserved.</p></div></footer>'
    );
  }

  var headerMount = document.getElementById("pbw-header");
  var footerMount = document.getElementById("pbw-footer");

  if (headerMount) {
    headerMount.outerHTML = headerHtml(currentPage);
  }
  if (footerMount) {
    footerMount.outerHTML = footerHtml();
  }

  var yearEl = document.getElementById("footerYear");
  if (yearEl) yearEl.textContent = String(new Date().getFullYear());

  window.PBW_LAYOUT = {
    playStoreUrl: PLAY_STORE,
    setPlayStoreUrl: function (url) {
      if (!url) return;
      window.PBW_LAYOUT.playStoreUrl = url;
      var footerLink = document.getElementById("footerPlayStore");
      if (footerLink) footerLink.href = url;
    },
  };
})();
