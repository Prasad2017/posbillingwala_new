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

  if (toggle && panel) {
    toggle.addEventListener("click", function () {
      var open = panel.classList.toggle("is-open");
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
    });

    panel.querySelectorAll("a").forEach(function (link) {
      link.addEventListener("click", function () {
        panel.classList.remove("is-open");
        toggle.setAttribute("aria-expanded", "false");
      });
    });
  }

  var links = document.querySelectorAll('.nav-desktop a[href^="#"]');
  var sections = Array.prototype.map.call(links, function (a) {
    return document.querySelector(a.getAttribute("href"));
  }).filter(Boolean);

  function setActive() {
    var y = window.scrollY + 100;
    var current = sections[0];
    sections.forEach(function (section) {
      if (section.offsetTop <= y) current = section;
    });
    links.forEach(function (a) {
      a.classList.toggle("is-active", current && a.getAttribute("href") === "#" + current.id);
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
})();
