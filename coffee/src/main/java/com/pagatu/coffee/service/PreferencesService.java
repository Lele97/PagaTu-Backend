package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.UserPreferencesDto;
import com.pagatu.coffee.dto.UserPreferencesRequest;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.exception.ValidationException;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreferencesService {

    private final BaseUserService baseUserService;
    private final CoffeeUserRepository coffeeUserRepository;

    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferences(Long userId) {
        return toDto(baseUserService.findUserByAuthId(userId));
    }

    @Transactional
    public UserPreferencesDto updatePreferences(Long userId, UserPreferencesRequest request) {
        if (request.getEmailTurnReminders() == null) {
            throw new ValidationException("At least one field must be provided");
        }

        CoffeeUser user = baseUserService.findUserByAuthId(userId);

        if (request.getEmailTurnReminders() != null) {
            user.setEmailTurnReminders(request.getEmailTurnReminders());
        }

        CoffeeUser saved = coffeeUserRepository.save(user);
        return toDto(saved);
    }

    private UserPreferencesDto toDto(CoffeeUser user) {
        return new UserPreferencesDto(
                user.getEmailTurnReminders() != null ? user.getEmailTurnReminders() : true);
    }
}