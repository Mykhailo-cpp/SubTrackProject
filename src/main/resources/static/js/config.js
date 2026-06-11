/* SubTrack frontend configuration.
 *
 * The frontend is served by the same Spring Boot application as the API, so a
 * relative base works and avoids any CORS configuration. If you ever host the
 * UI on a different origin, set API_BASE to the full backend URL (e.g.
 * "http://localhost:8080/api") and add a CORS mapping on the server.
 */
window.SubTrackConfig = {
  API_BASE: "/api",
  STORAGE_TOKEN: "subtrack_token",
  STORAGE_USER: "subtrack_user",
};
