(function () {
  var prodApi = "https://admin.posbillingwala.com/api/website";

  if (window.PBW_WEBSITE_API && String(window.PBW_WEBSITE_API).indexOf("/adminpanel") !== -1) {
    window.PBW_WEBSITE_API = prodApi;
  }

  if (
    !window.PBW_WEBSITE_API &&
    (!window.PBW_WEBSITE_API_CANDIDATES || !window.PBW_WEBSITE_API_CANDIDATES.length)
  ) {
    window.PBW_WEBSITE_API = prodApi;
  }

  var DEFAULT_CANDIDATES = [prodApi];
  var resolvedBase = "";
  var lastApiError = "";

  function apiCandidates() {
    if (window.PBW_WEBSITE_API_CANDIDATES && window.PBW_WEBSITE_API_CANDIDATES.length) {
      return window.PBW_WEBSITE_API_CANDIDATES;
    }
    if (window.PBW_WEBSITE_API) {
      return [String(window.PBW_WEBSITE_API).replace(/\/$/, "")];
    }
    return DEFAULT_CANDIDATES;
  }

  function escapeHtml(text) {
    if (!text) return "";
    var div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  function stars(rating) {
    var n = Math.max(0, Math.min(5, parseInt(rating, 10) || 0));
    var out = "";
    for (var i = 0; i < 5; i++) {
      out += i < n ? "★" : "☆";
    }
    return out;
  }

  function fetchJson(path) {
    var candidates = resolvedBase ? [resolvedBase] : apiCandidates();
    var index = 0;

    function tryCandidate() {
      if (index >= candidates.length) {
        lastApiError = "Could not reach admin API. Tried: " + candidates.join(", ");
        return Promise.reject(new Error(lastApiError));
      }

      var base = candidates[index];
      index += 1;

      return fetch(base + path, { headers: { Accept: "application/json" } })
        .then(function (res) {
          if (!res.ok) throw new Error(base + " HTTP " + res.status);
          return res.json();
        })
        .then(function (json) {
          if (!json || json.success !== true) throw new Error(base + " invalid response");
          resolvedBase = base;
          window.PBW_WEBSITE_API = base;
          return json;
        })
        .catch(function (err) {
          if (index >= candidates.length) throw err;
          return tryCandidate();
        });
    }

    return tryCandidate();
  }

  function showLoadError(rootId, label) {
    var root = document.getElementById(rootId);
    if (!root) return;
    root.innerHTML =
      '<p class="empty-note api-error">' +
      escapeHtml(label || "Content") +
      " could not load from admin. " +
      "Open Admin → Website Content once, deploy latest admin code, and run p23_website_catalog.sql if needed. " +
      (lastApiError ? "<br><small>" + escapeHtml(lastApiError) + "</small>" : "") +
      "</p>";
  }

  function observeReveal(container) {
    if (!container) return;
    var els = container.querySelectorAll(".reveal");
    if (!("IntersectionObserver" in window)) {
      els.forEach(function (el) {
        el.classList.add("is-visible");
      });
      return;
    }
    var io = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.1 }
    );
    els.forEach(function (el) {
      io.observe(el);
    });
  }

  function formatPrice(value) {
    var n = parseFloat(value);
    if (!isFinite(n) || n <= 0) return "Contact for price";
    return "₹" + n.toLocaleString("en-IN", { maximumFractionDigits: 0 });
  }

  function renderClients(clients, rootId) {
    rootId = rootId || "clientsRoot";
    var root = document.getElementById(rootId);
    if (!root || !clients || !clients.length) return;

    window.PBW_CLIENTS = clients;

    root.innerHTML = clients
      .map(function (client) {
        var photoStyle = client.photo_url
          ? ' style="background-image:url(\'' + escapeHtml(client.photo_url) + '\')"'
          : "";
        var logoHtml = client.logo_url
          ? '<img class="client-card-logo" src="' + escapeHtml(client.logo_url) + '" alt="">'
          : "";
        var metaParts = [];
        if (client.city) metaParts.push(escapeHtml(client.city));
        if (client.business_category) metaParts.push(escapeHtml(client.business_category));
        var meta = metaParts.length
          ? '<p class="client-meta">' + metaParts.join(" · ") + "</p>"
          : client.subtitle
          ? '<p class="client-subtitle">' + escapeHtml(client.subtitle) + "</p>"
          : "";
        var cta = client.cta_url
          ? '<a class="text-link" href="' +
            escapeHtml(client.cta_url) +
            '" target="_blank" rel="noopener noreferrer">Learn more →</a>'
          : "";
        var catAttr = client.business_category
          ? ' data-category="' + escapeHtml(client.business_category) + '"'
          : "";

        return (
          '<article class="client-card reveal"' +
          catAttr +
          ">" +
          '<div class="client-card-photo"' +
          photoStyle +
          "></div>" +
          '<div class="client-card-body">' +
          logoHtml +
          "<h3>" +
          escapeHtml(client.business_name) +
          "</h3>" +
          meta +
          "<p>" +
          escapeHtml(client.description || "") +
          "</p>" +
          cta +
          "</div></article>"
        );
      })
      .join("");

    observeReveal(root);
    initClientCategoryFilter();
  }

  function initClientCategoryFilter() {
    var select = document.getElementById("clientCategoryFilter");
    if (!select || select.dataset.bound) return;
    select.dataset.bound = "1";
    select.addEventListener("change", function () {
      var val = select.value.trim().toLowerCase();
      document.querySelectorAll("#clientsRoot .client-card").forEach(function (card) {
        if (!val) {
          card.hidden = false;
          return;
        }
        var cat = (card.getAttribute("data-category") || "").toLowerCase();
        card.hidden = cat.indexOf(val) === -1;
      });
    });
  }

  function renderTestimonials(items) {
    var root = document.getElementById("testimonialsRoot");
    var section = document.getElementById("testimonials");
    if (!root || !items || !items.length) return;

    if (section) section.hidden = false;

    root.innerHTML = items
      .map(function (item) {
        var photo = item.photo_url
          ? '<img class="testimonial-photo" src="' + escapeHtml(item.photo_url) + '" alt="">'
          : '<div class="testimonial-photo testimonial-photo--placeholder" aria-hidden="true"></div>';
        var business = item.business_name
          ? '<span class="testimonial-business">' + escapeHtml(item.business_name) + "</span>"
          : "";

        return (
          '<article class="testimonial-card reveal">' +
          photo +
          '<div class="testimonial-stars" aria-label="' +
          (item.rating || 5) +
          ' out of 5">' +
          stars(item.rating) +
          "</div>" +
          '<blockquote>"' +
          escapeHtml(item.quote) +
          '"</blockquote>' +
          '<p class="testimonial-author">' +
          escapeHtml(item.author_name) +
          business +
          "</p></article>"
        );
      })
      .join("");

    observeReveal(root);
  }

  function renderProducts(products) {
    var root = document.getElementById("productsRoot");
    if (!root) return;
    if (!products || !products.length) {
      root.innerHTML = '<p class="empty-note">Products will appear here once added in the admin panel.</p>';
      return;
    }

    var groups = { software: [], hardware: [], consumables: [], accessories: [] };
    products.forEach(function (p) {
      if (groups[p.category]) groups[p.category].push(p);
    });

    var labels = {
      software: "Software",
      hardware: "Hardware",
      consumables: "Consumables",
      accessories: "Accessories",
    };

    var html = "";
    Object.keys(labels).forEach(function (key) {
      if (!groups[key].length) return;
      html +=
        '<div class="catalog-group reveal"><h3 class="catalog-group-title">' +
        labels[key] +
        "</h3><div class=\"catalog-grid\">";
      html += groups[key]
        .map(function (p) {
          return (
            '<article class="catalog-card">' +
            '<div class="catalog-icon">' +
            escapeHtml(p.icon || "📦") +
            "</div>" +
            "<h4>" +
            escapeHtml(p.name) +
            "</h4>" +
            "<p>" +
            escapeHtml(p.description || "") +
            "</p>" +
            '<a class="text-link" href="contact.html">Contact dealer →</a></article>'
          );
        })
        .join("");
      html += "</div></div>";
    });

    root.innerHTML = html;
    observeReveal(root);
  }

  function renderPricing(plans, rootId) {
    rootId = rootId || "pricingRoot";
    var root = document.getElementById(rootId);
    if (!root) return;
    if (!plans || !plans.length) {
      root.innerHTML = '<p class="empty-note">Pricing plans will appear here once added in the admin panel.</p>';
      return;
    }

    var full = rootId === "pricingRoot";
    var rows = plans
      .map(function (plan) {
        return (
          "<tr>" +
          "<td>" +
          escapeHtml(plan.plan_type === "renewal" ? "Renewal" : "Subscription") +
          "</td>" +
          "<td>" +
          escapeHtml(plan.validity_label) +
          "</td>" +
          "<td><strong>" +
          formatPrice(plan.price) +
          "</strong></td>" +
          "<td>" +
          escapeHtml(plan.gst_note || "") +
          "</td>" +
          (full ? '<td class="muted">' + escapeHtml(plan.description || "") + "</td>" : "") +
          "</tr>"
        );
      })
      .join("");

    root.innerHTML =
      '<div class="table-wrap reveal"><table class="pricing-table">' +
      "<thead><tr><th>Plan</th><th>Validity</th><th>Price</th><th>GST</th>" +
      (full ? "<th>Notes</th>" : "") +
      "</tr></thead><tbody>" +
      rows +
      "</tbody></table></div>" +
      (full
        ? '<p class="pricing-note reveal">Prices may vary by dealer or region. Contact your <a href="dealers.html">local dealer</a> for exact quote.</p>'
        : "");
    observeReveal(root);
  }

  function renderDealers(dealers) {
    window.PBW_DEALERS = dealers || [];
    var root = document.getElementById("dealersRoot");
    if (!root) return;
    if (!window.PBW_DEALERS.length) {
      root.innerHTML = '<p class="empty-note">Dealer contacts will appear here once added in the admin panel.</p>';
      return;
    }
    paintDealers(window.PBW_DEALERS);
    initDealerSearch();
  }

  function paintDealers(dealers) {
    var root = document.getElementById("dealersRoot");
    if (!root) return;
    if (!dealers || !dealers.length) {
      root.innerHTML = '<p class="empty-note">No dealers match your search.</p>';
      return;
    }

    root.innerHTML = dealers
      .map(function (dealer) {
        var badge =
          dealer.dealer_type === "head_office"
            ? '<span class="dealer-badge">Head Office</span>'
            : '<span class="dealer-badge dealer-badge--auth">Authorized Dealer</span>';
        var person = dealer.contact_person
          ? "<p><strong>" +
            escapeHtml(dealer.contact_person) +
            "</strong>" +
            (dealer.role_title ? " · " + escapeHtml(dealer.role_title) : "") +
            "</p>"
          : "";
        var mobile = dealer.mobile
          ? '<a class="btn btn-sm btn-ghost-dark" href="tel:' +
            escapeHtml(dealer.mobile) +
            '">Call ' +
            escapeHtml(dealer.mobile) +
            "</a>"
          : "";
        var wa = dealer.whatsapp
          ? '<a class="btn btn-sm btn-primary" href="https://wa.me/91' +
            escapeHtml(String(dealer.whatsapp).replace(/\D/g, "")) +
            '" target="_blank" rel="noopener noreferrer">WhatsApp</a>'
          : "";
        var map = dealer.map_url
          ? '<a class="text-link" href="' +
            escapeHtml(dealer.map_url) +
            '" target="_blank" rel="noopener noreferrer">View on map →</a>'
          : "";

        return (
          '<article class="dealer-card reveal">' +
          '<div class="dealer-card-head"><div><h3>' +
          escapeHtml(dealer.area) +
          "</h3><p class=\"dealer-name\">" +
          escapeHtml(dealer.dealer_name) +
          "</p></div>" +
          badge +
          "</div>" +
          person +
          (dealer.address ? "<p class=\"dealer-address\">" + escapeHtml(dealer.address) + "</p>" : "") +
          '<div class="dealer-actions">' +
          mobile +
          wa +
          "</div>" +
          map +
          "</article>"
        );
      })
      .join("");

    observeReveal(root);
  }

  function initDealerSearch() {
    var input = document.getElementById("dealerSearch");
    if (!input || input.dataset.bound) return;
    input.dataset.bound = "1";
    input.addEventListener("input", function () {
      var q = input.value.trim().toLowerCase();
      var all = window.PBW_DEALERS || [];
      if (!q) {
        paintDealers(all);
        return;
      }
      paintDealers(
        all.filter(function (d) {
          var hay = [d.area, d.dealer_name, d.contact_person, d.address, d.mobile].join(" ").toLowerCase();
          return hay.indexOf(q) !== -1;
        })
      );
    });
  }

  function applySettings(settings) {
    if (!settings) return;

    if (settings.brand_tagline) {
      var taglineEl = document.getElementById("footerTagline");
      if (taglineEl) taglineEl.textContent = settings.brand_tagline;
      var heroTagline = document.getElementById("heroTagline");
      if (heroTagline) heroTagline.textContent = settings.brand_tagline;
    }

    if (settings.play_store_url && window.PBW_LAYOUT) {
      window.PBW_LAYOUT.setPlayStoreUrl(settings.play_store_url);
      document.querySelectorAll('a[data-play-store]').forEach(function (a) {
        a.href = settings.play_store_url;
      });
    }

    var legalParts = [];
    if (settings.legal_company_name) {
      legalParts.push("<strong>" + escapeHtml(settings.legal_company_name) + "</strong>");
      var companyEl = document.getElementById("footerCompany");
      if (companyEl) companyEl.textContent = settings.legal_company_name;
    }
    if (settings.gstin) legalParts.push("GSTIN: " + escapeHtml(settings.gstin));
    if (settings.office_address) legalParts.push(escapeHtml(settings.office_address));
    if (settings.support_phone) {
      legalParts.push('Support: <a href="tel:' + escapeHtml(settings.support_phone) + '">' + escapeHtml(settings.support_phone) + "</a>");
    }
    if (settings.business_hours) legalParts.push(escapeHtml(settings.business_hours));

    var legalEl = document.getElementById("footerLegal");
    if (legalEl && legalParts.length) {
      legalEl.innerHTML = legalParts.join("<br>");
    }

    var contactPhone = document.getElementById("contactPhone");
    if (contactPhone && settings.support_phone) {
      contactPhone.innerHTML = '<a href="tel:' + escapeHtml(settings.support_phone) + '">' + escapeHtml(settings.support_phone) + "</a>";
    }
    var contactEmail = document.getElementById("contactEmailDisplay");
    if (contactEmail && settings.support_email) {
      contactEmail.innerHTML = '<a href="mailto:' + escapeHtml(settings.support_email) + '">' + escapeHtml(settings.support_email) + "</a>";
    }
    var contactHours = document.getElementById("contactHours");
    if (contactHours && settings.business_hours) contactHours.textContent = settings.business_hours;

    var appVersion = document.getElementById("appVersion");
    if (appVersion && settings.app_latest_version) appVersion.textContent = "Latest version: " + settings.app_latest_version;

    var apkLink = document.getElementById("apkDownloadLink");
    if (apkLink && settings.apk_download_url) {
      apkLink.href = settings.apk_download_url;
      apkLink.hidden = false;
    }
  }

  function renderCmsPage(page, titleId, bodyId, updatedId, defaultTitle) {
    var titleEl = document.getElementById(titleId);
    var bodyEl = document.getElementById(bodyId);
    var updatedEl = document.getElementById(updatedId);
    if (!page || !bodyEl) return;

    var title = page.title || defaultTitle;
    if (titleEl) titleEl.textContent = title;
    document.title = title + " — POS Billingwala";
    bodyEl.innerHTML = page.body_html || "";

    if (updatedEl && page.updated_at) {
      try {
        updatedEl.textContent =
          "Last updated " +
          new Date(page.updated_at).toLocaleDateString(undefined, {
            year: "numeric",
            month: "long",
            day: "numeric",
          });
      } catch (e) {
        updatedEl.textContent = "";
      }
    }
  }

  fetchJson("/settings")
    .then(function (data) {
      if (data.success) applySettings(data.settings);
    })
    .catch(function () {});

  if (document.getElementById("clientsRoot")) {
    fetchJson("/clients")
      .then(function (data) {
        if (data.success) renderClients(data.clients);
      })
      .catch(function () {
        showLoadError("clientsRoot", "Customers");
      });

    fetchJson("/testimonials")
      .then(function (data) {
        if (data.success) renderTestimonials(data.testimonials);
      })
      .catch(function () {});
  }

  if (document.getElementById("productsRoot")) {
    fetchJson("/products")
      .then(function (data) {
        if (data.success) renderProducts(data.products);
      })
      .catch(function () {
        showLoadError("productsRoot", "Products");
      });
  }

  if (document.getElementById("pricingRoot") || document.getElementById("pricingHomeRoot")) {
    fetchJson("/pricing")
      .then(function (data) {
        if (!data.success) return;
        if (document.getElementById("pricingRoot")) renderPricing(data.plans, "pricingRoot");
        if (document.getElementById("pricingHomeRoot")) renderPricing(data.plans, "pricingHomeRoot");
      })
      .catch(function () {
        showLoadError("pricingRoot", "Pricing");
        showLoadError("pricingHomeRoot", "Pricing");
      });
  }

  if (document.getElementById("dealersRoot")) {
    fetchJson("/dealers")
      .then(function (data) {
        if (data.success) renderDealers(data.dealers);
      })
      .catch(function () {
        showLoadError("dealersRoot", "Dealers");
      });
  }

  var cmsPages = [
    { slug: "privacy", titleId: "privacyTitle", bodyId: "privacyBody", updatedId: "privacyUpdated", defaultTitle: "Privacy Policy" },
    { slug: "about", titleId: "aboutTitle", bodyId: "aboutBody", updatedId: "aboutUpdated", defaultTitle: "About Us" },
    { slug: "terms", titleId: "termsTitle", bodyId: "termsBody", updatedId: "termsUpdated", defaultTitle: "Terms & Conditions" },
    { slug: "refund-renewal", titleId: "refundTitle", bodyId: "refundBody", updatedId: "refundUpdated", defaultTitle: "Refund & Renewal Policy" },
    { slug: "support", titleId: "supportTitle", bodyId: "supportBody", updatedId: "supportUpdated", defaultTitle: "Customer Support" },
    { slug: "company", titleId: "companyTitle", bodyId: "companyBody", updatedId: "companyUpdated", defaultTitle: "Company Model" },
  ];

  cmsPages.forEach(function (cfg) {
    if (!document.getElementById(cfg.bodyId)) return;
    fetchJson("/pages/" + cfg.slug)
      .then(function (data) {
        if (data.success) renderCmsPage(data.page, cfg.titleId, cfg.bodyId, cfg.updatedId, cfg.defaultTitle);
      })
      .catch(function () {
        showLoadError(cfg.bodyId, cfg.defaultTitle);
      });
  });
})();
