/* SubTrack — API client.
 *
 * A thin wrapper over fetch that attaches the JWT, parses JSON, normalises
 * errors, and handles an expired session centrally. Feature tickets (the
 * subscriptions list, the analytics dashboard, categories) should call
 * Api.get/post/put/del rather than using fetch directly.
 */
(function () {
  var cfg = window.SubTrackConfig;

  /* Error carrying the HTTP status and a human-readable message so callers can
   * branch on status (e.g. 409 duplicate) and show .message to the user. */
  function ApiError(message, status) {
    this.name = "ApiError";
    this.message = message;
    this.status = status;
  }
  ApiError.prototype = Object.create(Error.prototype);

  async function request(path, options) {
    options = options || {};
    var headers = Object.assign(
      { Accept: "application/json" },
      options.headers || {}
    );

    var token = Auth.getToken();
    if (token) headers["Authorization"] = "Bearer " + token;

    // Only set a JSON content type when we are actually sending a body.
    if (options.body != null && !headers["Content-Type"]) {
      headers["Content-Type"] = "application/json";
    }

    var res;
    try {
      res = await fetch(cfg.API_BASE + path, Object.assign({}, options, { headers }));
    } catch (networkError) {
      throw new ApiError(
        "Can't reach the server. Check that the API is running.",
        0
      );
    }

    // Session no longer valid — clear it and bounce to login.
    if (res.status === 401) {
      Auth.clearSession();
      location.replace("login.html");
      throw new ApiError("Your session has expired. Please sign in again.", 401);
    }

    if (res.status === 204) return null;

    var text = await res.text();
    var data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = text;
      }
    }

    if (!res.ok) {
      var message =
        (data && (data.message || data.error)) ||
        "Request failed (" + res.status + ").";
      throw new ApiError(message, res.status);
    }

    return data;
  }

  /* Spring HATEOAS returns collections as
   *   { _embedded: { <name>: [...] }, _links: {...} }
   * and omits _embedded entirely when the collection is empty. This unwraps to
   * a plain array regardless. Plain-array endpoints pass through unchanged. */
  function unwrapCollection(data, embeddedKey) {
    if (Array.isArray(data)) return data;
    if (data && data._embedded && data._embedded[embeddedKey]) {
      return data._embedded[embeddedKey];
    }
    return [];
  }

  window.Api = {
    ApiError: ApiError,
    request: request,
    get: function (path) {
      return request(path, { method: "GET" });
    },
    post: function (path, body) {
      return request(path, { method: "POST", body: JSON.stringify(body) });
    },
    put: function (path, body) {
      return request(path, { method: "PUT", body: JSON.stringify(body) });
    },
    del: function (path) {
      return request(path, { method: "DELETE" });
    },
    unwrapCollection: unwrapCollection,
  };
})();
