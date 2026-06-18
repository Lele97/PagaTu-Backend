package com.pagatu.auth.service;

import com.pagatu.auth.dto.LoginResponse;
import com.pagatu.auth.dto.OAuthTokenRequest;
import com.pagatu.auth.entity.AuthProvider;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.exception.AuthenticationException;
import com.pagatu.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class OAuthService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final WebClient webClient;

    public OAuthService(UserRepository userRepository, AuthService authService, WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.webClient = webClientBuilder.build();
    }

    @Transactional
    public LoginResponse loginWithGoogle(OAuthTokenRequest request) {
        Map<?, ?> tokenInfo = webClient.get()
                .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", request.getToken())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenInfo == null || tokenInfo.get("email") == null) {
            throw new AuthenticationException("Token Google non valido");
        }

        String email = tokenInfo.get("email").toString();
        String providerId = tokenInfo.get("sub").toString();
        String name = tokenInfo.containsKey("given_name") ? tokenInfo.get("given_name").toString() : null;
        String lastName = tokenInfo.containsKey("family_name") ? tokenInfo.get("family_name").toString() : null;

        return authenticateOAuthUser(email, providerId, AuthProvider.GOOGLE, name, lastName);
    }

    @Transactional
    public LoginResponse loginWithMicrosoft(OAuthTokenRequest request) {
        Map<?, ?> profile = webClient.get()
                .uri("https://graph.microsoft.com/v1.0/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.getToken())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (profile == null || profile.get("mail") == null && profile.get("userPrincipalName") == null) {
            throw new AuthenticationException("Token Microsoft non valido");
        }

        String email = profile.get("mail") != null
                ? profile.get("mail").toString()
                : profile.get("userPrincipalName").toString();
        String providerId = profile.get("id").toString();
        String name = profile.get("givenName") != null ? profile.get("givenName").toString() : null;
        String lastName = profile.get("surname") != null ? profile.get("surname").toString() : null;

        return authenticateOAuthUser(email, providerId, AuthProvider.MICROSOFT, name, lastName);
    }

    private LoginResponse authenticateOAuthUser(String email, String providerId, AuthProvider provider,
                                                String firstName, String lastName) {
        Optional<User> byProvider = userRepository.findByProviderIdAndAuthProvider(providerId, provider);
        if (byProvider.isPresent()) {
            return authService.buildLoginResponse(byProvider.get());
        }

        Optional<User> byEmail = userRepository.getByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setAuthProvider(provider);
            existing.setProviderId(providerId);
            existing.setEmailVerified(true);
            userRepository.save(existing);
            return authService.buildLoginResponse(existing);
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(generateUsername(email));
        newUser.setAuthProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setEmailVerified(true);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));

        User saved = userRepository.save(newUser);
        authService.syncWithCoffeeService(saved);
        log.info("Nuovo utente OAuth {} creato per {}", provider, email);
        return authService.buildLoginResponse(saved);
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        if (base.length() < 6) {
            base = base + "user";
        }
        String candidate = base.substring(0, Math.min(base.length(), 15));
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base.substring(0, Math.min(base.length(), 12)) + suffix++;
        }
        return candidate;
    }
}