package com.pagatu.auth.service;

import com.pagatu.auth.dto.*;
import com.pagatu.auth.entity.EventType;
import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.ResetPasswordMailEvent;
import com.pagatu.auth.event.TokenForgotPswUserEvent;
import com.pagatu.auth.event.UserEvent;
import com.pagatu.auth.exception.*;
import com.pagatu.auth.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class ProfileAwareAuthService {

    @Value("${spring.kafka.topics.user-service}")
    private String USER_TOPIC;

    @Value("${spring.kafka.topics.resetPasswordMail}")
    private String RESET_PASSWORD_TOPIC;

    @Value("${spring.kafka.topics.user-service-reset-token}")
    private String TOKEN_FORGOT_USER_PSW;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${caffe.service.url}")
    private String caffeServiceUrl;

    private static final int LIMITER = 10;
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final Environment environment;
    private final PasswordEncoder passwordEncoder;
    private final WebClient webClient;

    // Production repositories (for test profile) - now optional
    private final FirstUserRepository firstUserRepository;
    private final TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository;

    // Dev repositories (for dev profile)
    private final DevUserRepository devUserRepository;
    private final DevTokenForUserPasswordResetRepository devTokenForUserPasswordResetRepository;

    // Kafka templates (only used in non-dev profiles) - now optional
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail;
    private final KafkaTemplate<String, TokenForgotPswUserEvent> kafkaTemplateTokenForUserPasswordReset;

    public ProfileAwareAuthService(
            Environment environment,
            @Autowired(required = false) FirstUserRepository firstUserRepository,
            @Autowired(required = false) TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository,
            @Autowired(required = false) DevUserRepository devUserRepository,
            @Autowired(required = false) DevTokenForUserPasswordResetRepository devTokenForUserPasswordResetRepository,
            PasswordEncoder passwordEncoder,
            WebClient.Builder webClientBuilder,
            @Autowired(required = false) @Qualifier("userEventKafkaTemplate") KafkaTemplate<String, UserEvent> kafkaTemplate,
            @Autowired(required = false) @Qualifier("resetPasswordMailKafkaTemplate") KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail,
            @Autowired(required = false) @Qualifier("tokenForgotUserPasswordTemplate") KafkaTemplate<String, TokenForgotPswUserEvent> kafkaTemplateTokenForUserPasswordReset) {

        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();

        // Assign all repositories (they'll be null if not available for the profile)
        this.firstUserRepository = firstUserRepository;
        this.tokenForUserPasswordResetRepository = tokenForUserPasswordResetRepository;
        this.devUserRepository = devUserRepository;
        this.devTokenForUserPasswordResetRepository = devTokenForUserPasswordResetRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplateResetPasswordMail = kafkaTemplateResetPasswordMail;
        this.kafkaTemplateTokenForUserPasswordReset = kafkaTemplateTokenForUserPasswordReset;

        log.info("Initialized ProfileAwareAuthService with profile: {}", getActiveProfile());

        // Validate that we have the required repositories for the active profile
        if (isDevProfile()) {
            if (devUserRepository == null || devTokenForUserPasswordResetRepository == null) {
                throw new IllegalStateException("Dev profile requires dev repositories but they were not found");
            }
            log.info("Using dev repositories for dev profile");
        } else {
            if (firstUserRepository == null || tokenForUserPasswordResetRepository == null) {
                throw new IllegalStateException("Test profile requires first repositories but they were not found");
            }
            log.info("Using first repositories for test profile");
        }
    }

    public LoginResponse login(LoginRequest loginRequest) {
        log.debug("Attempting login for user: {} (Profile: {})", loginRequest.getUsername(), getActiveProfile());

        Optional<User> userOpt = isDevProfile()
                ? devUserRepository.findByUsername(loginRequest.getUsername())
                : firstUserRepository.findByUsername(loginRequest.getUsername());

        if (userOpt.isEmpty()) {
            log.warn("Failed login attempt - user not found: {}", loginRequest.getUsername());
            throw new UserNotFoundException("User not found", loginRequest.getUsername(), "username");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            log.warn("Failed login attempt - invalid password for user: {}", loginRequest.getUsername());
            throw new AuthenticationException("Invalid username or password");
        }

        User user = userOpt.get();
        String token = generateToken(user);

        log.info("Successful login for user: {} (Profile: {})", user.getUsername(), getActiveProfile());
        return new LoginResponse(token, user.getUsername(), user.getEmail());
    }

    @Transactional
    public User register(RegisterRequest registerRequest) {
        log.debug("Starting registration for user: {} (Profile: {})", registerRequest.getUsername(), getActiveProfile());

        // Validate unique constraints using profile-specific repositories
        boolean usernameExists = isDevProfile()
                ? devUserRepository.existsByUsername(registerRequest.getUsername())
                : firstUserRepository.existsByUsername(registerRequest.getUsername());

        if (usernameExists) {
            throw new UserAlreadyExistsException("Username already exists", "username", registerRequest.getUsername());
        }

        boolean emailExists = isDevProfile()
                ? devUserRepository.existsByEmail(registerRequest.getEmail())
                : firstUserRepository.existsByEmail(registerRequest.getEmail());

        if (emailExists) {
            throw new UserAlreadyExistsException("Email already exists", "email", registerRequest.getEmail());
        }

        // Create user entity
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setDateOfBirth(registerRequest.getDateOfBirth());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setGroups(registerRequest.getGroups());

        // Save user using appropriate repository
        User savedUser = isDevProfile()
                ? devUserRepository.save(user)
                : firstUserRepository.save(user);

        log.info("User saved to database: {} (Profile: {})", savedUser.getUsername(), getActiveProfile());


        // Publish Kafka event only for non-dev profiles
        if (!isDevProfile())
            publishUserEvent(savedUser, EventType.CREATE);
        else
            log.debug("Skipping Kafka events for dev profile");


        try {
            syncWithCoffeeService(savedUser);
        } catch (Exception e) {
            log.error("Failed to sync with coffee service", e);
            throw new ServiceUnavailableException("Failed to sync with coffee service", e);
        }

        return savedUser;
    }

    public String validateResetTokenAndGetEmail(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Empty or null token provided");
                throw new InvalidTokenException("Token is required", "RESET_TOKEN");
            }

            Optional<TokenForUserPasswordReset> tokenOpt = isDevProfile()
                    ? devTokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken(token)
                    : tokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken(token);

            if (tokenOpt.isEmpty()) {
                log.warn("Token not found: {} (Profile: {})", token, getActiveProfile());
                throw new InvalidTokenException("Token not found", "RESET_TOKEN");
            }

            TokenForUserPasswordReset resetToken = tokenOpt.get();

            // Check if token is already used
            if (resetToken.getTokenStatus() == TokenStatus.USED) {
                log.warn("Token already used: {} (Profile: {})", token, getActiveProfile());
                throw new InvalidTokenException("Token has already been used", "RESET_TOKEN");
            }

            // Check if token is expired
            if (resetToken.getExpiredDate().isBefore(LocalDateTime.now())) {
                log.warn("Token has expired: {} (Profile: {})", token, getActiveProfile());
                // Automatically mark expired tokens using appropriate repository
                resetToken.setTokenStatus(TokenStatus.EXPIRED);
                if (isDevProfile()) {
                    devTokenForUserPasswordResetRepository.save(resetToken);
                } else {
                    tokenForUserPasswordResetRepository.save(resetToken);
                }
                throw new TokenExpiredException("Token has expired", "RESET_TOKEN");
            }

            log.info("Token validated successfully for email: {} (Profile: {})", resetToken.getEmail(), getActiveProfile());
            return resetToken.getEmail();

        } catch (TokenExpiredException | InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating reset token (Profile: {})", getActiveProfile(), e);
            throw new InvalidTokenException("Error validating reset token", e);
        }
    }

    public void sendEmailForResetPassword(String email) {
        log.debug("Processing password reset request for email: {} (Profile: {})", email, getActiveProfile());

        // Check if email exists using profile-specific repository
        boolean emailExists = isDevProfile()
                ? devUserRepository.existsByEmail(email)
                : firstUserRepository.existsByEmail(email);

        if (!emailExists) {
            log.warn("Password reset requested for non-existent email: {} (Profile: {})", email, getActiveProfile());
            throw new UserNotFoundException("Email not found", email, "email");
        }

        // Check daily limit using appropriate repository
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long recentTokenCount = isDevProfile()
                ? devTokenForUserPasswordResetRepository.countRecnetTokensByEmail(email, twentyFourHoursAgo)
                : tokenForUserPasswordResetRepository.countRecnetTokensByEmail(email, twentyFourHoursAgo);

        if (recentTokenCount >= LIMITER) {
            log.warn("Daily password reset limit exceeded for user: {} (Profile: {})", email, getActiveProfile());
            throw new RateLimiterException("You have exceeded the daily limit for password reset requests.",
                    3600, email);
        }

        // Create and save token
        TokenForUserPasswordReset tokenForUserPasswordReset = createTokenForUserPasswordReset(email);
        TokenForUserPasswordReset savedToken = isDevProfile()
                ? devTokenForUserPasswordResetRepository.save(tokenForUserPasswordReset)
                : tokenForUserPasswordResetRepository.save(tokenForUserPasswordReset);

        // Publish Kafka events only for non-dev profiles
        if (!isDevProfile())
            publishTokenEvent(savedToken, EventType.CREATE);
        else
            log.debug("Skipping Kafka events for dev profile");


        publishResetPasswordMailEvent(email, savedToken.getToken());

        log.info("Password reset process initiated for email: {} (Profile: {})", email,

                getActiveProfile());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest, String token) {
        log.debug("Processing password reset for email: {} (Profile: {})",
                resetPasswordRequest.getEmail(), getActiveProfile());

        // Validate inputs
        validateResetPasswordRequest(resetPasswordRequest, token);

        // Get user using appropriate repository
        Optional<User> userOpt = isDevProfile()
                ? devUserRepository.getByEmail(resetPasswordRequest.getEmail())
                : firstUserRepository.getByEmail(resetPasswordRequest.getEmail());

        User user = userOpt.orElseThrow(() ->
                new UserNotFoundException("User not found", resetPasswordRequest.getEmail(), "email"));

        // Get token using appropriate repository
        Optional<TokenForUserPasswordReset> tokenOpt = isDevProfile()
                ? devTokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken(token)
                : tokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken(token);

        TokenForUserPasswordReset resetToken = tokenOpt.orElseThrow(() ->
                new InvalidTokenException("Token not found", "RESET_TOKEN"));

        // Validate token
        validateResetToken(resetToken, resetPasswordRequest.getEmail());

        // Update password
        String encodedPassword = passwordEncoder.encode(resetPasswordRequest.getPassword());
        user.setPassword(encodedPassword);

        User updatedUser = isDevProfile()
                ? devUserRepository.save(user)
                : firstUserRepository.save(user);

        // Mark token as used
        resetToken.setTokenStatus(TokenStatus.USED);
        resetToken.setUsedAt(LocalDateTime.now());

        if (isDevProfile()) {
            devTokenForUserPasswordResetRepository.save(resetToken);
        } else {
            tokenForUserPasswordResetRepository.save(resetToken);
        }

        // Publish Kafka events only for non-dev profiles
        if (!isDevProfile()) {
            publishUserEvent(updatedUser, EventType.UPDATE);
            publishTokenEvent(resetToken, EventType.UPDATE);
        } else {
            log.debug("Skipping Kafka events for dev profile");
        }

        log.info("Password successfully reset for user: {} (Profile: {})",
                resetPasswordRequest.getEmail(), getActiveProfile());
    }

    // Profile detection methods
    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? String.join(",", profiles) : "default";
    }

    // Helper methods
    private void validateResetPasswordRequest(ResetPasswordRequest request, String token) {
        Map<String, List<String>> fieldErrors = new HashMap<>();

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            fieldErrors.put("password", List.of("Password cannot be empty"));
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            fieldErrors.put("email", List.of("Email cannot be empty"));
        }
        if (token == null || token.trim().isEmpty()) {
            fieldErrors.put("token", List.of("Token cannot be empty"));
        }

        if (!fieldErrors.isEmpty()) {
            throw new ValidationException("Validation failed for password reset request", fieldErrors);
        }
    }

    private void validateResetToken(TokenForUserPasswordReset resetToken, String email) {
        if (!resetToken.getEmail().equals(email)) {
            log.warn("Token email mismatch. Token email: {}, Request email: {} (Profile: {})",
                    resetToken.getEmail(), email, getActiveProfile());
            throw new InvalidTokenException("Invalid token for this email", "RESET_TOKEN");
        }

        if (resetToken.getTokenStatus() == TokenStatus.USED) {
            log.warn("Token already used: {} (Profile: {})", resetToken.getToken(), getActiveProfile());
            throw new InvalidTokenException("Token has already been used", "RESET_TOKEN");
        }

        if (resetToken.getExpiredDate().isBefore(LocalDateTime.now())) {
            log.warn("Token has expired for email: {} (Profile: {})", email, getActiveProfile());
            resetToken.setTokenStatus(TokenStatus.EXPIRED);
            if (isDevProfile()) {
                devTokenForUserPasswordResetRepository.save(resetToken);
            } else {
                tokenForUserPasswordResetRepository.save(resetToken);
            }
            throw new TokenExpiredException("Token has expired", "RESET_TOKEN");

        }
    }

    private TokenForUserPasswordReset createTokenForUserPasswordReset(String email) {
        String tokenHeader = "Paga_Tu_";
        String tokenTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_";
        String tokenBody = UUID.randomUUID().toString();
        String tokenFinal = "_Reset_Token_";
        String token = tokenHeader + tokenTimestamp + tokenBody + tokenFinal;

        TokenForUserPasswordReset tokenForUserPasswordReset = new TokenForUserPasswordReset();
        tokenForUserPasswordReset.setToken(token);
        tokenForUserPasswordReset.setEmail(email);
        tokenForUserPasswordReset.setCreatedAt(LocalDateTime.now());
        tokenForUserPasswordReset.setExpiredDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        tokenForUserPasswordReset.setTokenStatus(TokenStatus.ACTIVE);

        log.info("Created token active until: {} (Profile: {})",
                tokenForUserPasswordReset.getExpiredDate(), getActiveProfile());
        return tokenForUserPasswordReset;
    }

    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("email", user.getEmail())
                .claim("id", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    private void publishUserEvent(User user, EventType eventType) {
        try {
            UserEvent userEvent = UserEvent.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .password(user.getPassword())
                    .groups(user.getGroups())
                    .eventType(eventType)
                    .build();

            kafkaTemplate.send(USER_TOPIC, String.valueOf(user.getId()), userEvent);
            log.debug("Published user event: {} for user: {}", eventType, user.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish user event", e);
        }
    }

    private void publishTokenEvent(TokenForUserPasswordReset token, EventType eventType) {
        try {
            TokenForgotPswUserEvent tokenEvent = new TokenForgotPswUserEvent();
            tokenEvent.setId(token.getId());
            tokenEvent.setToken(token.getToken());
            tokenEvent.setEmail(token.getEmail());
            tokenEvent.setCreatedAt(token.getCreatedAt());
            tokenEvent.setTokenStatus(token.getTokenStatus());
            tokenEvent.setExpiredDate(token.getExpiredDate());
            tokenEvent.setUsedAt(token.getUsedAt());
            tokenEvent.setEventType(eventType);

            kafkaTemplateTokenForUserPasswordReset.send(TOKEN_FORGOT_USER_PSW, tokenEvent);
            log.debug("Published token event: {} for email: {}", eventType, token.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish token event", e);
        }
    }

    private void publishResetPasswordMailEvent(String email, String token) {
        try {
            ResetPasswordMailEvent event = new ResetPasswordMailEvent();
            event.setEmail(email);
            event.setToken(token);

            kafkaTemplateResetPasswordMail.send(RESET_PASSWORD_TOPIC, event);
            log.debug("Published reset password mail event for email: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish reset password mail event", e);
        }
    }

    private void syncWithCoffeeService(User user) {
        UtenteDto utenteDto = new UtenteDto();
        utenteDto.setId(user.getId());
        utenteDto.setAuthId(user.getId());
        utenteDto.setUsername(user.getUsername());
        utenteDto.setEmail(user.getEmail());
        utenteDto.setName(user.getFirstName());
        utenteDto.setLastname(user.getLastName());
        utenteDto.setGroups(user.getGroups());

        webClient.post()
                .uri("/api/coffee/user")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(utenteDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    String errorMsg = String.format(
                                            "Coffee service error - Status: %s, Body: %s",
                                            response.statusCode(), errorBody);
                                    log.error(errorMsg);
                                    return Mono.error(new RuntimeException(errorMsg));
                                }))
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(x -> log.info("User successfully synchronized with coffee service"))
                .doOnError(WebClientResponseException.class,
                        error -> log.error("Coffee service HTTP error: {}", error.getMessage()))
                .doOnError(Exception.class,
                        error -> log.error("Error synchronizing with coffee service", error))
                .onErrorResume(Exception.class, error -> {
                    log.warn("Coffee service sync failed, continuing with main operation");
                    return Mono.empty();
                })
                .subscribe();
    }
}
