package com.pagatu.auth.service;

import com.pagatu.auth.dto.LoginRequest;
import com.pagatu.auth.dto.LoginResponse;
import com.pagatu.auth.dto.RegisterRequest;
import com.pagatu.auth.dto.UtenteDto;
import com.pagatu.auth.entity.EventType;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.event.UserEvent;
import com.pagatu.auth.repository.FirstUserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class AuthService {

    @Value("${spring.kafka.topics.user-service}")
    private String USER_TOPIC;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final FirstUserRepository fristUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public AuthService(FirstUserRepository fristUserRepository, PasswordEncoder passwordEncoder, WebClient.Builder webClientBuilder, KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.fristUserRepository = fristUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.webClientBuilder = webClientBuilder;
        this.kafkaTemplate = kafkaTemplate;
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
        user.setGroups(registerRequest.getGroups()); // Deve essere List<String>

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
        utenteDto.setGroups(savedUser.getGroups()); // Invia List<String>

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