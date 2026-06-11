/* SubTrack — categories page logic.
 *
 * Lists the shared categories and drives create / edit / delete through one
 * modal, mirroring the subscriptions page. Categories are not user-scoped on
 * the server, so every signed-in user sees and edits the same set. Each
 * mutation re-fetches the list so the page mirrors the server.
 *
 * Data flow:
 *   - GET    /categories        → list (plain array)
 *   - POST   /categories        → create  (409 if the name already exists)
 *   - PUT    /categories/{id}    → update  (409 on a name clash)
 *   - DELETE /categories/{id}    → delete
 * The JWT is attached centrally by Api; nothing here touches the token.
 */
(function () {
  var categories = [];
  var subscriptions = [];
  var modal;
  var confirmModal;
  var editingId = null;
  var pendingDeleteId = null;
  var el = {};

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  // ---------------------------------------------------------------------------
  // Data loading
  // ---------------------------------------------------------------------------

  async function loadCategories() {
    showState("loading");
    try {
      var catData = await Api.get("/categories");
      categories = Api.unwrapCollection(catData, "categoryResponseList");
      // Also load subscriptions so we can show them per category
      try {
        var subData = await Api.get("/subscriptions");
        subscriptions = Api.unwrapCollection(subData, "subscriptionResponseList");
      } catch (e) {
        subscriptions = [];
      }
      renderList();
    } catch (e) {
      showState("error", e.message || "Couldn't load categories.");
    }
  }

  // ---------------------------------------------------------------------------
  // Rendering
  // ---------------------------------------------------------------------------

  function showState(state, message) {
    el.loading.classList.toggle("d-none", state !== "loading");
    el.error.classList.toggle("d-none", state !== "error");
    el.content.classList.toggle("d-none", state !== "content");
    if (state === "error" && message) el.error.textContent = message;
  }

  function renderList() {
    showState("content");

    var n = categories.length;
    el.count.textContent =
      n === 0 ? "" : n + (n === 1 ? " category" : " categories");

    if (!n) {
      el.empty.classList.remove("d-none");
      el.grid.classList.add("d-none");
      return;
    }

    el.empty.classList.add("d-none");
    el.grid.classList.remove("d-none");

    el.grid.innerHTML = categories
      .map(function (cat) {
        // Find subscriptions belonging to this category
        var catSubs = subscriptions.filter(function (s) {
          return String(s.categoryId) === String(cat.id) ||
                 (s.categoryName && s.categoryName === cat.name);
        });

        var subsHtml = "";
        if (catSubs.length) {
          subsHtml =
            '<ul class="cat-subs-list">' +
            catSubs.map(function (s) {
              var inactive = s.active === false;
              return (
                '<li class="cat-sub-item' + (inactive ? " cat-sub-inactive" : "") + '">' +
                '<span class="cat-sub-name">' + escapeHtml(s.name) + "</span>" +
                '<span class="cat-sub-price">' + escapeHtml(s.currency || "") + " " +
                (Number(s.price) || 0).toFixed(2) + "</span>" +
                "</li>"
              );
            }).join("") +
            "</ul>";
        } else {
          subsHtml = '<p class="cat-subs-empty">No subscriptions yet</p>';
        }

        return (
          '<div class="cat-card">' +
          '<div class="cat-card-body">' +
          '<div class="cat-name">' +
          escapeHtml(cat.name) +
          "</div>" +
          (cat.description
            ? '<div class="cat-desc">' + escapeHtml(cat.description) + "</div>"
            : '<div class="cat-desc cat-desc--empty">No description</div>') +
          '<div class="cat-subs">' +
          '<div class="cat-subs-header">' +
          catSubs.length + (catSubs.length === 1 ? " subscription" : " subscriptions") +
          "</div>" +
          subsHtml +
          "</div>" +
          "</div>" +
          '<div class="cat-card-actions">' +
          '<button type="button" class="btn btn-sm btn-outline-secondary me-1" data-edit="' +
          cat.id +
          '">Edit</button>' +
          '<button type="button" class="btn btn-sm btn-outline-danger" data-delete="' +
          cat.id +
          '">Delete</button>' +
          "</div>" +
          "</div>"
        );
      })
      .join("");
  }

  // ---------------------------------------------------------------------------
  // Modal: create / edit
  // ---------------------------------------------------------------------------

  function clearFormError() {
    el.formError.textContent = "";
    el.formError.classList.add("d-none");
  }

  function showFormError(message) {
    el.formError.textContent = message;
    el.formError.classList.remove("d-none");
  }

  function openCreate() {
    editingId = null;
    clearFormError();
    el.modalTitle.textContent = "Add category";
    el.saveBtn.textContent = "Add category";
    el.fName.value = "";
    el.fDescription.value = "";
    modal.show();
  }

  function openEdit(id) {
    var cat = categories.find(function (c) {
      return String(c.id) === String(id);
    });
    if (!cat) return;
    editingId = cat.id;
    clearFormError();
    el.modalTitle.textContent = "Edit category";
    el.saveBtn.textContent = "Save changes";
    el.fName.value = cat.name || "";
    el.fDescription.value = cat.description || "";
    modal.show();
  }

  function readForm() {
    var name = el.fName.value.trim();
    var description = el.fDescription.value.trim();

    if (!name) {
      showFormError("Please enter a category name.");
      return null;
    }
    if (name.length > 255) {
      showFormError("Category name must not exceed 255 characters.");
      return null;
    }
    return { name: name, description: description || null };
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
        await Api.post("/categories", body);
      } else {
        await Api.put("/categories/" + editingId, body);
      }
      modal.hide();
      await loadCategories();
    } catch (e) {
      // 409 → duplicate name; the server's message is already user-friendly.
      showFormError(e.message || "Couldn't save the category.");
    } finally {
      el.saveBtn.disabled = false;
      el.saveBtn.textContent = original;
    }
  }

  // ---------------------------------------------------------------------------
  // Delete (with confirmation)
  // ---------------------------------------------------------------------------

  function askDelete(id) {
    var cat = categories.find(function (c) {
      return String(c.id) === String(id);
    });
    if (!cat) return;
    pendingDeleteId = cat.id;
    el.confirmName.textContent = cat.name;
    el.confirmError.classList.add("d-none");
    confirmModal.show();
  }

  async function confirmDelete() {
    if (pendingDeleteId == null) return;
    el.confirmDeleteBtn.disabled = true;
    var original = el.confirmDeleteBtn.textContent;
    el.confirmDeleteBtn.textContent = "Deleting…";

    try {
      await Api.del("/categories/" + pendingDeleteId);
      pendingDeleteId = null;
      confirmModal.hide();
      await loadCategories();
    } catch (e) {
      el.confirmError.textContent = e.message || "Couldn't delete the category.";
      el.confirmError.classList.remove("d-none");
    } finally {
      el.confirmDeleteBtn.disabled = false;
      el.confirmDeleteBtn.textContent = original;
    }
  }

  // ---------------------------------------------------------------------------
  // Wiring
  // ---------------------------------------------------------------------------

  function cacheDom() {
    el.loading = document.getElementById("catLoading");
    el.error = document.getElementById("catError");
    el.content = document.getElementById("catContent");
    el.empty = document.getElementById("catEmpty");
    el.grid = document.getElementById("catGrid");
    el.count = document.getElementById("catCount");
    el.addBtn = document.getElementById("catAddBtn");
    el.emptyAddBtn = document.getElementById("catEmptyAddBtn");

    el.modalTitle = document.getElementById("catModalTitle");
    el.saveBtn = document.getElementById("catSaveBtn");
    el.formError = document.getElementById("catFormError");
    el.fName = document.getElementById("catFName");
    el.fDescription = document.getElementById("catFDescription");

    el.confirmName = document.getElementById("confirmCatName");
    el.confirmError = document.getElementById("confirmCatError");
    el.confirmDeleteBtn = document.getElementById("confirmCatDeleteBtn");
  }

  function bindEvents() {
    el.addBtn.addEventListener("click", openCreate);
    if (el.emptyAddBtn) el.emptyAddBtn.addEventListener("click", openCreate);
    el.saveBtn.addEventListener("click", save);
    el.confirmDeleteBtn.addEventListener("click", confirmDelete);

    document.getElementById("catForm").addEventListener("submit", function (e) {
      e.preventDefault();
      save();
    });

    el.grid.addEventListener("click", function (e) {
      var editBtn = e.target.closest("[data-edit]");
      if (editBtn) {
        openEdit(editBtn.getAttribute("data-edit"));
        return;
      }
      var delBtn = e.target.closest("[data-delete]");
      if (delBtn) askDelete(delBtn.getAttribute("data-delete"));
    });
  }

  // Start once the layout has rebuilt #page-content. Deferred scripts run in
  // order, so layout.js may mount and dispatch `layout:ready` before this
  // listener is registered; we therefore also check for the shell and start
  // directly if it's already there. The guard prevents a double start.
  var started = false;
  function start() {
    if (started) return;
    started = true;
    cacheDom();
    modal = new bootstrap.Modal(document.getElementById("catModal"));
    confirmModal = new bootstrap.Modal(document.getElementById("catConfirmModal"));
    bindEvents();
    loadCategories();
  }

  document.addEventListener("layout:ready", start);
  if (document.querySelector(".app-shell")) start();
})();