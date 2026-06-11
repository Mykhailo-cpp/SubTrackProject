/* SubTrack — change-password page logic (email-code flow).
 *
 * Standalone page (no app shell / session guard) reachable from the login
 * screen. Two steps, wired to the existing auth endpoints:
 *
 *   1. Send code   → POST /auth/forgot-password { email }
 *                    Backend emails a one-time token (the "code").
 *   2. Verify+set  → POST /auth/reset-password  { token, newPassword }
 *                    Consumes the code and sets the new password.
 *
 * Adds: live inline validation (requirements + match) and show/hide toggles.
 */
(function () {
  var cfg = window.SubTrackConfig;

  /* Endpoint contract — adjust here if your backend differs. */
  var SEND_CODE_PATH = "/auth/forgot-password"; // body: { email }
  var CHANGE_PATH = "/auth/reset-password"; // body: { token, newPassword }

  // Same rule enforced server-side (see ResetPasswordRequest): one special char.
  var SPECIAL_CHAR = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/;

  var EYE =
    '<svg class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
    'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
    '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z"/><circle cx="12" cy="12" r="3"/></svg>';
  var EYE_OFF =
    '<svg class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
    'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
    '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20C5 20 1 12 1 12a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>' +
    '<line x1="1" y1="1" x2="23" y2="23"/></svg>';

  function qs(id) {
    return document.getElementById(id);
  }

  /* New password meets the same rule the server enforces. */
  function meetsRequirements(pw) {
    return pw.length >= 8 && SPECIAL_CHAR.test(pw);
  }

  document.addEventListener("DOMContentLoaded", function () {
    var form = qs("cpForm");
    var email = qs("cpEmail");
    var next = qs("cpNew");
    var confirm = qs("cpConfirm");
    var code = qs("cpCode");
    var sendBtn = qs("cpSendBtn");
    var verifyBtn = qs("cpVerifyBtn");
    var resendBtn = qs("cpResendBtn");
    var codeSection = qs("cpCodeSection");
    var errorBox = qs("cpError");
    var successBox = qs("cpSuccess");
    var infoBox = qs("cpInfo");
    var newError = qs("cpNewError");
    var confirmError = qs("cpConfirmError");

    function show(box, msg) {
      [errorBox, successBox, infoBox].forEach(function (b) {
        if (b !== box) b.classList.add("d-none");
      });
      box.textContent = msg;
      box.classList.remove("d-none");
    }
    function clearAlerts() {
      [errorBox, successBox, infoBox].forEach(function (b) {
        b.classList.add("d-none");
        b.textContent = "";
      });
    }

    /* ---- Show / hide password toggles ---------------------------------- */
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

    /* ---- Live inline validation ---------------------------------------- */
    // Wrong password = doesn't meet requirements (length / special character).
    function checkRequirements() {
      var val = next.value;
      if (val && !meetsRequirements(val)) {
        newError.textContent =
          "Password must be at least 8 characters and include one special character.";
        newError.classList.remove("d-none");
        return false;
      }
      newError.classList.add("d-none");
      return true;
    }
    // Repeat must match the new password.
    function checkMatch() {
      if (confirm.value && confirm.value !== next.value) {
        confirmError.textContent = "Passwords don't match.";
        confirmError.classList.remove("d-none");
        return false;
      }
      confirmError.classList.add("d-none");
      return true;
    }

    next.addEventListener("input", function () {
      checkRequirements();
      checkMatch();
    });
    confirm.addEventListener("input", checkMatch);

    /* ---- Step validation before sending a code ------------------------- */
    function validateDetails() {
      var emailVal = email.value.trim();
      if (!emailVal || !next.value || !confirm.value) {
        return "Please fill in email and both password fields.";
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailVal)) {
        return "Please enter a valid email address.";
      }
      if (!meetsRequirements(next.value)) {
        return "Password must be at least 8 characters and include one special character.";
      }
      if (next.value !== confirm.value) {
        return "Passwords don't match.";
      }
      return null;
    }

    async function post(path, body) {
      var res = await fetch(cfg.API_BASE + path, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(body),
      });
      var text = await res.text();
      var data = text ? JSON.parse(text) : null;
      return { ok: res.ok, status: res.status, data: data };
    }

    async function sendCode() {
      clearAlerts();
      checkRequirements();
      checkMatch();
      var problem = validateDetails();
      if (problem) {
        show(errorBox, problem);
        return;
      }

      sendBtn.disabled = true;
      resendBtn.disabled = true;
      sendBtn.textContent = "Sending…";

      try {
        // forgot-password always returns 200 (anti-enumeration), so success
        // here doesn't confirm the email exists — the verify step will.
        var r = await post(SEND_CODE_PATH, { email: email.value.trim() });
        if (!r.ok) {
          show(errorBox, (r.data && (r.data.message || r.data.error)) ||
            "Couldn't send the code. Please try again.");
          return;
        }
        // Lock the details so they can't drift from what the code was issued for.
        email.readOnly = true;
        next.readOnly = true;
        confirm.readOnly = true;
        codeSection.classList.remove("d-none");
        sendBtn.classList.add("d-none");
        code.focus();
        show(infoBox, "If that email is registered, a verification code is on its way. " +
          "Paste it below to finish.");
      } catch (e) {
        show(errorBox, "Can't reach the server. Check that the API is running.");
      } finally {
        sendBtn.disabled = false;
        resendBtn.disabled = false;
        sendBtn.textContent = "Send code";
      }
    }

    async function verifyAndChange() {
      clearAlerts();
      var codeVal = code.value.trim();
      if (!codeVal) {
        show(errorBox, "Enter the verification code from your email.");
        return;
      }

      verifyBtn.disabled = true;
      verifyBtn.textContent = "Updating…";

      try {
        var r = await post(CHANGE_PATH, { token: codeVal, newPassword: next.value });
        if (!r.ok) {
          show(errorBox, (r.data && (r.data.message || r.data.error)) ||
            "Invalid or expired code. Please check it or resend a new one.");
          return;
        }
        form.reset();
        codeSection.classList.add("d-none");
        show(successBox, "Password updated. You can now sign in with your new password.");
      } catch (e) {
        show(errorBox, "Can't reach the server. Check that the API is running.");
      } finally {
        verifyBtn.disabled = false;
        verifyBtn.textContent = "Verify & change password";
      }
    }

    sendBtn.addEventListener("click", sendCode);
    resendBtn.addEventListener("click", sendCode);

    // The form's submit (Enter key or the verify button) does the change step.
    form.addEventListener("submit", function (e) {
      e.preventDefault();
      if (codeSection.classList.contains("d-none")) {
        sendCode();
      } else {
        verifyAndChange();
      }
    });
  });
})();