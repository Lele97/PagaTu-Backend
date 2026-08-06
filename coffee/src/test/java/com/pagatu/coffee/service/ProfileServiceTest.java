package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UserProfileDto;
import com.pagatu.coffee.dto.UserProfileRequest;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.exception.ValidationException;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private BaseUserService baseUserService;

    @Mock
    private CoffeeUserRepository coffeeUserRepository;

    @InjectMocks
    private ProfileService profileService;

    private CoffeeUser user;

    @BeforeEach
    void setUp() {
        user = new CoffeeUser();
        user.setId(1L);
        user.setAuthId(100L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setName("Mario");
        user.setLastname("Rossi");
        user.setAvatarKey("default");
    }

    @Test
    void getProfile_returnsDefaultsWhenNull() {
        user.setAvatarKey(null);
        when(baseUserService.findUserByAuthId(100L)).thenReturn(user);

        UserProfileDto result = profileService.getProfile(100L);

        assertEquals("default", result.getAvatarKey());
    }

    @Test
    void updateProfile_validAvatar() {
        when(baseUserService.findUserByAuthId(100L)).thenReturn(user);
        when(coffeeUserRepository.save(any(CoffeeUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileRequest request = new UserProfileRequest();
        request.setAvatarKey("cup");

        UserProfileDto result = profileService.updateProfile(100L, request);

        assertEquals("cup", result.getAvatarKey());
    }

    @Test
    void updateProfile_invalidAvatar_throwsValidationException() {
        when(baseUserService.findUserByAuthId(100L)).thenReturn(user);

        UserProfileRequest request = new UserProfileRequest();
        request.setAvatarKey("invalid");

        assertThrows(ValidationException.class, () -> profileService.updateProfile(100L, request));
    }

    @Test
    void updateProfile_sanitizesPaymentLinks() {
        when(baseUserService.findUserByAuthId(100L)).thenReturn(user);
        when(coffeeUserRepository.save(any(CoffeeUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileRequest request = new UserProfileRequest();
        request.setSatispayLink("satispay.com/user");

        UserProfileDto result = profileService.updateProfile(100L, request);

        assertEquals("https://satispay.com/user", result.getSatispayLink());
    }
}