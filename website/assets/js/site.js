(function () {
  var nav = document.getElementById("siteNav");
  var toggle = document.getElementById("menuToggle");
  var panel = document.getElementById("navPanel");

  function onScroll() {
    if (!nav) return;
    nav.classList.toggle("is-scrolled", window.scrollY > 12);
  }

  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  function setMenuOpen(open) {
    if (!panel || !toggle) return;
    panel.classList.toggle("is-open", open);
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
    document.body.classList.toggle("nav-open", open);
  }

  if (toggle && panel) {
    toggle.addEventListener("click", function () {
      setMenuOpen(!panel.classList.contains("is-open"));
    });

    panel.querySelectorAll("a").forEach(function (link) {
      link.addEventListener("click", function () {
        setMenuOpen(false);
      });
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") setMenuOpen(false);
    });
  }

  var links = document.querySelectorAll('a[href^="#"]');
  var sections = [];

  links.forEach(function (a) {
    var id = a.getAttribute("href");
    if (!id || id === "#") return;
    var section = document.querySelector(id);
    if (section) sections.push({ link: a, section: section, id: id.slice(1) });
  });

  function setActive() {
    var y = window.scrollY + 120;
    var current = sections.length ? sections[0].id : null;
    sections.forEach(function (item) {
      if (item.section.offsetTop <= y) current = item.id;
    });
    sections.forEach(function (item) {
      var active = item.id === current;
      item.link.classList.toggle("is-active", active);
    });
  }

  window.addEventListener("scroll", setActive, { passive: true });
  setActive();

  if ("IntersectionObserver" in window) {
    var io = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.14 }
    );
    document.querySelectorAll(".reveal").forEach(function (el) {
      io.observe(el);
    });
  } else {
    document.querySelectorAll(".reveal").forEach(function (el) {
      el.classList.add("is-visible");
    });
  }

  var GLYPH_MAP = {
    "▤": "receipt-text",
    "□": "box",
    "↗": "trending-up",
    "♙": "users",
    "▣": "smartphone",
    "₹": "indian-rupee",
    "⚡": "zap",
    "✓": "check",
    "◉": "wifi-off",
    "♧": "users",
    "⌁": "wifi-off",
    "⬇": "download",
    "☎": "phone",
    "✉": "mail",
    "⚙": "settings",
    "💬": "message-circle",
    "☁": "cloud-sync",
    "◎": "languages",
    "↻": "refresh",
    "🍽️": "utensils",
    "🛒": "shopping-bag",
    "🥦": "store",
    "👕": "shirt",
    "🍛": "utensils",
    "🏨": "building",
    "💰": "tag",
    "🧹": "sparkles",
    "🔗": "layers",
    "📐": "monitor",
    "🧰": "package",
    "🔌": "cpu",
    "🖨️": "printer",
    "🖥️": "monitor",
    "📱": "smartphone",
    "📲": "smartphone",
  };

  var CHIP_MAP = {
    "Offline Billing": "wifi-off",
    "Bluetooth Printing": "bluetooth",
    "GST Billing": "indian-rupee",
    "Sales Reports": "bar-chart",
    "Customer Management": "users",
    "Data Sync": "cloud-sync",
    Multilingual: "languages",
  };

  var TONES = ["purple", "blue", "green", "orange", "cyan", "pink"];

  function iconNameFor(el) {
    var explicit = el.getAttribute("data-icon") || el.getAttribute("data-pbw-icon");
    if (explicit) return explicit;
    var text = (el.textContent || "").trim();
    if (GLYPH_MAP[text]) return GLYPH_MAP[text];
    if (/^0[1-3]$/.test(text)) return null;
    return null;
  }

  function hydrateModernIcons() {
    if (!window.PBW_ICONS) return;

    document.querySelectorAll("[data-pbw-icon]").forEach(function (el) {
      var name = el.getAttribute("data-pbw-icon");
      var tone = el.getAttribute("data-pbw-tone") || "blue";
      var size = parseInt(el.getAttribute("data-pbw-size") || "22", 10);
      var sm = el.hasAttribute("data-pbw-sm");
      el.outerHTML = window.PBW_ICONS.iconBox(name, tone, {
        size: size,
        className: sm ? "icon-box--sm" : "",
      });
    });

    document
      .querySelectorAll(
        ".feature-icon, .bento-icon, .stat-icon, .page-hero-icon, .support-channel-icon, .hv2-pills i, [data-icon]:not(.btn-inline-icon):not(.nav-phone-icon)"
      )
      .forEach(function (el, i) {
        var name = iconNameFor(el);
        if (!name) return;
        var tone = el.getAttribute("data-pbw-tone") || TONES[i % TONES.length];
        var inPill = el.closest && el.closest(".hv2-pills");
        var size = el.classList.contains("page-hero-icon")
          ? 28
          : el.classList.contains("stat-icon")
          ? 20
          : inPill
          ? 18
          : el.classList.contains("support-channel-icon")
          ? 20
          : 22;
        el.className = (el.className + " icon-box icon-box--" + tone).trim();
        el.innerHTML = window.PBW_ICONS.icon(name, { size: size });
      });

    document.querySelectorAll(".btn-inline-icon[data-icon], .nav-phone-icon[data-icon]").forEach(function (el) {
      el.innerHTML = window.PBW_ICONS.icon(el.getAttribute("data-icon"), { size: 16 });
    });

    document.querySelectorAll(".chip").forEach(function (chip, i) {
      var raw = chip.textContent.trim();
      var matched = null;
      var label = raw;
      Object.keys(CHIP_MAP).forEach(function (key) {
        if (raw.indexOf(key) !== -1) {
          matched = CHIP_MAP[key];
          label = key;
        }
      });
      if (!matched) {
        var glyph = raw.charAt(0);
        if (GLYPH_MAP[glyph]) {
          matched = GLYPH_MAP[glyph];
          label = raw.slice(glyph.length).trim();
        }
      }
      if (!matched) return;
      var tone = TONES[i % TONES.length];
      chip.innerHTML =
        '<span class="chip-icon icon-box icon-box--' +
        tone +
        ' icon-box--sm">' +
        window.PBW_ICONS.icon(matched, { size: 14 }) +
        "</span><span>" +
        label +
        "</span>";
      chip.classList.add("chip--modern");
    });

    document.querySelectorAll(".btn").forEach(function (btn) {
      if (btn.querySelector(".pbw-icon") || btn.querySelector("[data-icon]")) return;
      var text = btn.textContent.trim();
      if (text.charAt(0) === "⬇") {
        btn.innerHTML =
          window.PBW_ICONS.icon("download", { size: 16, className: "btn-icon" }) +
          "<span>" +
          text.slice(1).trim() +
          "</span>";
        btn.classList.add("btn--with-icon");
      }
    });

    document.querySelectorAll(".check-list li").forEach(function (li) {
      li.classList.add("check-list--modern");
    });
  }

  hydrateModernIcons();
})();
