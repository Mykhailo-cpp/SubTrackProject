/* SubTrack — shared application layout.
 *
 * This is the base structure every authenticated page reuses. A page only has
 * to provide its own content; this module guards the session and renders the
 * navbar and sidebar around it so the chrome never gets duplicated across
 * pages or future tickets.
 *
 * A page opts in with:
 *   <body data-page="subscriptions" data-title="Subscriptions"> ... </body>
 * where data-page matches a nav key below and data-title is shown in the navbar.
 */
(function () {
  // Stop immediately if there is no valid session.
  if (!Auth.requireAuth()) return;

  var NAV = [
    {
      key: "dashboard",
      label: "Dashboard",
      href: "dashboard.html",
      icon: '<path d="M3 13h8V3H3v10Zm0 8h8v-6H3v6Zm10 0h8V11h-8v10Zm0-18v6h8V3h-8Z"/>',
    },
    {
      key: "subscriptions",
      label: "Subscriptions",
      href: "subscriptions.html",
      icon: '<path d="M3 5h18v4H3V5Zm0 6h18v8H3v-8Zm3 3h6v2H6v-2Z"/>',
    },
    {
      key: "categories",
      label: "Categories",
      href: "categories.html",
      icon: '<path d="M3 3h8v8H3V3Zm10 0h8v8h-8V3ZM3 13h8v8H3v-8Zm10 0h8v8h-8v-8Z"/>',
    },
  ];

  var BRAND_MARK =
    '<span class="brand-mark"><svg viewBox="0 0 24 24" fill="none" stroke="#fff" ' +
    'stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">' +
    '<path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 4v5h-5"/></svg></span>';

  function initials(name) {
    if (!name) return "?";
    return name.trim().slice(0, 2).toUpperCase();
  }

  function navLinks(activeKey) {
    return NAV.map(function (item) {
      var cls = item.key === activeKey ? "active" : "";
      return (
        '<a href="' + item.href + '" class="' + cls + '"' +
        (cls ? ' aria-current="page"' : "") + ">" +
        '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">' +
        item.icon + "</svg>" +
        "<span>" + item.label + "</span></a>"
      );
    }).join("");
  }

  function buildSidebar(activeKey, username) {
    return (
      '<div class="offcanvas-md offcanvas-start app-sidebar" tabindex="-1" id="appSidebar" ' +
      'aria-label="Main navigation">' +
        '<div class="sidebar-brand">' + BRAND_MARK +
          '<span class="brand-name">Sub<span>Track</span></span>' +
        "</div>" +

        '<div class="spend-chip" id="spendChip">' +
          '<div class="label">This month</div>' +
          '<div class="amount is-loading" id="spendChipAmount">—</div>' +
        "</div>" +

        '<nav class="sidebar-nav" aria-label="Pages">' +
          '<div class="nav-label">Menu</div>' +
          navLinks(activeKey) +
        "</nav>" +

        '<div class="sidebar-foot">' +
          '<div class="who">Signed in as <strong>' + username + "</strong></div>" +
          '<button type="button" class="btn btn-sm btn-outline-light w-100" data-logout>' +
            "Log out" +
          "</button>" +
        "</div>" +
      "</div>"
    );
  }

  function buildTopbar(title, username) {
    return (
      '<header class="app-topbar">' +
        '<button class="sidebar-toggle" type="button" data-bs-toggle="offcanvas" ' +
          'data-bs-target="#appSidebar" aria-controls="appSidebar" aria-label="Open navigation">' +
          '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" ' +
            'stroke-width="2" stroke-linecap="round"><path d="M3 6h18M3 12h18M3 18h18"/></svg>' +
        "</button>" +

        '<a class="topbar-brand" href="dashboard.html">' +
          "Sub<span>Track</span></a>" +
        '<h1 class="topbar-title">' + title + "</h1>" +

        '<div class="topbar-user dropdown ms-auto">' +
          '<button class="btn btn-light btn-sm dropdown-toggle" type="button" ' +
            'data-bs-toggle="dropdown" aria-expanded="false">' +
            '<span class="avatar">' + initials(username) + "</span>" +
            '<span class="d-none d-sm-inline">' + (username || "Account") + "</span>" +
          "</button>" +
          '<ul class="dropdown-menu dropdown-menu-end">' +
            '<li><button class="dropdown-item" type="button" data-logout>Log out</button></li>' +
          "</ul>" +
        "</div>" +
      "</header>"
    );
  }

  function mount() {
    var body = document.body;
    var activeKey = body.getAttribute("data-page") || "";
    var title = body.getAttribute("data-title") || "SubTrack";
    var username = Auth.getUsername();

    // The page author puts the page body inside #page-content; we wrap it in
    // the shell so every page gets the same navbar and sidebar.
    var content = document.getElementById("page-content");
    var contentHTML = content ? content.innerHTML : "";

    body.innerHTML =
      '<div class="app-shell">' +
        buildSidebar(activeKey, username) +
        '<div class="app-main">' +
          buildTopbar(title, username) +
          '<main class="app-content" id="page-content">' + contentHTML + "</main>" +
        "</div>" +
      "</div>";

    // Wire every logout control on the page.
    Array.prototype.forEach.call(
      document.querySelectorAll("[data-logout]"),
      function (btn) {
        btn.addEventListener("click", function () {
          Auth.logout();
        });
      }
    );

    loadMonthlySpend();
    document.dispatchEvent(new CustomEvent("layout:ready"));
  }

  /* Best-effort: show the user's normalised monthly total in the sidebar chip.
   * Ties every screen back to the app's single job. Fails quietly if the API
   * is unavailable so the shell still renders. */
  async function loadMonthlySpend() {
    var el = document.getElementById("spendChipAmount");
    if (!el) return;
    try {
      var summary = await Api.get("/subscriptions/summary");
      var total = 0;
      (summary.categories || []).forEach(function (cat) {
        (cat.subscriptions || []).forEach(function (sub) {
          total += Number(sub.monthlyPrice) || 0;
        });
      });
      el.classList.remove("is-loading");
      el.textContent = "€" + total.toFixed(2);
    } catch (e) {
      el.classList.remove("is-loading");
      el.textContent = "—";
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mount);
  } else {
    mount();
  }
})();
