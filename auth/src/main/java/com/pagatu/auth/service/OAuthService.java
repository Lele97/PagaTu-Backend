package com.pagatu.auth.service;

import com.pagatu.auth.config.DiscordProperties;
import com.pagatu.auth.config.GithubProperties;
import com.pagatu.auth.config.SlackProperties;
import com.pagatu.auth.dto.*;
import com.pagatu.auth.entity.AuthProvider;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.exception.AuthenticationException;
import com.pagatu.auth.exception.ServiceUnavailableException;
import com.pagatu.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service handling the OAuth2 logins (GitHub, Discord, Slack, Google, Microsoft).
 * <p>
 * GitHub, Discord, and Slack use the authorization code flow: the frontend sends
 * the code obtained from the provider, the backend exchanges it for an access
 * token and reads the user profile. Google and Microsoft instead receive a ready
 * token from the frontend and validate it against the provider APIs.
 * </p>
 */
@Service
@Slf4j
public class OAuthService {

    /**
     * Timeout applied to every outbound call to the providers.
     */
    private static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Lunghezza massima del corpo d'errore di GitHub riportato nel messaggio.
     */
    private static final int MAX_GITHUB_ERROR_DETAIL = 300;

    /**
     * Placeholder birth date for OAuth users, who are not asked for it.
     * The column is NOT NULL, so it must always be set.
     */
    private static final LocalDate DEFAULT_DATE_OF_BIRTH = LocalDate.of(1990, 1, 1);

    private final UserRepository userRepository;
    private final AuthService authService;
    private final WebClient webClient;
    private final GithubProperties githubProperties;
    private final DiscordProperties discordProperties;
    private final SlackProperties slackProperties;

    public OAuthService(UserRepository userRepository,
                        AuthService authService,
                        WebClient.Builder webClientBuilder,
                        GithubProperties githubProperties,
                        DiscordProperties discordProperties,
                        SlackProperties slackProperties) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.webClient = webClientBuilder.build();
        this.githubProperties = githubProperties;
        this.discordProperties = discordProperties;
        this.slackProperties = slackProperties;
    }


    //TODO oauth2 login with Discord
    public LoginResponse loginWithDiscord(@Valid DiscordRequest request) {


        if (!discordProperties.isConfigured()) {
            log.error("Login Discord non configurato: DISCORD_CLIENT_ID / DISCORD_CLIENT_SECRET mancanti");
            throw new ServiceUnavailableException("Login con Discord non configurato",
                    new IllegalStateException("discord.oauth.client-id / client-secret non configurati"));
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", discordProperties.getClientId());
        form.add("client_secret", discordProperties.getClientSecret());
        form.add("code", request.getCode().trim());
        form.add("grant_type", discordProperties.getGrantType());

        String redirectUri = resolveDiscordRedirectUri(request);
        if (redirectUri != null) {
            form.add("redirect_uri", redirectUri);
        }

        DiscordTokenResponse tokenResponse = webClient.post()
                .uri(discordProperties.getAccessTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(serviceUnavailableFor("Discord", response.statusCode(), body))))
                .bodyToMono(DiscordTokenResponse.class)
                .timeout(PROVIDER_TIMEOUT)
                .block();

        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new AuthenticationException("Codice Discord non valido: " + describeDiscordTokenError(tokenResponse));
        }

        log.debug("Access token Discord ottenuto (type: {}, scope: {})",
                tokenResponse.tokenType(), tokenResponse.scope());

        log.debug("Access Token: " + tokenResponse.accessToken());

        DiscordUser user = webClient.get()
                .uri(discordProperties.getUserUrl())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new AuthenticationException(
                                "Access token Discord rifiutato (HTTP "
                                        + response.statusCode().value() + "): " + body))))
                .bodyToMono(DiscordUser.class)
                .timeout(PROVIDER_TIMEOUT)
                .block();


        if (user == null || !StringUtils.hasText(user.id())) {
            throw new AuthenticationException("Profilo utente Discord non valido");
        }

        if (!StringUtils.hasText(user.email())) {
            throw new AuthenticationException(
                    "Discord non ha restituito l'email: verifica che il login richieda gli scope identify e email"
            );
        }

        String name = StringUtils.hasText(user.globalName())
                ? user.globalName()
                : user.username();

        log.debug("Login Discord completato per providerId={}", user.id());

        return authenticateOAuthUser(
                user.email(),
                user.id(),
                AuthProvider.DISCORD,
                name,
                ""
        );


    }


    /**
     * Completes the Slack login starting from the authorization code.
     * <ol>
     *     <li>exchanges the code for an access token;</li>
     *     <li>reads the Slack user identity;</li>
     *     <li>provisions or links the local user and returns the JWT.</li>
     * </ol>
     *
     * @param request the authorization code (and optional redirect uri)
     * @return the login response with the application JWT
     * @throws AuthenticationException     if the code is missing, invalid or
     *                                     the email cannot be resolved
     * @throws ServiceUnavailableException if Slack is unreachable or not configured
     */
    @Transactional
    public LoginResponse loginWithSlack(@Valid SlackRequest request) {

        if (!slackProperties.isConfigured()) {
            log.error("Login Slack non configurato: SLACK_CLIENT_ID / SLACK_CLIENT_SECRET mancanti");
            throw new ServiceUnavailableException("Login con Slack non configurato",
                    new IllegalStateException("slack.oauth.client-id / client-secret non configurati"));
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", slackProperties.getClientId());
        form.add("client_secret", slackProperties.getClientSecret());
        form.add("code", request.getCode().trim());
        form.add("grant_type", slackProperties.getGrantType());

        String redirectUri = resolveSlackRedirectUri(request);
        if (redirectUri != null) {
            form.add("redirect_uri", redirectUri);
        }

        SlackTokenResponse tokenResponse = webClient.post()
                .uri(slackProperties.getAccessTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(serviceUnavailableFor("Slack", response.statusCode(), body))))
                .bodyToMono(SlackTokenResponse.class)
                .timeout(PROVIDER_TIMEOUT)
                .block();

        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new AuthenticationException("Codice Slack non valido: " + describeSlackTokenError(tokenResponse));
        }

        log.debug("Access token Slack ottenuto (type: {}, scope: {})",
                tokenResponse.tokenType(), tokenResponse.scope());

        log.debug("Access Token: " + tokenResponse.accessToken());

        // Fetch user identity from Slack
        SlackUser user = webClient.get()
                .uri(slackProperties.getUserUrl())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new AuthenticationException(
                                "Access token Slack rifiutato (HTTP "
                                        + response.statusCode().value() + "): " + body))))
                .bodyToMono(SlackUser.class)
                .timeout(PROVIDER_TIMEOUT)
                .block();

        if (user == null || !user.isSuccessful()) {
            throw new AuthenticationException("Profilo utente Slack non valido: " +
                    (user != null ? user.errorDescription() : "risposta nulla"));
        }

        if (!StringUtils.hasText(user.getEmail())) {
            throw new AuthenticationException(
                    "Slack non ha restituito l'email: verifica che il login richieda gli scope users:read.email"
            );
        }

        String providerId = user.getUserId();
        String email = user.getEmail();
        String name = user.getUserName();

        log.debug("Login Slack completato per providerId={}, email={}", providerId, email);

        return authenticateOAuthUser(
                email,
                providerId,
                AuthProvider.SLACK,
                name,
                ""
        );
    }

    /**
     * Completes the GitHub login starting from the authorization code.
     * <ol>
     *     <li>exchanges the code for an access token;</li>
     *     <li>reads the GitHub profile;</li>
     *     <li>resolves a verified email address;</li>
     *     <li>provisions or links the local user and returns the JWT.</li>
     * </ol>
     *
     * @param request the authorization code (and optional redirect uri)
     * @return the login response with the application JWT
     * @throws AuthenticationException     if the code is missing, invalid or
     *                                     the email cannot be resolved
     * @throws ServiceUnavailableException if GitHub is unreachable or not configured
     */
    @Transactional
    public LoginResponse loginWithGithub(GithubRequest request) {

        String accessToken = exchangeGithubCodeForAccessToken(request);
        GithubUser githubUser = fetchGithubUser(accessToken);
        String email = resolveGithubEmail(accessToken, githubUser);

        String providerId = githubUser.id().toString();
        String firstName = extractFirstName(githubUser);
        String lastName = extractLastName(githubUser);

        log.info("GitHub login riuscito per {} (id {})", email, providerId);

        return authenticateOAuthUser(email, providerId, AuthProvider.GITHUB, firstName, lastName);
    }

    /**
     * Exchanges the authorization code for a GitHub access token.
     *
     * @param request the incoming request holding the code
     * @return the access token
     */
    private String exchangeGithubCodeForAccessToken(GithubRequest request) {

        if (!githubProperties.isConfigured()) {
            log.error("Login GitHub non configurato: GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET mancanti");
            throw new ServiceUnavailableException("Login con GitHub non configurato",
                    new IllegalStateException("github.oauth.client-id / client-secret non configurati"));
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", githubProperties.getClientId());
        form.add("client_secret", githubProperties.getClientSecret());
        form.add("code", request.getCode().trim());

        String redirectUri = resolveGithubRedirectUri(request);
        if (redirectUri != null) {
            form.add("redirect_uri", redirectUri);
        }

        GithubTokenResponse tokenResponse = webClient.post()
                .uri(githubProperties.getAccessTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(serviceUnavailableFor("GitHub", response.statusCode(), body))))
                .bodyToMono(GithubTokenResponse.class)
                .timeout(PROVIDER_TIMEOUT)
                .block();

        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new AuthenticationException("Codice GitHub non valido: " + describeTokenError(tokenResponse));
        }

        log.debug("Access token GitHub ottenuto (type: {}, scope: {})",
                tokenResponse.tokenType(), tokenResponse.scope());

        return tokenResponse.accessToken();
    }

    /**
     * Reads the GitHub profile of the authenticated user.
     *
     * @param accessToken the GitHub access token
     * @return the GitHub profile
     */
    private GithubUser fetchGithubUser(String accessToken) {

        GithubUser githubUser = webClient.get()
                .uri(githubProperties.getUserUrl())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("X-GitHub-Api-Version", githubProperties.getApiVersion())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new AuthenticationException(
                                "Access token GitHub rifiutato (HTTP "
                                        + response.statusCode().value() + ")"))))
                .bodyToMono(GithubUser.class)
                .timeout(PROVIDER_TIMEOUT)
                .block();

        if (githubUser == null || githubUser.id() == null) {
            log.warn("Profilo GitHub non leggibile con l'access token ottenuto");
            throw new AuthenticationException("Token GitHub non valido");
        }

        return githubUser;
    }

    /**
     * Resolves the email address of the GitHub user.
     * <p>
     * The profile email can be {@code null} when the user keeps it private, so
     * the verified email list is used as fallback. Only verified addresses are
     * accepted: the local user is created already verified.
     * </p>
     *
     * @param accessToken the GitHub access token
     * @param githubUser  the GitHub profile
     * @return the email address
     */
    private String resolveGithubEmail(String accessToken, GithubUser githubUser) {

        List<GithubUserEmail> emails = fetchGithubEmails(accessToken);

        Optional<GithubUserEmail> verifiedPrimary = emails.stream()
                .filter(GithubUserEmail::verified)
                .filter(GithubUserEmail::primary)
                .findFirst();

        if (verifiedPrimary.isPresent()) {
            return verifiedPrimary.get().email();
        }

        Optional<GithubUserEmail> anyVerified = emails.stream()
                .filter(GithubUserEmail::verified)
                .filter(e -> StringUtils.hasText(e.email()))
                .findFirst();

        if (anyVerified.isPresent()) {
            log.debug("Email GitHub non primaria ma verificata, la uso per {}", githubUser.login());
            return anyVerified.get().email();
        }

        if (StringUtils.hasText(githubUser.email())) {
            return githubUser.email();
        }

        log.warn("Nessuna email verificata trovata sull'account GitHub {}", githubUser.login());
        throw new AuthenticationException(
                "Impossibile recuperare l'email GitHub: rendila pubblica oppure concedi lo scope user:email");
    }

    /**
     * Reads the verified email list of the GitHub user.
     * <p>
     * The call requires the {@code user:email} scope: when it is missing or
     * fails, the list is simply considered empty and the caller falls back to
     * the profile email, so a scope limitation does not break the login.
     * </p>
     *
     * @param accessToken the GitHub access token
     * @return the email list, never null
     */
    private List<GithubUserEmail> fetchGithubEmails(String accessToken) {

        List<GithubUserEmail> emails = webClient.get()
                .uri(githubProperties.getUserEmailsUrl())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("X-GitHub-Api-Version", githubProperties.getApiVersion())
                .retrieve()
                .bodyToFlux(GithubUserEmail.class)
                .timeout(PROVIDER_TIMEOUT)
                .collectList()
                .onErrorResume(Exception.class, error -> {
                    log.warn("Impossibile leggere le email GitHub (scope user:email?): {}",
                            error.getMessage());
                    return Mono.just(List.<GithubUserEmail>of());
                })
                .block();

        return emails == null ? List.of() : emails;
    }

    /**
     * Picks the redirect uri to use during the code exchange.
     *
     * @param request the incoming request
     * @return the request redirect uri if present, the configured one otherwise
     */
    private String resolveGithubRedirectUri(GithubRequest request) {
        if (request != null && StringUtils.hasText(request.getRedirectUri())) {
            return request.getRedirectUri().trim();
        }
        if (StringUtils.hasText(githubProperties.getRedirectUri())) {
            return githubProperties.getRedirectUri().trim();
        }
        return null;
    }

    private String resolveDiscordRedirectUri(DiscordRequest request) {
        if (request != null && StringUtils.hasText(request.getRedirectUri())) {
            return request.getRedirectUri().trim();
        }
        if (StringUtils.hasText(discordProperties.getRedirectUri())) {
            return discordProperties.getRedirectUri().trim();
        }
        return null;
    }

    private String resolveSlackRedirectUri(SlackRequest request) {
        if (request != null && StringUtils.hasText(request.getRedirectUri())) {
            return request.getRedirectUri().trim();
        }
        if (StringUtils.hasText(slackProperties.getRedirectUri())) {
            return slackProperties.getRedirectUri().trim();
        }
        return null;
    }

    /**
     * Translates a non-2xx response from an OAuth provider into an exception exposing the body.
     * <p>
     * The body is essential: the provider explains the reason for rejection
     * (rate limit, malformed request, anti-automation block) and without it
     * the error reaches the user as a non-diagnostic generic 503.
     * </p>
     *
     * @param provider provider name (e.g., "GitHub", "Discord") for logging
     * @param status   the HTTP status returned by the provider
     * @param body     the response body, already acquired
     * @return the exception to propagate
     */
    private ServiceUnavailableException serviceUnavailableFor(String provider, HttpStatusCode status, String body) {
        String detail = body == null || body.isBlank() ? "empty body" : body.trim();
        if (detail.length() > MAX_GITHUB_ERROR_DETAIL) {
            detail = detail.substring(0, MAX_GITHUB_ERROR_DETAIL) + "...";
        }
        log.warn("{} ha risposto {} allo scambio del code: {}", provider, status.value(), detail);
        return new ServiceUnavailableException(
                provider + " ha rifiutato lo scambio del codice (HTTP " + status.value() + "): " + detail,
                new IllegalStateException(detail));
    }

    /**
     * Builds a readable message out of a failed Discord token response.
     */
    private String describeDiscordTokenError(DiscordTokenResponse tokenResponse) {
        if (tokenResponse == null) {
            return "risposta vuota da Discord";
        }
        if (StringUtils.hasText(tokenResponse.errorDescription())) {
            return tokenResponse.errorDescription();
        }
        if (StringUtils.hasText(tokenResponse.error())) {
            return tokenResponse.error();
        }
        return "access_token mancante";
    }

    /**
     * Builds a readable message out of a failed Discord token response.
     */
    private String describeSlackTokenError(SlackTokenResponse tokenResponse) {
        if (tokenResponse == null) {
            return "risposta vuota da Slack";
        }
        if (StringUtils.hasText(tokenResponse.errorDescription())) {
            return tokenResponse.errorDescription();
        }
        if (StringUtils.hasText(tokenResponse.error())) {
            return tokenResponse.error();
        }
        return "access_token mancante";
    }

    /**
     * Builds a readable message out of a failed token response.
     */
    private String describeTokenError(GithubTokenResponse tokenResponse) {
        if (tokenResponse == null) {
            return "risposta vuota da GitHub";
        }
        if (StringUtils.hasText(tokenResponse.errorDescription())) {
            return tokenResponse.errorDescription();
        }
        if (StringUtils.hasText(tokenResponse.error())) {
            return tokenResponse.error();
        }
        return "access_token mancante";
    }

    /**
     * Derives the first name from the GitHub profile, falling back to the login.
     */
    private String extractFirstName(GithubUser githubUser) {
        if (StringUtils.hasText(githubUser.name())) {
            return githubUser.name().trim().split("\\s+")[0];
        }
        return githubUser.login();
    }

    /**
     * Derives the last name from the GitHub profile, when available.
     */
    private String extractLastName(GithubUser githubUser) {
        if (!StringUtils.hasText(githubUser.name())) {
            return null;
        }
        String[] parts = githubUser.name().trim().split("\\s+");
        return parts.length > 1 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)) : null;
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

//    @Transactional
//    public LoginResponse loginWithMicrosoft(OAuthTokenRequest request) {
//        Map<?, ?> profile = webClient.get()
//                .uri("https://graph.microsoft.com/v1.0/me")
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.getToken())
//                .retrieve()
//                .bodyToMono(Map.class)
//                .block();
//
//        if (profile == null || profile.get("mail") == null && profile.get("userPrincipalName") == null) {
//            throw new AuthenticationException("Token Microsoft non valido");
//        }
//
//        String email = profile.get("mail") != null
//                ? profile.get("mail").toString()
//                : profile.get("userPrincipalName").toString();
//        String providerId = profile.get("id").toString();
//        String name = profile.get("givenName") != null ? profile.get("givenName").toString() : null;
//        String lastName = profile.get("surname") != null ? profile.get("surname").toString() : null;
//
//        return authenticateOAuthUser(email, providerId, AuthProvider.MICROSOFT, name, lastName);
//    }


    //TODO: scrivere java doc dove il metodo prende i dati del provider controlla che sono validi, poi controlla se l'account ha fatto gia l'accesso il provider , infine vengono salvati i dati utente del provider nell entita dell applivcativo
    private LoginResponse authenticateOAuthUser(String email, String providerId, AuthProvider provider,
                                                String firstName, String lastName) {

        if (!StringUtils.hasText(email) || !StringUtils.hasText(providerId)) {
            throw new AuthenticationException("Dati del provider non validi");
        }

        Optional<User> byProvider = userRepository.findByProviderIdAndAuthProvider(providerId, provider);
        if (byProvider.isPresent()) {
            return authService.buildLoginResponse(byProvider.get());
        }

        Optional<User> byEmail = userRepository.getByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();

            if (existing.getAuthProvider() != null
                    && existing.getAuthProvider() != AuthProvider.LOCAL
                    && existing.getAuthProvider() != provider) {
                log.warn("Account {} già collegato a {}, rifiuto il collegamento con {}",
                        email, existing.getAuthProvider(), provider);
                throw new AuthenticationException(
                        "Account già collegato a " + existing.getAuthProvider() + ": accedi con quel provider");
            }

            existing.setAuthProvider(provider);
            existing.setProviderId(providerId);
            existing.setEmailVerified(true);
            userRepository.save(existing);
            log.info("Account {} collegato al provider {}", email, provider);
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
        newUser.setDateOfBirth(DEFAULT_DATE_OF_BIRTH);

        User saved = userRepository.save(newUser);
        authService.syncWithCoffeeService(saved);
        log.info("Nuovo utente OAuth {} creato per {}", provider, email);
        return authService.buildLoginResponse(saved);
    }

    // TODO metodo che genera un username per l'utente salvato
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