package com.pagatu.auth.service;

import com.pagatu.auth.dto.*;
import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.ResetPasswordMailEvent;
import com.pagatu.auth.exception.*;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import com.pagatu.auth.repository.UserRepository;
import com.pagatu.auth.util.Constants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Service class for handling authentication and user management operations.
 * Provides functionality for user registration, login, password reset, and token management.
 * Integrates with Kafka for event publishing and external services for user synchronization.
 */
@Service
@Slf4j
public class AuthService {

    @Value("${spring.kafka.topics.resetPasswordMail}")
    private String resetPasswordTopic;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${caffe.service.url}")
    private String caffeServiceUrl;


    private static final int LIMITER = 10;
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private final PasswordEncoder passwordEncoder;
    private final WebClient webClient;
    private final UserRepository userRepository;
    private final TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository;
    private final KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail;

    public AuthService(@Autowired(required = false) TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       WebClient.Builder webClientBuilder,
                       @Autowired(required = false) @Qualifier("resetPasswordMailKafkaTemplate") KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail) {
        this.passwordEncoder = passwordEncoder;
        this.webClient = webClientBuilder.baseUrl(caffeServiceUrl).build();
        this.tokenForUserPasswordResetRepository = tokenForUserPasswordResetRepository;
        this.userRepository = userRepository;
        this.kafkaTemplateResetPasswordMail = kafkaTemplateResetPasswordMail;
    }

    /**
     * Authenticates a user and generates a JWT token upon successful authentication.
     *
     * @param loginRequest the login credentials containing username and password
     * @return LoginResponse containing JWT token and user information
     * @throws UserNotFoundException if the user with the provided username doesn't exist
     * @throws AuthenticationException if the provided password is incorrect
     */
    public LoginResponse login(LoginRequest loginRequest) {

        log.debug("Attempting login for user: {}", loginRequest.getUsername());

        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());

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

        log.info("Successful login for user: {}", user.getUsername());

        return new LoginResponse(token, user.getUsername(), user.getEmail());
    }

    /**
     * Registers a new user in the system.
     * Validates uniqueness of username and email before creating the user.
     * Synchronizes the user with external services after successful registration.
     *
     * @param registerRequest the user registration information
     * @throws UserAlreadyExistsException  if username or email already exists in the system
     * @throws ServiceUnavailableException if synchronization with external services fails
     */
    @Transactional
    public void register(RegisterRequest registerRequest) {

        log.debug("Starting registration for user: {}", registerRequest.getUsername());

        boolean usernameExists = userRepository.existsByUsername(registerRequest.getUsername());

        if (usernameExists) {
            throw new UserAlreadyExistsException("Username already exists", "username", registerRequest.getUsername());
        }

        boolean emailExists = userRepository.existsByEmail(registerRequest.getEmail());

        if (emailExists) {
            throw new UserAlreadyExistsException("Email already exists", Constants.EMAIL_EXCEPRION_VALUE, registerRequest.getEmail());
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setDateOfBirth(registerRequest.getDateOfBirth());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());

        User savedUser = userRepository.save(user);

        log.info("User saved to database: {}", savedUser.getUsername());
        try {
            syncWithCoffeeService(savedUser);
        } catch (Exception e) {
            log.error("Failed to sync with coffee service", e);
            throw new ServiceUnavailableException("Failed to sync with coffee service", e);
        }

    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address of the user to retrieve
     * @return the User entity associated with the email
     * @throws UserNotFoundException if no user exists with the provided email
     */
    public User getUserByEmail(String email) {

        Optional<User> userOpt = userRepository.getByEmail(email);

        return userOpt.orElseThrow(() ->
                new UserNotFoundException("User not found", email, Constants.EMAIL_EXCEPRION_VALUE));
    }

    /**
     * Validates a password reset token and returns the associated email if valid.
     *
     * @param token the reset token to validate
     * @return the email address associated with the valid token
     * @throws InvalidTokenException if the token is invalid, null, or already used
     * @throws TokenExpiredException if the token has expired
     */
    public String validateResetTokenAndGetEmail(String token) {

        try {

            if (token == null || token.trim().isEmpty()) {
                log.warn("Empty or null token provided");
                throw new InvalidTokenException("Token is required", "RESET_TOKEN");
            }

            Optional<TokenForUserPasswordReset> tokenOpt = tokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken(token);

            if (tokenOpt.isEmpty()) {
                log.warn("Token not found: {}", token);
                throw new InvalidTokenException("Token not found", "RESET_TOKEN");
            }

            TokenForUserPasswordReset resetToken = tokenOpt.get();

            if (resetToken.getTokenStatus() == TokenStatus.USED) {
                log.warn("Token already used: {}", token);
                throw new InvalidTokenException("Token has already been used", "RESET_TOKEN");
            }

            if (resetToken.getExpiredDate().isBefore(LocalDateTime.now())) {
                log.warn("Token has expired: {}", token);
                resetToken.setTokenStatus(TokenStatus.EXPIRED);
                tokenForUserPasswordResetRepository.save(resetToken);
                throw new TokenExpiredException("Token has expired", "RESET_TOKEN");
            }

            log.info("Token validated successfully for email: {}", resetToken.getEmail());
            return resetToken.getEmail();

        } catch (TokenExpiredException | InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating reset token", e);
            throw new InvalidTokenException("Error validating reset token", e);
        }
    }

    /**
     * Initiates the password reset process for a user.
     * Validates the email, checks rate limits, creates a reset token, and publishes a reset event.
     *
     * @param email the email address of the user requesting password reset
     * @throws UserNotFoundException if no user exists with the provided email
     * @throws RateLimiterException if the daily password reset limit has been exceeded
     */
    public void sendEmailForResetPassword(String email) {
        log.debug("Processing password reset request for email: {}", email);

        boolean emailExists = userRepository.existsByEmail(email);

        if (!emailExists) {
            log.warn("Password reset requested for non-existent email: {}", email);
            throw new UserNotFoundException("Email not found", email, Constants.EMAIL_EXCEPRION_VALUE);
        }

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long recentTokenCount = tokenForUserPasswordResetRepository.countRecnetTokensByEmail(email, twentyFourHoursAgo);

        if (recentTokenCount >= LIMITER) {
            log.warn("Daily password reset limit exceeded for user: {}", email);
            throw new RateLimiterException("You have exceeded the daily limit for password reset requests.",
                    3600, email);
        }

        TokenForUserPasswordReset tokenForUserPasswordReset = createTokenForUserPasswordReset(email);
        TokenForUserPasswordReset savedToken = tokenForUserPasswordResetRepository.save(tokenForUserPasswordReset);

        publishResetPasswordMailEvent(email, savedToken.getToken());

        log.info("Password reset process initiated for email: {}", email);
    }

    /**
     * Resets a user's password using a valid reset token.
     * Validates the request, updates the password, and marks the token as used.
     *
     * @param resetPasswordRequest the new password information
     * @param token the reset token obtained from the email link
     * @throws ValidationException if the request contains invalid data
     * @throws UserNotFoundException if no user exists with the provided email
     * @throws InvalidTokenException if the token is invalid or doesn't match the email
     * @throws TokenExpiredException if the token has expired
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest, String token) {
        log.debug("Processing password reset for email: {}",
                resetPasswordRequest.getEmail());

        validateResetPasswordRequest(resetPasswordRequest, token);

        Optional<User> userOpt = userRepository.getByEmail(resetPasswordRequest.getEmail());

        User user = userOpt.orElseThrow(() ->
                new UserNotFoundException("User not found", resetPasswordRequest.getEmail(), Constants.EMAIL_EXCEPRION_VALUE));

        Optional<TokenForUserPasswordReset> tokenOpt = tokenForUserPasswordResetRepository.findTokenForUserPasswordResetByToken(token);

        TokenForUserPasswordReset resetToken = tokenOpt.orElseThrow(() ->
                new InvalidTokenException("Token not found", "RESET_TOKEN"));

        validateResetToken(resetToken, resetPasswordRequest.getEmail());

        String encodedPassword = passwordEncoder.encode(resetPasswordRequest.getPassword());
        user.setPassword(encodedPassword);

        userRepository.save(user);

        resetToken.setTokenStatus(TokenStatus.USED);
        resetToken.setUsedAt(LocalDateTime.now());

        tokenForUserPasswordResetRepository.save(resetToken);

        log.info("Password successfully reset for user: {}",
                resetPasswordRequest.getEmail());
    }

    /**
     * Validates the password reset request parameters.
     *
     * @param request the password reset request to validate
     * @param token the reset token to validate
     * @throws ValidationException if any required field is missing or empty
     */
    private void validateResetPasswordRequest(ResetPasswordRequest request, String token) {
        Map<String, List<String>> fieldErrors = new HashMap<>();

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            fieldErrors.put("password", List.of("Password cannot be empty"));
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            fieldErrors.put(Constants.EMAIL_EXCEPRION_VALUE, List.of("Email cannot be empty"));
        }
        if (token == null || token.trim().isEmpty()) {
            fieldErrors.put("token", List.of("Token cannot be empty"));
        }

        if (!fieldErrors.isEmpty()) {
            throw new ValidationException("Validation failed for password reset request", fieldErrors);
        }
    }

    /**
     * Validates a reset token for a specific email address.
     *
     * @param resetToken the token to validate
     * @param email the email address to validate against the token
     * @throws InvalidTokenException if the token doesn't match the email or has been used
     * @throws TokenExpiredException if the token has expired
     */
    private void validateResetToken(TokenForUserPasswordReset resetToken, String email) {
        if (!resetToken.getEmail().equals(email)) {
            log.warn("Token email mismatch. Token email: {}, Request email: {}",
                    resetToken.getEmail(), email);
            throw new InvalidTokenException("Invalid token for this email", "RESET_TOKEN");
        }

        if (resetToken.getTokenStatus() == TokenStatus.USED) {
            log.warn("Token already used: {}", resetToken.getToken());
            throw new InvalidTokenException("Token has already been used", "RESET_TOKEN");
        }

        if (resetToken.getExpiredDate().isBefore(LocalDateTime.now())) {
            log.warn("Token has expired for email: {}", email);
            resetToken.setTokenStatus(TokenStatus.EXPIRED);

            tokenForUserPasswordResetRepository.save(resetToken);
            throw new TokenExpiredException("Token has expired", "RESET_TOKEN");

        }
    }

    /**
     * Creates a new password reset token for a user.
     *
     * @param email the email address of the user requesting password reset
     * @return the created TokenForUserPasswordReset entity
     */
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

        log.info("Created token active until: {}",
                tokenForUserPasswordReset.getExpiredDate());
        return tokenForUserPasswordReset;
    }

    /**
     * Generates a JWT token for an authenticated user.
     *
     * @param user the user entity for which to generate the token
     * @return the generated JWT token as a string
     */
    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim(Constants.EMAIL_EXCEPRION_VALUE, user.getEmail())
                .claim("id", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    /**
     * Publishes a password reset mail event to Kafka.
     *
     * @param email the email address of the user requesting password reset
     * @param token the reset token to include in the event
     */
    private void publishResetPasswordMailEvent(String email, String token) {
        try {
            ResetPasswordMailEvent event = new ResetPasswordMailEvent();
            event.setEmail(email);
            event.setToken(token);

            kafkaTemplateResetPasswordMail.send(resetPasswordTopic, event);
            log.debug("Published reset password mail event for email: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish reset password mail event", e);
        }
    }

    /**
     * Synchronizes user information with an external coffee service.
     *
     * @param user the user entity to synchronize
     */
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