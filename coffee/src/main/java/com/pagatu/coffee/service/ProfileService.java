package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.PaymentLinksDto;
import com.pagatu.coffee.dto.PaymentLinksRequest;
import com.pagatu.coffee.dto.UserProfileDto;
import com.pagatu.coffee.dto.UserProfileRequest;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.exception.ValidationException;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import com.pagatu.coffee.util.ProfileKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final BaseUserService baseUserService;
    private final CoffeeUserRepository coffeeUserRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        return toProfileDto(baseUserService.findUserByAuthId(userId));
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UserProfileRequest request) {
        if (request.getAvatarKey() == null && request.getThemeKey() == null
                && request.getSatispayLink() == null && request.getRevolutLink() == null) {
            throw new ValidationException("At least one field must be provided");
        }

        CoffeeUser user = baseUserService.findUserByAuthId(userId);

        if (request.getAvatarKey() != null) {
            if (!ProfileKeys.isValidAvatar(request.getAvatarKey())) {
                throw new ValidationException("Invalid avatar key: " + request.getAvatarKey());
            }
            user.setAvatarKey(request.getAvatarKey());
        }
        if (request.getThemeKey() != null) {
            if (!ProfileKeys.isValidTheme(request.getThemeKey())) {
                throw new ValidationException("Invalid theme key: " + request.getThemeKey());
            }
            user.setThemeKey(request.getThemeKey());
        }
        if (request.getSatispayLink() != null) {
            user.setSatispayLink(sanitizeLink(request.getSatispayLink()));
        }
        if (request.getRevolutLink() != null) {
            user.setRevolutLink(sanitizeLink(request.getRevolutLink()));
        }

        CoffeeUser saved = coffeeUserRepository.save(user);
        return toProfileDto(saved);
    }

    @Transactional(readOnly = true)
    public PaymentLinksDto getPaymentLinks(Long userId) {
        CoffeeUser user = baseUserService.findUserByAuthId(userId);
        return new PaymentLinksDto(user.getSatispayLink(), user.getRevolutLink());
    }

    @Transactional
    public PaymentLinksDto updatePaymentLinks(Long userId, PaymentLinksRequest request) {
        CoffeeUser user = baseUserService.findUserByAuthId(userId);
        user.setSatispayLink(sanitizeLink(request.getSatispayLink()));
        user.setRevolutLink(sanitizeLink(request.getRevolutLink()));
        CoffeeUser saved = coffeeUserRepository.save(user);
        return new PaymentLinksDto(saved.getSatispayLink(), saved.getRevolutLink());
    }

    private UserProfileDto toProfileDto(CoffeeUser user) {
        return new UserProfileDto(
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getLastname(),
                user.getAvatarKey() != null ? user.getAvatarKey() : ProfileKeys.DEFAULT_AVATAR,
                user.getThemeKey() != null ? user.getThemeKey() : ProfileKeys.DEFAULT_THEME,
                user.getSatispayLink(),
                user.getRevolutLink());
    }

    private String sanitizeLink(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        String trimmed = link.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }
}