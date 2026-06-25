package com.pagatu.auth.service;

import com.pagatu.auth.dto.ChangePasswordRequest;
import com.pagatu.auth.dto.UpdateUserProfileRequest;
import com.pagatu.auth.dto.UserProfileDto;
import com.pagatu.auth.entity.AuthProvider;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.exception.AuthenticationException;
import com.pagatu.auth.exception.ValidationException;
import com.pagatu.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setDateOfBirth(LocalDate.of(1990, 5, 15));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPassword("encoded-old");
    }

    @Test
    void getProfile_returnsUserData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileDto result = profileService.getProfile(1L);

        assertEquals("testuser", result.getUsername());
        assertEquals("Mario", result.getFirstName());
        assertEquals(AuthProvider.LOCAL, result.getAuthProvider());
    }

    @Test
    void updateProfile_syncsWithCoffee() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFirstName("Luigi");

        profileService.updateProfile(1L, request);

        verify(authService).syncProfileWithCoffeeService(any(User.class));
        assertEquals("Luigi", user.getFirstName());
    }

    @Test
    void changePassword_oauthUser_throwsValidationException() {
        user.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("NewPass123!");

        assertThrows(ValidationException.class, () -> profileService.changePassword(1L, request));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsAuthenticationException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("NewPass123!");

        assertThrows(AuthenticationException.class, () -> profileService.changePassword(1L, request));
    }

    @Test
    void changePassword_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("NewPass123!", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("encoded-new");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        profileService.changePassword(1L, request);

        verify(userRepository).save(user);
        assertEquals("encoded-new", user.getPassword());
    }
}