(function () {
  if (document.body.getAttribute("data-pbw-page") !== "home") return;

  function formatCompact(n) {
    if (n >= 100000) return (n / 100000).toFixed(0) + "L";
    if (n >= 1000) return (n / 1000).toFixed(0) + "K";
    return String(n);
  }

  function animateCounter(el) {
    var target = parseInt(el.getAttribute("data-target"), 10);
    if (!isFinite(target)) return;

    var suffix = el.getAttribute("data-suffix") || "";
    var prefix = el.textContent.indexOf("₹") === 0 ? "₹" : "";
    var compact = el.getAttribute("data-format") === "compact";
    var duration = 1400;
    var startTime = null;

    function step(ts) {
      if (!startTime) startTime = ts;
      var progress = Math.min((ts - startTime) / duration, 1);
      var eased = 1 - Math.pow(1 - progress, 3);
      var value = Math.round(target * eased);

      if (compact && value >= 1000) {
        el.textContent = formatCompact(value) + suffix;
      } else if (prefix) {
        el.textContent = prefix + value.toLocaleString("en-IN") + suffix;
      } else {
        el.textContent = value.toLocaleString("en-IN") + suffix;
      }

      if (progress < 1) requestAnimationFrame(step);
    }

    requestAnimationFrame(step);
  }

  if ("IntersectionObserver" in window) {
    var counterIo = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          animateCounter(entry.target);
          counterIo.unobserve(entry.target);
        });
      },
      { threshold: 0.5 }
    );
    document.querySelectorAll(".counter[data-target]").forEach(function (el) {
      counterIo.observe(el);
    });
  } else {
    document.querySelectorAll(".counter[data-target]").forEach(animateCounter);
  }
})();
