package com.subtrack.service;

import com.subtrack.entity.User;
import com.subtrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        // Arrange
        User mockUser = new User("john_doe", "john@example.com", "hashed_password");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(mockUser));

        // Act
        String token = "john_doe";
        UserDetails userDetails = userDetailsService.loadUserByUsername("john_doe");

        // Assert
        assertNotNull(userDetails);
        assertEquals("john_doe", userDetails.getUsername());
        assertEquals("hashed_password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))); // Spring prefixes roles with "ROLE_" internally

        verify(userRepository, times(1)).findByUsername("john_doe");
    }

    @Test
    void loadUserByUsername_UserDoesNotExist_ThrowsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("unknown_user");
        });

        assertEquals("User not found with username: unknown_user", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("unknown_user");
    }
}