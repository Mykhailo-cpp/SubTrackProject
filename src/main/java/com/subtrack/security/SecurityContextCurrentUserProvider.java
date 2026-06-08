package com.subtrack.security;

import com.subtrack.entity.User;
import com.subtrack.exception.ResourceNotFoundException;
import com.subtrack.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Production {@link CurrentUserProvider} that resolves the authenticated user
 * from Spring Security's {@link SecurityContextHolder}.
 *
 * Once the JWT authentication filter (issue #12) populates the security
 * context, {@link Authentication#getName()} yields the authenticated
 * username, which is then loaded from the {@link UserRepository}. Until that
 * filter exists, requests will not be authenticated and this provider will
 * report no current user; a development stub can be substituted in the
 * meantime.
 */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    /** Repository used to load the persisted user for the authenticated principal. */
    private final UserRepository userRepository;

    /**
     * Creates the provider with its required repository.
     *
     * @param userRepository the user repository
     */
    public SecurityContextCurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** {@inheritDoc} */
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + username));
    }
}