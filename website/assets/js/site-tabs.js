(function () {
  function activateTab(root, tabId) {
    var tabs = root.querySelectorAll("[data-tab]");
    var panels = root.querySelectorAll("[data-tab-panel]");

    tabs.forEach(function (tab) {
      var active = tab.getAttribute("data-tab") === tabId;
      tab.classList.toggle("is-active", active);
      tab.setAttribute("aria-selected", active ? "true" : "false");
    });

    panels.forEach(function (panel) {
      var active = panel.getAttribute("data-tab-panel") === tabId;
      panel.classList.toggle("is-active", active);
      panel.hidden = !active;
    });
  }

  function initTabs(root) {
    if (!root || root.dataset.tabsBound) return;
    root.dataset.tabsBound = "1";

    var tabs = root.querySelectorAll("[data-tab]");
    if (!tabs.length) return;

    tabs.forEach(function (tab) {
      tab.setAttribute("role", "tab");
      tab.addEventListener("click", function () {
        activateTab(root, tab.getAttribute("data-tab"));
      });
    });

    root.querySelectorAll("[data-tab-panel]").forEach(function (panel) {
      panel.setAttribute("role", "tabpanel");
    });

    var initial =
      root.querySelector("[data-tab].is-active") ||
      tabs[0];
    if (initial) activateTab(root, initial.getAttribute("data-tab"));
  }

  function initFilterTabs(root) {
    if (!root || root.dataset.filterBound) return;
    root.dataset.filterBound = "1";

    var target = root.getAttribute("data-filter-target");
    var attr = root.getAttribute("data-filter-attr") || "data-category";
    var container = target ? document.querySelector(target) : null;
    if (!container) return;

    root.querySelectorAll("[data-filter]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var val = btn.getAttribute("data-filter") || "";
        root.querySelectorAll("[data-filter]").forEach(function (b) {
          b.classList.toggle("is-active", b === btn);
        });

        container.querySelectorAll(".client-card").forEach(function (card) {
          if (!val) {
            card.hidden = false;
            return;
          }
          var cat = (card.getAttribute(attr) || "").toLowerCase();
          card.hidden = cat.indexOf(val.toLowerCase()) === -1;
        });
      });
    });
  }

  document.querySelectorAll("[data-tabs]").forEach(initTabs);
  document.querySelectorAll("[data-filter-tabs]").forEach(initFilterTabs);

  window.PBW_TABS = {
    init: function () {
      document.querySelectorAll("[data-tabs]").forEach(initTabs);
      document.querySelectorAll("[data-filter-tabs]").forEach(initFilterTabs);
    },
  };
})();
