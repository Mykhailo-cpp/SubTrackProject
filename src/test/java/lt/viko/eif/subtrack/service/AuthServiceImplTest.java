package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.AuthResponse;
import lt.viko.eif.subtrack.dto.ForgotPasswordRequest;
import lt.viko.eif.subtrack.dto.LoginRequest;
import lt.viko.eif.subtrack.dto.RegisterRequest;
import lt.viko.eif.subtrack.dto.ResetPasswordRequest;
import lt.viko.eif.subtrack.entity.PasswordResetToken;
import lt.viko.eif.subtrack.entity.User;
import lt.viko.eif.subtrack.exception.ResourceNotFoundException;
import lt.viko.eif.subtrack.repository.PasswordResetTokenRepository;
import lt.viko.eif.subtrack.repository.UserRepository;
import lt.viko.eif.subtrack.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "T3st@pass!");
        loginRequest    = new LoginRequest("testuser", "T3st@pass!");

        user = new User("testuser", "test@example.com", "hashed");
        user.setId(1L);
    }

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @Nested
    class RegisterTests {

        @Test
        void register_Success_ReturnsAuthResponse() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("T3st@pass!")).thenReturn("hashedPassword");
            when(jwtUtil.generateToken("testuser")).thenReturn("mocked-jwt-token");

            AuthResponse response = authService.register(registerRequest);

            assertNotNull(response);
            assertEquals("mocked-jwt-token", response.token());
            assertEquals("testuser", response.username());
            verify(userRepository).save(any(User.class));
        }

        @Test
        void register_UsernameExists_ThrowsIllegalArgumentException() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Username already exists: testuser", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void register_EmailExists_ThrowsIllegalArgumentException() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> authService.register(registerRequest));

            assertEquals("Email already registered: test@example.com", ex.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Nested
    class LoginTests {

        @Test
        void login_Success_ReturnsAuthResponse() {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken("testuser", "T3st@pass!");
            when(authenticationManager.authenticate(authToken)).thenReturn(authToken);
            when(jwtUtil.generateToken("testuser")).thenReturn("mocked-jwt-token");

            AuthResponse response = authService.login(loginRequest);

            assertNotNull(response);
            assertEquals("mocked-jwt-token", response.token());
            assertEquals("testuser", response.username());
            verify(authenticationManager).authenticate(authToken);
        }

        @Test
        void login_InvalidCredentials_ThrowsBadCredentialsException() {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken("testuser", "T3st@pass!");
            when(authenticationManager.authenticate(authToken))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
            verify(jwtUtil, never()).generateToken(anyString());
        }
    }

    // -------------------------------------------------------------------------
    // Forgot password
    // -------------------------------------------------------------------------

    @Nested
    class ForgotPasswordTests {

        @Test
        void forgotPassword_KnownEmail_SavesTokenAndSendsEmail() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            authService.forgotPassword(new ForgotPasswordRequest("test@example.com"));

            // Token must be persisted
            ArgumentCaptor<PasswordResetToken> tokenCaptor =
                    ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            PasswordResetToken saved = tokenCaptor.getValue();
            assertNotNull(saved.getToken());
            assertEquals(user, saved.getUser());
            assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));

            // Email must be sent
            verify(emailService).sendEmail(
                    eq("test@example.com"),
                    anyString(),
                    anyString());
        }

        @Test
        void forgotPassword_UnknownEmail_DoesNotSendEmail() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            // Must not throw — user enumeration prevention
            assertDoesNotThrow(() ->
                    authService.forgotPassword(new ForgotPasswordRequest("nobody@example.com")));

            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        void forgotPassword_ExistingTokenDeleted_BeforeIssuingNew() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            authService.forgotPassword(new ForgotPasswordRequest("test@example.com"));

            // Old token for this user must be deleted first
            verify(tokenRepository).deleteByUser(user);
        }
    }

    // -------------------------------------------------------------------------
    // Reset password
    // -------------------------------------------------------------------------

    @Nested
    class ResetPasswordTests {

        @Test
        void resetPassword_ValidToken_UpdatesPasswordAndDeletesToken() {
            PasswordResetToken token = new PasswordResetToken(
                    "valid-token", user, LocalDateTime.now().plusHours(1));
            when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NewP@ss1!")).thenReturn("newHashed");

            authService.resetPassword(new ResetPasswordRequest("valid-token", "NewP@ss1!"));

            // Password updated
            assertEquals("newHashed", user.getPassword());
            verify(userRepository).save(user);

            // Token deleted so it cannot be reused
            verify(tokenRepository).delete(token);
        }

        @Test
        void resetPassword_InvalidToken_ThrowsResourceNotFoundException() {
            when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    authService.resetPassword(new ResetPasswordRequest("bad-token", "NewP@ss1!")));

            verify(userRepository, never()).save(any());
        }

        @Test
        void resetPassword_ExpiredToken_ThrowsIllegalArgumentException() {
            PasswordResetToken expired = new PasswordResetToken(
                    "expired-token", user, LocalDateTime.now().minusMinutes(1));
            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    authService.resetPassword(new ResetPasswordRequest("expired-token", "NewP@ss1!")));

            assertTrue(ex.getMessage().contains("expired"));
            // Expired token must be cleaned up
            verify(tokenRepository).delete(expired);
            verify(userRepository, never()).save(any());
        }
    }
}