package com.pagatu.auth.service;

import com.pagatu.auth.dto.LoginRequest;
import com.pagatu.auth.dto.LoginResponse;
import com.pagatu.auth.dto.RegisterRequest;
import com.pagatu.auth.dto.UtenteDto;
import com.pagatu.auth.entity.EventType;
import com.pagatu.auth.entity.TokenForUserPasswordReset;
import com.pagatu.auth.entity.TokenStatus;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.ResetPasswordMailEvent;
import com.pagatu.auth.event.UserEvent;
import com.pagatu.auth.repository.FirstUserRepository;
import com.pagatu.auth.repository.TokenForUserPasswordResetRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    @Value("${spring.kafka.topics.user-service}")
    private String USER_TOPIC;

    @Value("${spring.kafka.topics.resetPasswordMail}")
    private String RESET_PASSWORD_TOPIC;


    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final int LIMITER = 10;

    private final FirstUserRepository fristUserRepository;
    private final TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebClient.Builder webClientBuilder;

    @Qualifier("userEventKafkaTemplate")
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Qualifier("resetPasswordMailKafkaTemplate")
    private final KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail;

    public AuthService(FirstUserRepository fristUserRepository, TokenForUserPasswordResetRepository tokenForUserPasswordResetRepository, PasswordEncoder passwordEncoder, WebClient.Builder webClientBuilder, KafkaTemplate<String, UserEvent> kafkaTemplate, KafkaTemplate<String, ResetPasswordMailEvent> kafkaTemplateResetPasswordMail) {
        this.fristUserRepository = fristUserRepository;
        this.tokenForUserPasswordResetRepository = tokenForUserPasswordResetRepository;
        this.passwordEncoder = passwordEncoder;
        this.webClientBuilder = webClientBuilder;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplateResetPasswordMail = kafkaTemplateResetPasswordMail;
    }

    public LoginResponse login(LoginRequest loginRequest) {

        Optional<User> userOpt = fristUserRepository.findByUsername(loginRequest.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();
        String token = generateToken(user);

        return new LoginResponse(token, user.getUsername(), user.getEmail());
    }

    @Transactional("firstTransactionManager")
    public User register(RegisterRequest registerRequest) {

        if (fristUserRepository.existsByUsername(registerRequest.getUsername()))
            throw new RuntimeException("Username already exists");

        if (fristUserRepository.existsByEmail(registerRequest.getEmail()))
            throw new RuntimeException("Email already exists");

        // Crea l'utente con i gruppi come List<String>
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setGroups(registerRequest.getGroups());

        User savedUser = fristUserRepository.save(user);

        // Pubblica evento Kafka
        UserEvent userEvent = UserEvent.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .password(savedUser.getPassword())
                .groups(savedUser.getGroups())
                .eventType(EventType.CREATE)
                .build();

        kafkaTemplate.send(USER_TOPIC, String.valueOf(savedUser.getId()), userEvent);

        // Crea DTO per il servizio coffee
        UtenteDto utenteDto = new UtenteDto();
        utenteDto.setId(savedUser.getId());
        utenteDto.setAuthId(savedUser.getId());
        utenteDto.setUsername(savedUser.getUsername());
        utenteDto.setEmail(savedUser.getEmail());
        utenteDto.setName(savedUser.getFirstName());
        utenteDto.setLastname(savedUser.getLastName());
        utenteDto.setGroups(savedUser.getGroups());

        log.info("ID :: {}", utenteDto.getId());

        // Make the WebClient call with enhanced error handling
        webClientBuilder.build()
                .post()
                .uri("http://localhost:8082/api/coffee/user")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(utenteDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    String errorMsg = String.format(
                                            "Coffee service error - Status: %s, Body: %s",
                                            response.statusCode(),
                                            errorBody
                                    );
                                    log.error(errorMsg);
                                    return Mono.error(new RuntimeException(errorMsg));
                                })
                )
                .bodyToMono(Void.class)
                .doOnSuccess(x -> log.info("User successfully synchronized with coffee service"))
                .doOnError(error -> log.error("Error synchronizing with coffee service", error))
                .subscribe();

        return savedUser;
    }

    public void sendEmailForResetPassword(String email) {

        if (fristUserRepository.existsByEmail(email)) {

            // CHECK LIMIT GIORNALIERO DI RESET PASSWORD
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            if (tokenForUserPasswordResetRepository.countRecnetTokensByEmail(email, twentyFourHoursAgo) >= LIMITER) {
                log.warn("Daily password reset limit exceeded for user {}", email);
                throw new RuntimeException("You have exceeded the daily limit for password reset requests.");
            }

            TokenForUserPasswordReset tokenForUserPasswordReset = createTokenForUserPasswordReset(email);

            TokenForUserPasswordReset saved = tokenForUserPasswordResetRepository.save(tokenForUserPasswordReset);

            log.info("Saved token with ID: {}", saved.getId());

            ResetPasswordMailEvent event = new ResetPasswordMailEvent();
            event.setEmail(email);
            event.setToken(tokenForUserPasswordReset.getToken());

            kafkaTemplateResetPasswordMail.send(RESET_PASSWORD_TOPIC, event);

            log.info("Reset password event sent for user {}", email);

        } else {
            log.warn("Reset password event not sent for user {}", email);
            throw new RuntimeException("Unable sent Reset password event");
        }
    }

    public void resetPassword() {
    }

    private TokenForUserPasswordReset createTokenForUserPasswordReset(String email) {
        String token_header = "Paga_Tu_";
        String tokenTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_";
        String token_body = UUID.randomUUID().toString();
        String token_final = "_Reset_Token_";
        String token = token_header + tokenTimestamp + token_body + token_final;
        TokenForUserPasswordReset tokenForUserPasswordReset = new TokenForUserPasswordReset();
        tokenForUserPasswordReset.setToken(token);
        tokenForUserPasswordReset.setEmail(email);
        tokenForUserPasswordReset.setCreatedAt(LocalDateTime.now());
        //Token valido da un ora a partire dalla sua creazione
        tokenForUserPasswordReset.setExpiredDate(LocalDateTime.now().plusMinutes(30));
        tokenForUserPasswordReset.setTokenStatus(TokenStatus.ACTIVE);
        log.info("Creato token attivo fino a {}", LocalDateTime.now().plusMinutes(30));
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
}