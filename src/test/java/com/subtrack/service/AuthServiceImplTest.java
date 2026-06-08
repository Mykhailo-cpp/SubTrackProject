package com.subtrack.service;

import com.subtrack.dto.AuthResponse;
import com.subtrack.dto.LoginRequest;
import com.subtrack.dto.RegisterRequest;
import com.subtrack.entity.User;
import com.subtrack.repository.UserRepository;
import com.subtrack.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        loginRequest = new LoginRequest("testuser", "password123");
    }

    @Nested
    class RegisterTests {

        @Test
        void register_Success_ReturnsAuthResponse() {
            // Arrange
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
            when(jwtUtil.generateToken(registerRequest.getUsername())).thenReturn("mocked-jwt-token");

            // Act
            AuthResponse response = authService.register(registerRequest);

            // Assert
            assertNotNull(response);
            assertEquals("mocked-jwt-token", response.getToken());
            assertEquals("testuser", response.getUsername());

            // Verify interactions
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        void register_UsernameExists_ThrowsIllegalArgumentException() {
            // Arrange
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                authService.register(registerRequest);
            });

            assertEquals("Username already exists: testuser", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void register_EmailExists_ThrowsIllegalArgumentException() {
            // Arrange
            when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                authService.register(registerRequest);
            });

            assertEquals("Email already registered: test@example.com", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    class LoginTests {

        @Test
        void login_Success_ReturnsAuthResponse() {
            // Arrange
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

            when(authenticationManager.authenticate(authToken)).thenReturn(authToken);
            when(jwtUtil.generateToken(loginRequest.getUsername())).thenReturn("mocked-jwt-token");

            // Act
            AuthResponse response = authService.login(loginRequest);

            // Assert
            assertNotNull(response);
            assertEquals("mocked-jwt-token", response.getToken());
            assertEquals("testuser", response.getUsername());

            verify(authenticationManager, times(1)).authenticate(authToken);
        }

        @Test
        void login_InvalidCredentials_ThrowsException() {
            // Arrange
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

            // Simulate Spring Security throwing an exception for bad credentials
            when(authenticationManager.authenticate(authToken))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
                authService.login(loginRequest);
            });

            verify(jwtUtil, never()).generateToken(anyString());
        }
    }
}