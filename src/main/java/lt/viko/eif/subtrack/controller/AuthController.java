package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.AuthResponse;
import lt.viko.eif.subtrack.dto.LoginRequest;
import lt.viko.eif.subtrack.dto.RegisterRequest;
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
 * Provides endpoints for user registration and login.
 *
 * <p>These endpoints are intentionally unauthenticated — they are the entry
 * point for obtaining a JWT token.</p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register a new account or log in to receive a JWT bearer token")
@SecurityRequirements   // no auth required on these endpoints
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
            summary = "Log in and receive a JWT token",
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
}