/* SubTrack — authentication helpers.
 *
 * Owns everything about the client-side session: storing the JWT returned by
 * the API, reading it back, checking whether it has expired, guarding pages,
 * and logging out. Pages and the API layer talk to the session only through
 * this module so the storage details live in one place.
 */
(function () {
  var cfg = window.SubTrackConfig;

  function getToken() {
    return localStorage.getItem(cfg.STORAGE_TOKEN);
  }

  function getUsername() {
    return localStorage.getItem(cfg.STORAGE_USER) || "";
  }

  /* Persist the session after a successful login or registration. */
  function setSession(token, username) {
    localStorage.setItem(cfg.STORAGE_TOKEN, token);
    if (username) localStorage.setItem(cfg.STORAGE_USER, username);
  }

  function clearSession() {
    localStorage.removeItem(cfg.STORAGE_TOKEN);
    localStorage.removeItem(cfg.STORAGE_USER);
  }

  /* Decode the JWT payload without verifying the signature — used only to read
   * the expiry so we can redirect proactively instead of waiting for a 401.
   * Verification is the server's job. Returns null if the token is unreadable. */
  function decode(token) {
    try {
      var payload = token.split(".")[1];
      payload = payload.replace(/-/g, "+").replace(/_/g, "/");
      return JSON.parse(atob(payload));
    } catch (e) {
      return null;
    }
  }

  function isExpired(token) {
    var claims = decode(token);
    if (!claims || !claims.exp) return false; // no exp claim → let the server decide
    return claims.exp * 1000 <= Date.now();
  }

  function isAuthenticated() {
    var token = getToken();
    return !!token && !isExpired(token);
  }

  /* Guard an app page. Call at the top of every authenticated page: if there is
   * no valid session the user is sent to the login screen and the page stops.
   * Returns true when the session is valid. */
  function requireAuth() {
    if (isAuthenticated()) return true;
    clearSession();
    location.replace("login.html");
    return false;
  }

  /* Keep the login page from showing to someone already signed in. */
  function redirectIfAuthenticated() {
    if (isAuthenticated()) location.replace("dashboard.html");
  }

  function logout() {
    clearSession();
    location.replace("login.html");
  }

  window.Auth = {
    getToken: getToken,
    getUsername: getUsername,
    setSession: setSession,
    clearSession: clearSession,
    isAuthenticated: isAuthenticated,
    requireAuth: requireAuth,
    redirectIfAuthenticated: redirectIfAuthenticated,
    logout: logout,
  };
})();
