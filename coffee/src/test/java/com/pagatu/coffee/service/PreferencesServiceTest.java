package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UserPreferencesDto;
import com.pagatu.coffee.dto.UserPreferencesRequest;
import com.pagatu.coffee.entity.CoffeeUser;
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
class PreferencesServiceTest {

    @Mock
    private BaseUserService baseUserService;

    @Mock
    private CoffeeUserRepository coffeeUserRepository;

    @InjectMocks
    private PreferencesService preferencesService;

    private CoffeeUser user;

    @BeforeEach
    void setUp() {
        user = new CoffeeUser();
        user.setAuthId(100L);
        user.setEmailTurnReminders(true);
    }

    @Test
    void updatePreferences_disablesEmailReminders() {
        when(baseUserService.findUserByAuthId(100L)).thenReturn(user);
        when(coffeeUserRepository.save(any(CoffeeUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserPreferencesRequest request = new UserPreferencesRequest();
        request.setEmailTurnReminders(false);

        UserPreferencesDto result = preferencesService.updatePreferences(100L, request);

        assertFalse(result.getEmailTurnReminders());
    }

    @Test
    void getPreferences_defaultsWhenNull() {
        user.setEmailTurnReminders(null);
        when(baseUserService.findUserByAuthId(100L)).thenReturn(user);

        UserPreferencesDto result = preferencesService.getPreferences(100L);

        assertTrue(result.getEmailTurnReminders());
    }
}