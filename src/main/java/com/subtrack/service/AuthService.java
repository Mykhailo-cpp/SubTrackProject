package com.subtrack.service;

import com.subtrack.dto.AuthResponse;
import com.subtrack.dto.LoginRequest;
import com.subtrack.dto.RegisterRequest;

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
