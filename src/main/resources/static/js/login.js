/* SubTrack — login / register page logic.
 *
 * Handles both modes on one screen. On success it stores the JWT via Auth and
 * sends the user to the dashboard. Auth errors are shown inline rather than
 * redirecting, so a wrong password doesn't bounce the user anywhere.
 */
(function () {
  var cfg = window.SubTrackConfig;

  // Someone already signed in shouldn't see the login screen.
  Auth.redirectIfAuthenticated();

  var mode = "login"; // "login" | "register"

  var form, title, sub, emailField, submitBtn, toggleBtn, toggleText, errorBox;

  function qs(id) {
    return document.getElementById(id);
  }

  function setMode(next) {
    mode = next;
    var login = mode === "login";
    title.textContent = login ? "Welcome back" : "Create your account";
    sub.textContent = login
      ? "Sign in to keep tabs on your subscriptions."
      : "Start tracking what your subscriptions really cost.";
    emailField.classList.toggle("d-none", login);
    qs("email").required = !login;
    submitBtn.textContent = login ? "Sign in" : "Create account";
    toggleText.textContent = login
      ? "Don't have an account?"
      : "Already have an account?";
    toggleBtn.textContent = login ? "Create one" : "Sign in";
    clearError();
  }

  function showError(message) {
    errorBox.textContent = message;
    errorBox.classList.remove("d-none");
  }

  function clearError() {
    errorBox.textContent = "";
    errorBox.classList.add("d-none");
  }

  async function submit() {
    clearError();
    var username = qs("username").value.trim();
    var password = qs("password").value;
    var email = qs("email").value.trim();

    if (!username || !password || (mode === "register" && !email)) {
      showError("Please fill in all fields.");
      return;
    }

    var path = mode === "login" ? "/auth/login" : "/auth/register";
    var payload =
      mode === "login"
        ? { username: username, password: password }
        : { username: username, email: email, password: password };

    submitBtn.disabled = true;
    submitBtn.textContent = mode === "login" ? "Signing in…" : "Creating…";

    try {

      var res = await fetch(cfg.API_BASE + path, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(payload),
      });

      var text = await res.text();
      var data = text ? JSON.parse(text) : null;

      if (!res.ok) {
        var msg =
          (data && (data.message || data.error)) ||
          (mode === "login"
            ? "Incorrect username or password."
            : "Couldn't create that account.");
        showError(msg);
        return;
      }

      Auth.setSession(data.token, data.username || username);
      location.replace("dashboard.html");
    } catch (e) {
      showError("Can't reach the server. Check that the API is running.");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = mode === "login" ? "Sign in" : "Create account";
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    form = qs("authForm");
    title = qs("authTitle");
    sub = qs("authSub");
    emailField = qs("emailField");
    submitBtn = qs("submitBtn");
    toggleBtn = qs("toggleBtn");
    toggleText = qs("toggleText");
    errorBox = qs("authError");

    form.addEventListener("submit", function (e) {
      e.preventDefault();
      submit();
    });
    toggleBtn.addEventListener("click", function () {
      setMode(mode === "login" ? "register" : "login");
    });

    // Show / hide password toggles (eye button).
    var EYE =
      '<svg class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
      'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
      '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z"/><circle cx="12" cy="12" r="3"/></svg>';
    var EYE_OFF =
      '<svg class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
      'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
      '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20C5 20 1 12 1 12a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>' +
      '<line x1="1" y1="1" x2="23" y2="23"/></svg>';

    Array.prototype.forEach.call(
      document.querySelectorAll("[data-toggle-pw]"),
      function (btn) {
        btn.innerHTML = EYE;
        btn.addEventListener("click", function () {
          var input = qs(btn.getAttribute("data-toggle-pw"));
          if (!input) return;
          var reveal = input.type === "password";
          input.type = reveal ? "text" : "password";
          btn.innerHTML = reveal ? EYE_OFF : EYE;
          btn.setAttribute("aria-pressed", reveal ? "true" : "false");
          btn.setAttribute("aria-label", reveal ? "Hide password" : "Show password");
        });
      }
    );

    setMode("login");
  });
})();