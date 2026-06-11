package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.AuthResponse;
import lt.viko.eif.subtrack.dto.LoginRequest;
import lt.viko.eif.subtrack.dto.RegisterRequest;

/**
 * Service interface for user authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user and returns a JWT token.
     *
     * @param request the registration details
     * @return AuthResponse containing JWT token and username
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login credentials
     * @return AuthResponse containing JWT token and username
     */
    AuthResponse login(LoginRequest request);
}
