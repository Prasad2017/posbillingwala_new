(function () {
  var DEFAULT_API = "/adminpanel/api/website";
  var apiBase = (window.PBW_WEBSITE_API || DEFAULT_API).replace(/\/$/, "");

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
    return fetch(apiBase + path, { headers: { Accept: "application/json" } }).then(function (res) {
      if (!res.ok) throw new Error("HTTP " + res.status);
      return res.json();
    });
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

  function renderClients(clients) {
    var root = document.getElementById("clientsRoot");
    if (!root || !clients || !clients.length) return;

    root.innerHTML = clients
      .map(function (client) {
        var photoStyle = client.photo_url
          ? ' style="background-image:url(\'' + escapeHtml(client.photo_url) + '\')"'
          : "";
        var logoHtml = client.logo_url
          ? '<img class="client-card-logo" src="' + escapeHtml(client.logo_url) + '" alt="">'
          : "";
        var subtitle = client.subtitle
          ? '<p class="client-subtitle">' + escapeHtml(client.subtitle) + "</p>"
          : "";
        var cta = client.cta_url
          ? '<a class="text-link" href="' +
            escapeHtml(client.cta_url) +
            '" target="_blank" rel="noopener noreferrer">Learn more →</a>'
          : "";

        return (
          '<article class="client-card reveal">' +
          '<div class="client-card-photo"' +
          photoStyle +
          "></div>" +
          '<div class="client-card-body">' +
          logoHtml +
          "<h3>" +
          escapeHtml(client.business_name) +
          "</h3>" +
          subtitle +
          "<p>" +
          escapeHtml(client.description || "") +
          "</p>" +
          cta +
          "</div></article>"
        );
      })
      .join("");

    observeReveal(root);
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

  function renderPrivacy(page) {
    renderCmsPage(page, "privacyTitle", "privacyBody", "privacyUpdated", "Privacy Policy");
  }

  function renderAbout(page) {
    renderCmsPage(page, "aboutTitle", "aboutBody", "aboutUpdated", "About Us");
  }

  if (document.getElementById("clientsRoot")) {
    fetchJson("/clients")
      .then(function (data) {
        if (data.success) renderClients(data.clients);
      })
      .catch(function () {});

    fetchJson("/testimonials")
      .then(function (data) {
        if (data.success) renderTestimonials(data.testimonials);
      })
      .catch(function () {});
  }

  if (document.getElementById("privacyBody")) {
    fetchJson("/pages/privacy")
      .then(function (data) {
        if (data.success) renderPrivacy(data.page);
      })
      .catch(function () {
        var updatedEl = document.getElementById("privacyUpdated");
        if (updatedEl) updatedEl.textContent = "";
      });
  }

  if (document.getElementById("aboutBody")) {
    fetchJson("/pages/about")
      .then(function (data) {
        if (data.success) renderAbout(data.page);
      })
      .catch(function () {
        var updatedEl = document.getElementById("aboutUpdated");
        if (updatedEl) updatedEl.textContent = "";
      });
  }
})();
