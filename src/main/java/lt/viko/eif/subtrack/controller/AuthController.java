package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.AuthResponse;
import lt.viko.eif.subtrack.dto.ForgotPasswordRequest;
import lt.viko.eif.subtrack.dto.LoginRequest;
import lt.viko.eif.subtrack.dto.RegisterRequest;
import lt.viko.eif.subtrack.dto.ResetPasswordRequest;
import lt.viko.eif.subtrack.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication.
 * Provides endpoints for registration, login, and password reset.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login and password reset")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Sends a password reset email to the given address if an account exists.
     * Always returns 200 regardless of whether the email is registered, to
     * prevent user enumeration.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Validates the reset token and sets the new password.
     * Returns 400 if the token is invalid or expired.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a token from email")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}