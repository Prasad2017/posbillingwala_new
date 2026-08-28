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
})();
