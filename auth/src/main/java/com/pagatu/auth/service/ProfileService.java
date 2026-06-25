package com.pagatu.auth.service;

import com.pagatu.auth.dto.ChangePasswordRequest;
import com.pagatu.auth.dto.UpdateUserProfileRequest;
import com.pagatu.auth.dto.UserProfileDto;
import com.pagatu.auth.entity.AuthProvider;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.exception.AuthenticationException;
import com.pagatu.auth.exception.UserNotFoundException;
import com.pagatu.auth.exception.ValidationException;
import com.pagatu.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = findUser(userId);
        return toDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateUserProfileRequest request) {
        if (request.getFirstName() == null && request.getLastName() == null && request.getDateOfBirth() == null) {
            throw new ValidationException("At least one field must be provided", Map.of(
                    "profile", List.of("firstName, lastName or dateOfBirth required")));
        }

        User user = findUser(userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().isBlank() ? null : request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().isBlank() ? null : request.getLastName().trim());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        User saved = userRepository.save(user);
        authService.syncProfileWithCoffeeService(saved);
        log.info("Profile updated for user: {}", saved.getUsername());
        return toDto(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ValidationException("Password change not available for OAuth accounts", Map.of(
                    "authProvider", List.of("Use your provider to manage password")));
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ValidationException("New password must differ from current password", Map.of(
                    "newPassword", List.of("Choose a different password")));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found", String.valueOf(userId), "id"));
    }

    private UserProfileDto toDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDateOfBirth(),
                user.getAuthProvider());
    }
}