/* SubTrack — subscriptions page logic (ticket #26).
 *
 * Renders the user's subscriptions in a table and drives the create / edit /
 * delete actions through a single Bootstrap modal. The shell (navbar, sidebar,
 * auth, API client) is already in place; this module only owns the page body,
 * which the layout exposes after it fires `layout:ready`.
 *
 * Data flow:
 *   - GET  /subscriptions          → list (HATEOAS collection, unwrapped)
 *   - GET  /categories             → populate the category <select> and names
 *   - POST /subscriptions          → create
 *   - PUT  /subscriptions/{id}      → update
 *   - DELETE /subscriptions/{id}    → delete
 * Every mutation re-fetches the list so the table always reflects the server.
 * The JWT is attached centrally by Api; nothing here touches the token.
 */
(function () {
  // Billing cycles mirror the server-side BillingCycle enum, with friendly
  // labels for the table and the form's <select>.
  var BILLING_CYCLES = [
    { value: "WEEKLY", label: "Weekly" },
    { value: "MONTHLY", label: "Monthly" },
    { value: "QUARTERLY", label: "Quarterly" },
    { value: "SEMI_ANNUAL", label: "Semi-annual" },
    { value: "ANNUAL", label: "Annual" },
  ];

  // Page state. `categories` is cached from the categories endpoint so the
  // table can show names and the form can offer a category picker.
  var subscriptions = [];
  var categories = [];
  var modal; // bootstrap.Modal instance
  var editingId = null; // null → creating, otherwise the id being edited

  // Sort + search state
  var sortKey = "name";   // "name" | "price" | "renewal"
  var sortDir = "asc";    // "asc" | "desc"
  var searchQuery = "";

  // DOM handles, resolved once the layout has mounted the page body.
  var el = {};

  function cycleLabel(value) {
    var match = BILLING_CYCLES.find(function (c) {
      return c.value === value;
    });
    return match ? match.label : value;
  }

  /* Escape user-supplied strings before injecting them into table HTML so a
   * subscription named `<img onerror=…>` can't run script. */
  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  /* Format a price with its currency. Money uses the ledger-style mono class
   * defined in app.css so columns line up. */
  function formatPrice(price, currency) {
    var amount = Number(price);
    var text = isNaN(amount) ? String(price) : amount.toFixed(2);
    return escapeHtml(currency) + " " + text;
  }

  /* Render an ISO date (yyyy-mm-dd) as a short, locale-aware label. Falls back
   * to the raw value if it can't be parsed. */
  function formatDate(iso) {
    if (!iso) return "—";
    var d = new Date(iso + "T00:00:00");
    if (isNaN(d.getTime())) return escapeHtml(iso);
    return d.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  // ---------------------------------------------------------------------------
  // Data loading
  // ---------------------------------------------------------------------------

  async function loadCategories() {
    try {
      var data = await Api.get("/categories");
      // The categories endpoint returns a plain array, but unwrapCollection is
      // a harmless pass-through for arrays and future-proofs against HATEOAS.
      categories = Api.unwrapCollection(data, "categoryResponseList");
    } catch (e) {
      categories = [];
    }
  }

  async function loadSubscriptions() {
    showState("loading");
    try {
      var data = await Api.get("/subscriptions");
      subscriptions = Api.unwrapCollection(data, "subscriptionResponseList");
      renderTable();
    } catch (e) {
      showState("error", e.message || "Couldn't load your subscriptions.");
    }
  }

  /* Reload everything the page shows. Called on first mount and after every
   * create / update / delete so the table mirrors the server. */
  async function refresh() {
    await loadCategories();
    await loadSubscriptions();
  }

  // ---------------------------------------------------------------------------
  // Sort + Search helpers
  // ---------------------------------------------------------------------------

  function getSortedFiltered() {
    var q = searchQuery.trim().toLowerCase();
    var list = q
      ? subscriptions.filter(function (s) {
          return s.name && s.name.toLowerCase().indexOf(q) !== -1;
        })
      : subscriptions.slice();

    list.sort(function (a, b) {
      var av, bv;
      if (sortKey === "price") {
        av = Number(a.price) || 0;
        bv = Number(b.price) || 0;
      } else if (sortKey === "renewal") {
        av = a.nextRenewalDate || "";
        bv = b.nextRenewalDate || "";
      } else {
        av = (a.name || "").toLowerCase();
        bv = (b.name || "").toLowerCase();
      }
      if (av < bv) return sortDir === "asc" ? -1 : 1;
      if (av > bv) return sortDir === "asc" ? 1 : -1;
      return 0;
    });
    return list;
  }

  function updateSortHeaders() {
    ["name", "price", "renewal"].forEach(function (key) {
      var th = document.getElementById("sortTh-" + key);
      if (!th) return;
      var icon = th.querySelector(".sort-icon");
      if (sortKey === key) {
        icon.textContent = sortDir === "asc" ? " ↑" : " ↓";
        th.classList.add("sort-active");
      } else {
        icon.textContent = " ↕";
        th.classList.remove("sort-active");
      }
    });
  }

  function onSortClick(key) {
    if (sortKey === key) {
      sortDir = sortDir === "asc" ? "desc" : "asc";
    } else {
      sortKey = key;
      sortDir = "asc";
    }
    updateSortHeaders();
    renderTable();
  }


  // Rendering


  /* Toggle the three mutually exclusive page states: loading, error, content.
   * The empty state is handled inside renderTable since it's part of content. */
  function showState(state, message) {
    el.loading.classList.toggle("d-none", state !== "loading");
    el.error.classList.toggle("d-none", state !== "error");
    el.content.classList.toggle("d-none", state !== "content");
    if (state === "error" && message) {
      el.error.textContent = message;
    }
  }

  function categoryName(categoryId, fallback) {
    if (fallback) return fallback;
    var match = categories.find(function (c) {
      return c.id === categoryId;
    });
    return match ? match.name : "—";
  }

  function renderTable() {
    showState("content");

    var list = getSortedFiltered();
    var total = subscriptions.length;
    var filtered = list.length;

    if (searchQuery.trim()) {
      el.count.textContent =
        filtered + " of " + total + (total === 1 ? " subscription" : " subscriptions");
    } else {
      el.count.textContent =
        total === 0 ? "" : total + (total === 1 ? " subscription" : " subscriptions");
    }

    if (!total) {
      el.empty.classList.remove("d-none");
      el.tableWrap.classList.add("d-none");
      return;
    }

    el.empty.classList.add("d-none");
    el.tableWrap.classList.remove("d-none");

    if (!filtered) {
      el.tbody.innerHTML =
        '<tr><td colspan="7" class="text-center text-muted py-4">No subscriptions match your search.</td></tr>';
      updateSortHeaders();
      return;
    }

    el.tbody.innerHTML = list
      .map(function (sub) {
        var inactive = sub.active === false;
        var reminderBadge = sub.renewalReminderEnabled
          ? '<span class="st-badge st-badge--reminder" title="Email reminder on">' +
            reminderIcon() +
            "Reminder</span>"
          : '<span class="st-badge st-badge--muted" title="No email reminder">Off</span>';

        var statusBadge = inactive
          ? '<span class="st-badge st-badge--muted">Inactive</span>'
          : '<span class="st-badge st-badge--active">Active</span>';

        return (
          '<tr class="' +
          (inactive ? "is-inactive" : "") +
          '">' +
          '<td><div class="st-name">' +
          escapeHtml(sub.name) +
          "</div>" +
          (sub.description
            ? '<div class="st-sub">' + escapeHtml(sub.description) + "</div>"
            : "") +
          "</td>" +
          '<td class="st-money">' +
          formatPrice(sub.price, sub.currency) +
          "</td>" +
          "<td>" +
          escapeHtml(cycleLabel(sub.billingCycle)) +
          "</td>" +
          '<td class="st-nowrap">' +
          formatDate(sub.nextRenewalDate) +
          "</td>" +
          "<td>" +
          escapeHtml(categoryName(sub.categoryId, sub.categoryName)) +
          "</td>" +
          '<td class="st-nowrap">' +
          statusBadge +
          " " +
          reminderBadge +
          "</td>" +
          '<td class="text-end st-nowrap">' +
          '<button type="button" class="btn btn-sm btn-outline-secondary me-1" data-edit="' +
          sub.id +
          '">Edit</button>' +
          '<button type="button" class="btn btn-sm btn-outline-danger" data-delete="' +
          sub.id +
          '">Delete</button>' +
          "</td>" +
          "</tr>"
        );
      })
      .join("");
    updateSortHeaders();
  }

  function reminderIcon() {
    return (
      '<svg viewBox="0 0 24 24" width="13" height="13" fill="none" ' +
      'stroke="currentColor" stroke-width="2" stroke-linecap="round" ' +
      'stroke-linejoin="round" aria-hidden="true">' +
      '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/>' +
      '<path d="M13.7 21a2 2 0 0 1-3.4 0"/></svg> '
    );
  }

  // Modal: create / edit


  /* Populate the category <select> from the cached categories. When none exist
   * yet we leave a disabled hint so the user knows to create one first. */
  function fillCategorySelect(selectedId) {
    if (!categories.length) {
      el.fCategory.innerHTML =
        '<option value="">No categories yet — create one first</option>';
      el.fCategory.disabled = true;
      return;
    }
    el.fCategory.disabled = false;
    el.fCategory.innerHTML = categories
      .map(function (c) {
        var sel = c.id === selectedId ? " selected" : "";
        return (
          '<option value="' +
          c.id +
          '"' +
          sel +
          ">" +
          escapeHtml(c.name) +
          "</option>"
        );
      })
      .join("");
  }

  function fillCycleSelect(selected) {
    el.fCycle.innerHTML = BILLING_CYCLES.map(function (c) {
      var sel = c.value === selected ? " selected" : "";
      return '<option value="' + c.value + '"' + sel + ">" + c.label + "</option>";
    }).join("");
  }

  function clearFormError() {
    el.formError.textContent = "";
    el.formError.classList.add("d-none");
  }

  function showFormError(message) {
    el.formError.textContent = message;
    el.formError.classList.remove("d-none");
  }

  /* Open the modal in create mode: blank fields, sensible defaults (active on,
   * reminder off per GDPR opt-in, renewal date today). */
  function openCreate() {
    editingId = null;
    clearFormError();
    el.modalTitle.textContent = "Add subscription";
    el.saveBtn.textContent = "Add subscription";

    el.fName.value = "";
    el.fDescription.value = "";
    el.fPrice.value = "";
    el.fCurrency.value = "EUR";
    fillCycleSelect("MONTHLY");
    el.fRenewal.value = new Date().toISOString().slice(0, 10);
    el.fActive.checked = true;
    el.fReminder.checked = false;
    fillCategorySelect(categories.length ? categories[0].id : null);

    modal.show();
  }

  /* Open the modal in edit mode, pre-filling every field from the existing
   * subscription so the user edits rather than re-enters. */
  function openEdit(id) {
    var sub = subscriptions.find(function (s) {
      return String(s.id) === String(id);
    });
    if (!sub) return;

    editingId = sub.id;
    clearFormError();
    el.modalTitle.textContent = "Edit subscription";
    el.saveBtn.textContent = "Save changes";

    el.fName.value = sub.name || "";
    el.fDescription.value = sub.description || "";
    el.fPrice.value = sub.price != null ? sub.price : "";
    el.fCurrency.value = sub.currency || "EUR";
    fillCycleSelect(sub.billingCycle || "MONTHLY");
    el.fRenewal.value = sub.nextRenewalDate || "";
    el.fActive.checked = sub.active !== false;
    el.fReminder.checked = !!sub.renewalReminderEnabled;
    fillCategorySelect(sub.categoryId);

    modal.show();
  }

  /* Read and lightly validate the form. Returns a request body matching
   * SubscriptionRequest, or null after surfacing an inline error. Server-side
   * validation is authoritative; these checks just save a round-trip. */
  function readForm() {
    var name = el.fName.value.trim();
    var description = el.fDescription.value.trim();
    var priceRaw = el.fPrice.value.trim();
    var currency = el.fCurrency.value.trim().toUpperCase();
    var billingCycle = el.fCycle.value;
    var nextRenewalDate = el.fRenewal.value;
    var categoryId = el.fCategory.value;

    if (!name) return fail("Please enter a subscription name.");
    if (name.length > 100)
      return fail("Subscription name must not exceed 100 characters.");

    var price = Number(priceRaw);
    if (!priceRaw || isNaN(price) || price <= 0)
      return fail("Price must be a number greater than zero.");

    if (!/^[A-Za-z]{3}$/.test(currency))
      return fail("Currency must be a 3-letter code (for example, EUR).");

    if (!nextRenewalDate) return fail("Please choose the next renewal date.");

    if (!categoryId) return fail("Please choose a category.");

    return {
      name: name,
      description: description || null,
      price: price,
      currency: currency,
      billingCycle: billingCycle,
      nextRenewalDate: nextRenewalDate,
      active: el.fActive.checked,
      renewalReminderEnabled: el.fReminder.checked,
      categoryId: Number(categoryId),
    };

    function fail(message) {
      showFormError(message);
      return null;
    }
  }

  async function save() {
    clearFormError();
    var body = readForm();
    if (!body) return;

    el.saveBtn.disabled = true;
    var original = el.saveBtn.textContent;
    el.saveBtn.textContent = "Saving…";

    try {
      if (editingId == null) {
        await Api.post("/subscriptions", body);
      } else {
        await Api.put("/subscriptions/" + editingId, body);
      }
      modal.hide();
      await refresh();
    } catch (e) {
      showFormError(e.message || "Couldn't save the subscription.");
    } finally {
      el.saveBtn.disabled = false;
      el.saveBtn.textContent = original;
    }
  }


  // Delete (with confirmation)


  var pendingDeleteId = null;

  function askDelete(id) {
    var sub = subscriptions.find(function (s) {
      return String(s.id) === String(id);
    });
    if (!sub) return;
    pendingDeleteId = sub.id;
    el.confirmName.textContent = sub.name;
    clearConfirmError();
    confirmModal.show();
  }

  function clearConfirmError() {
    el.confirmError.textContent = "";
    el.confirmError.classList.add("d-none");
  }

  async function confirmDelete() {
    if (pendingDeleteId == null) return;
    el.confirmDeleteBtn.disabled = true;
    var original = el.confirmDeleteBtn.textContent;
    el.confirmDeleteBtn.textContent = "Deleting…";

    try {
      await Api.del("/subscriptions/" + pendingDeleteId);
      pendingDeleteId = null;
      confirmModal.hide();
      await refresh();
    } catch (e) {
      el.confirmError.textContent = e.message || "Couldn't delete the subscription.";
      el.confirmError.classList.remove("d-none");
    } finally {
      el.confirmDeleteBtn.disabled = false;
      el.confirmDeleteBtn.textContent = original;
    }
  }


  // Wiring


  var confirmModal;

  function cacheDom() {
    el.loading = document.getElementById("subsLoading");
    el.error = document.getElementById("subsError");
    el.content = document.getElementById("subsContent");
    el.empty = document.getElementById("subsEmpty");
    el.tableWrap = document.getElementById("subsTableWrap");
    el.tbody = document.getElementById("subsTbody");
    el.count = document.getElementById("subsCount");
    el.addBtn = document.getElementById("subsAddBtn");
    el.emptyAddBtn = document.getElementById("subsEmptyAddBtn");
    el.searchInput = document.getElementById("subsSearch");

    // Form modal
    el.modalTitle = document.getElementById("subModalTitle");
    el.saveBtn = document.getElementById("subSaveBtn");
    el.formError = document.getElementById("subFormError");
    el.fName = document.getElementById("fName");
    el.fDescription = document.getElementById("fDescription");
    el.fPrice = document.getElementById("fPrice");
    el.fCurrency = document.getElementById("fCurrency");
    el.fCycle = document.getElementById("fCycle");
    el.fRenewal = document.getElementById("fRenewal");
    el.fActive = document.getElementById("fActive");
    el.fReminder = document.getElementById("fReminder");
    el.fCategory = document.getElementById("fCategory");

    // Confirm modal
    el.confirmName = document.getElementById("confirmSubName");
    el.confirmError = document.getElementById("confirmError");
    el.confirmDeleteBtn = document.getElementById("confirmDeleteBtn");
  }

  function bindEvents() {
    el.addBtn.addEventListener("click", openCreate);
    if (el.emptyAddBtn) el.emptyAddBtn.addEventListener("click", openCreate);
    el.saveBtn.addEventListener("click", save);
    el.confirmDeleteBtn.addEventListener("click", confirmDelete);

    // Search input
    if (el.searchInput) {
      el.searchInput.addEventListener("input", function () {
        searchQuery = el.searchInput.value;
        renderTable();
      });
    }

    // Sortable column headers
    ["name", "price", "renewal"].forEach(function (key) {
      var th = document.getElementById("sortTh-" + key);
      if (th) th.addEventListener("click", function () { onSortClick(key); });
    });

    // Submit the form on Enter inside the modal
    document
      .getElementById("subForm")
      .addEventListener("submit", function (e) {
        e.preventDefault();
        save();
      });

    // Event delegation for the per-row edit / delete buttons
    el.tbody.addEventListener("click", function (e) {
      var editBtn = e.target.closest("[data-edit]");
      if (editBtn) {
        openEdit(editBtn.getAttribute("data-edit"));
        return;
      }
      var delBtn = e.target.closest("[data-delete]");
      if (delBtn) {
        askDelete(delBtn.getAttribute("data-delete"));
      }
    });
  }

  var started = false;
  function start() {
    if (started) return;
    if (!document.querySelector(".app-shell")) return; // layout not mounted yet
    if (!document.getElementById("subModal")) return; // content not in place yet
    started = true;
    cacheDom();
    modal = new bootstrap.Modal(document.getElementById("subModal"));
    confirmModal = new bootstrap.Modal(document.getElementById("confirmModal"));
    bindEvents();
    refresh();
  }

  // Multiple triggers so we can't miss the moment the layout becomes ready,
  // regardless of script timing or a stale cached file elsewhere.
  document.addEventListener("layout:ready", start);
  document.addEventListener("DOMContentLoaded", start);
  window.addEventListener("load", start);
  start(); // in case the shell is already mounted right now

  // Final safety net: poll briefly so a missed event can never leave the page
  // stuck on the loading card.
  var tries = 0;
  var poll = setInterval(function () {
    if (started || tries++ > 50) {
      clearInterval(poll);
      return;
    }
    start();
  }, 100);
})();