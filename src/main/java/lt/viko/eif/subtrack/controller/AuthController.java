package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.AuthResponse;
import lt.viko.eif.subtrack.dto.ForgotPasswordRequest;
import lt.viko.eif.subtrack.dto.LoginRequest;
import lt.viko.eif.subtrack.dto.RegisterRequest;
import lt.viko.eif.subtrack.dto.ResetPasswordRequest;
import lt.viko.eif.subtrack.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication.
 * Provides endpoints for user registration, login, and password management.
 *
 * <p>These endpoints are intentionally unauthenticated — they are the entry
 * point for obtaining a JWT token or resetting credentials.</p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, and password reset operations")
@SecurityRequirements   // No auth required on these endpoints in Swagger UI
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account.
     *
     * @param request the registration details
     * @return JWT token and username on success
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the supplied credentials and " +
                    "returns a JWT token that can be used immediately for authenticated requests."
    )
    @RequestBody(
            description = "Username and password for the new account. " +
                    "Username must be unique; password requirements are enforced server-side.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(
                            name = "Example registration",
                            value = """
                                    {
                                      "username": "alice",
                                      "password": "s3cur3P@ss!"
                                    }"""
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account created — JWT token returned",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "username": "alice"
                                    }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — missing or invalid fields",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "username: must not be blank" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username is already taken",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Username 'alice' is already in use" }""")
                    )
            )
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Authenticates an existing user.
     *
     * @param request the login credentials
     * @return JWT token and username on success
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login and receive JWT token",
            description = "Validates the supplied credentials and returns a signed JWT token. " +
                    "Pass this token as `Authorization: Bearer <token>` on all protected endpoints."
    )
    @RequestBody(
            description = "Existing account credentials.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(
                            name = "Example login",
                            value = """
                                    {
                                      "username": "alice",
                                      "password": "s3cur3P@ss!"
                                    }"""
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful — JWT token returned",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "username": "alice"
                                    }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — missing or invalid fields",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "password: must not be blank" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid username or password",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Bad credentials" }""")
                    )
            )
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Sends a password reset email to the given address if an account exists.
     * Always returns 200 regardless of whether the email is registered, to
     * prevent user enumeration.
     */
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request a password reset email",
            description = "Triggers a password reset workflow. If the email exists, an email containing a secure token will be sent. " +
                    "To prevent user enumeration attacks, this endpoint always responds with 200 OK even if the email isn't registered."
    )
    @RequestBody(
            description = "The email address associated with the user account.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ForgotPasswordRequest.class),
                    examples = @ExampleObject(
                            name = "Example forgot password request",
                            value = """
                                    {
                                      "email": "alice@example.com"
                                    }"""
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reset request handled successfully (does not confirm if email exists)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — invalid email format",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "email: must be a well-formed email address" }""")
                    )
            )
    })
    public ResponseEntity<Void> forgotPassword(
            @Valid @org.springframework.web.bind.annotation.RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Validates the reset token and sets the new password.
     * Returns 400 if the token is invalid or expired.
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password using a token from email",
            description = "Consumes the verification token received via email to set a new password for the account."
    )
    @RequestBody(
            description = "The verification token alongside the new password criteria.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ResetPasswordRequest.class),
                    examples = @ExampleObject(
                            name = "Example reset password request",
                            value = """
                                    {
                                      "token": "550e8400-e29b-41d4-a716-446655440000",
                                      "newPassword": "n3wS3cur3P@ss!"
                                    }"""
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password successfully changed"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired token, or weak password formatting",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Invalid or expired password reset token" }""")
                    )
            )
    })
    public ResponseEntity<Void> resetPassword(
            @Valid @org.springframework.web.bind.annotation.RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}