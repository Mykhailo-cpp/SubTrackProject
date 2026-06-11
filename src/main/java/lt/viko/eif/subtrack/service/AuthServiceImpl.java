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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of {@link AuthService} for user registration, login,
 * and password reset.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** How long a password-reset token remains valid. */
    private static final int RESET_TOKEN_VALIDITY_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuthenticationManager authenticationManager,
                           PasswordResetTokenRepository tokenRepository,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /** {@inheritDoc} */
    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }

    /** {@inheritDoc} */
    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword())
        );
        String token = jwtUtil.generateToken(request.getUsername());
        return new AuthResponse(token, request.getUsername());
    }

    /**
     * {@inheritDoc}
     *
     * <p>If no account exists for the given email, the method returns
     * silently — no exception is thrown and no email is sent. This prevents
     * callers from discovering which email addresses are registered
     * (user enumeration attack).</p>
     */
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresentOrElse(user -> {
            // Delete any existing token for this user so only one is ever active
            tokenRepository.deleteByUser(user);

            String rawToken = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(RESET_TOKEN_VALIDITY_HOURS);
            tokenRepository.save(new PasswordResetToken(rawToken, user, expiresAt));

            String subject = "SubTrack — password reset request";
            String body = String.format(
                    "Hi %s,%n%n" +
                            "We received a request to reset your SubTrack password.%n%n" +
                            "Use the token below in the password reset form (valid for %d hour):%n%n" +
                            "  %s%n%n" +
                            "If you did not request a password reset, you can safely ignore this email.%n%n" +
                            "— The SubTrack team",
                    user.getUsername(), RESET_TOKEN_VALIDITY_HOURS, rawToken
            );
            emailService.sendEmail(user.getEmail(), subject, body);
            log.info("Password reset token issued for user '{}'", user.getUsername());

        }, () -> log.warn("Password reset requested for unknown email '{}'", request.getEmail()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>After a successful reset the token is deleted immediately so it
     * cannot be reused.</p>
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Password reset token not found or already used"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException(
                    "Password reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete the token so it cannot be reused
        tokenRepository.delete(resetToken);
        log.info("Password successfully reset for user '{}'", user.getUsername());
    }
}
