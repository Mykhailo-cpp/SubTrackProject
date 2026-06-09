package com.subtrack.security;

import com.subtrack.entity.User;

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
     * @throws com.subtrack.exception.ResourceNotFoundException if the authenticated
     *         principal does not correspond to a persisted user
     * @throws IllegalStateException if there is no authenticated user in the context
     */
    User getCurrentUser();
}