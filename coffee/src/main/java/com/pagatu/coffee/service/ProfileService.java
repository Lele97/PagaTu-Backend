package com.pagatu.coffee.service;

import com.pagatu.coffee.dto.PaymentLinksDto;
import com.pagatu.coffee.dto.PaymentLinksRequest;
import com.pagatu.coffee.entity.CoffeeUser;
import com.pagatu.coffee.repository.CoffeeUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final BaseUserService baseUserService;
    private final CoffeeUserRepository coffeeUserRepository;

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