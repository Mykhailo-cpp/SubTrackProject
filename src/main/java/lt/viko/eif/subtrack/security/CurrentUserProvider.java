package lt.viko.eif.subtrack.security;

import lt.viko.eif.subtrack.entity.User;
import lt.viko.eif.subtrack.exception.ResourceNotFoundException;

/**
 * Resolves the currently authenticated user.
 *
 * This abstraction is the single seam between the subscription feature and
 * the authentication mechanism. The production implementation reads the user
 * from Spring Security's context (populated by the JWT filter); tests and
 * pre-authentication development can supply a stub returning a fixed user.
 */
public interface CurrentUserProvider {

    /**
     * Returns the user associated with the current request.
     *
     * @return the authenticated {@link User}
     * @throws ResourceNotFoundException if the authenticated
     *         principal does not correspond to a persisted user
     * @throws IllegalStateException if there is no authenticated user in the context
     */
    User getCurrentUser();
}