package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.AuthResponse;
import lt.viko.eif.subtrack.dto.ForgotPasswordRequest;
import lt.viko.eif.subtrack.dto.LoginRequest;
import lt.viko.eif.subtrack.dto.RegisterRequest;
import lt.viko.eif.subtrack.dto.ResetPasswordRequest;

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

    /**
     * Sends a password reset email if the given address belongs to a
     * registered account. Always returns normally — even if no account
     * is found — to prevent user enumeration.
     *
     * @param request contains the email address to look up
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Validates the reset token and sets the new password.
     *
     * @param request contains the token and the desired new password
     * @throws lt.viko.eif.subtrack.exception.ResourceNotFoundException if the token does not exist
     * @throws IllegalArgumentException if the token has expired
     */
    void resetPassword(ResetPasswordRequest request);
}
