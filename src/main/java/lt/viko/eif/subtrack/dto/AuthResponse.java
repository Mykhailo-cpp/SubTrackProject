package lt.viko.eif.subtrack.dto;

/**
 * Response payload returned after a successful registration or login.
 *
 * <p>Immutable by design — the server constructs this once and serialises it;
 * there is no reason for any caller to mutate it after creation.</p>
 *
 * @param token    the signed JWT the client must supply on subsequent requests
 * @param username the authenticated user's username
 */
public record AuthResponse(String token, String username) {}