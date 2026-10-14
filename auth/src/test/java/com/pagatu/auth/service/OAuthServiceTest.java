package com.pagatu.auth.service;

import com.pagatu.auth.config.DiscordProperties;
import com.pagatu.auth.config.GithubProperties;
import com.pagatu.auth.dto.GithubRequest;
import com.pagatu.auth.dto.GithubTokenResponse;
import com.pagatu.auth.dto.GithubUser;
import com.pagatu.auth.dto.GithubUserEmail;
import com.pagatu.auth.dto.LoginResponse;
import com.pagatu.auth.entity.AuthProvider;
import com.pagatu.auth.entity.User;
import com.pagatu.auth.exception.AuthenticationException;
import com.pagatu.auth.exception.ServiceUnavailableException;
import com.pagatu.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the GitHub branch of {@link OAuthService}: authorization code
 * exchange, profile/email resolution and local user provisioning.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("rawtypes")
class OAuthServiceTest {

    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";
    private static final String USER_EMAILS_URL = "https://api.github.com/user/emails";

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private AuthService authService;

    private WebClient webClient;

    private GithubProperties githubProperties;
    private DiscordProperties discordProperties;

    private OAuthService oAuthService;

    // Leaf of the POST chain (token exchange): post() -> ... -> retrieve()
    private RequestBodySpec requestBodySpec;
    private ResponseSpec tokenResponseSpec;

    // Leaves of the GET chains (profile and email list): get() -> ... -> retrieve()
    private RequestHeadersSpec profileRequestSpec;
    private RequestHeadersSpec emailsRequestSpec;
    private ResponseSpec userResponseSpec;
    private ResponseSpec emailsResponseSpec;

    @BeforeEach
    void setUp() {
        // The fluent WebClient chain is stubbed as a whole; individual tests only assert
        // the part of the flow they target, hence the lenient strictness.
        webClient = mock(WebClient.class);
        when(webClientBuilder.build()).thenReturn(webClient);

        // POST chain: post() -> uri() -> contentType() -> accept() -> bodyValue() -> retrieve()
        requestBodySpec = mock(RequestBodySpec.class);
        tokenResponseSpec = mock(ResponseSpec.class);
        RequestBodySpec bodySpecStub = requestBodySpec;
        RequestBodyUriSpec postSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClient.post()).thenReturn(postSpec);
        doReturn(bodySpecStub).when(postSpec).uri(eq(TOKEN_URL), any(Object[].class));
        when(bodySpecStub.contentType(any())).thenReturn(bodySpecStub);
        when(bodySpecStub.accept(any())).thenReturn(bodySpecStub);
        RequestHeadersSpec tokenBodyStub = mock(RequestHeadersSpec.class);
        when(tokenBodyStub.retrieve()).thenReturn(tokenResponseSpec);
        doReturn(tokenBodyStub).when(bodySpecStub).bodyValue(any());
        when(tokenResponseSpec.onStatus(any(), any())).thenReturn(tokenResponseSpec);

        // GET chains: get() -> uri() -> accept()/header() -> retrieve()
        userResponseSpec = mock(ResponseSpec.class);
        emailsResponseSpec = mock(ResponseSpec.class);
        profileRequestSpec = mock(RequestHeadersSpec.class);
        emailsRequestSpec = mock(RequestHeadersSpec.class);
        RequestHeadersUriSpec getSpec = mock(WebClient.RequestHeadersUriSpec.class);
        when(webClient.get()).thenReturn(getSpec);
        doReturn(profileRequestSpec).when(getSpec).uri(eq(USER_URL), any(Object[].class));
        doReturn(emailsRequestSpec).when(getSpec).uri(eq(USER_EMAILS_URL), any(Object[].class));
        when(profileRequestSpec.accept(any())).thenReturn(profileRequestSpec);
        when(profileRequestSpec.header(anyString(), any(String[].class))).thenReturn(profileRequestSpec);
        when(profileRequestSpec.retrieve()).thenReturn(userResponseSpec);
        when(userResponseSpec.onStatus(any(), any())).thenReturn(userResponseSpec);
        when(emailsRequestSpec.accept(any())).thenReturn(emailsRequestSpec);
        when(emailsRequestSpec.header(anyString(), any(String[].class))).thenReturn(emailsRequestSpec);
        when(emailsRequestSpec.retrieve()).thenReturn(emailsResponseSpec);

        githubProperties = new GithubProperties();
        githubProperties.setClientId("test-client-id");
        githubProperties.setClientSecret("test-client-secret");

        oAuthService = new OAuthService(userRepository, authService, webClientBuilder, githubProperties,discordProperties);
    }

    @Test
    void loginWithGithub_WhenCredentialsMissing_ShouldThrowServiceUnavailable() {
        githubProperties.setClientId(null);
        githubProperties.setClientSecret(null);

        assertThrows(ServiceUnavailableException.class,
                () -> oAuthService.loginWithGithub(new GithubRequest("code", null)));

        verify(webClient, never()).post();
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGithub_WhenCodeIsInvalid_ShouldThrowAuthenticationException() {
        stubTokenResponse(new GithubTokenResponse(null, null, null, "bad_verification_code",
                "The code passed is incorrect or expired."));

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> oAuthService.loginWithGithub(new GithubRequest("expired-code", null)));

        assertTrue(exception.getMessage().contains("code passed is incorrect"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGithub_WhenNoAccessTokenInResponse_ShouldThrowAuthenticationException() {
        stubTokenResponse(new GithubTokenResponse(null, "bearer", "read:user", null, null));

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> oAuthService.loginWithGithub(new GithubRequest("code", null)));

        assertTrue(exception.getMessage().contains("access_token mancante"));
    }

    @Test
    void loginWithGithub_ShouldSendClientCredentialsAndRedirectUri() {
        stubHappyPath(existingGithubUser(), "ghuser@example.com");

        oAuthService.loginWithGithub(new GithubRequest("valid-code", "http://localhost:8888/auth/callback/github"));

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor =
                ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).contentType(MediaType.APPLICATION_FORM_URLENCODED);
        verify(requestBodySpec).bodyValue(formCaptor.capture());

        MultiValueMap<String, String> form = formCaptor.getValue();
        assertEquals("test-client-id", form.getFirst("client_id"));
        assertEquals("test-client-secret", form.getFirst("client_secret"));
        assertEquals("valid-code", form.getFirst("code"));
        assertEquals("http://localhost:8888/auth/callback/github", form.getFirst("redirect_uri"));
    }

    @Test
    void loginWithGithub_WhenRedirectUriNotProvided_ShouldUseConfiguredOne() {
        githubProperties.setRedirectUri("http://localhost:8888/auth/callback/github");
        stubHappyPath(existingGithubUser(), "ghuser@example.com");

        oAuthService.loginWithGithub(new GithubRequest("valid-code", null));

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor =
                ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodySpec).bodyValue(formCaptor.capture());
        assertEquals("http://localhost:8888/auth/callback/github",
                formCaptor.getValue().getFirst("redirect_uri"));
    }

    @Test
    void loginWithGithub_ShouldCallGithubApisWithBearerToken() {
        stubHappyPath(existingGithubUser(), "ghuser@example.com");

        oAuthService.loginWithGithub(new GithubRequest("valid-code", null));

        verify(profileRequestSpec).header(HttpHeaders.AUTHORIZATION, "Bearer gho_token");
        verify(profileRequestSpec).header("X-GitHub-Api-Version", "2022-11-28");
        verify(emailsRequestSpec).header(HttpHeaders.AUTHORIZATION, "Bearer gho_token");
    }

    @Test
    void loginWithGithub_WhenUserAlreadyLinked_ShouldReturnExistingUser() {
        User existing = existingGithubUser();
        stubHappyPath(existing, "ghuser@example.com", 42L, "ghuser", "Gh User");

        when(userRepository.findByProviderIdAndAuthProvider("42", AuthProvider.GITHUB))
                .thenReturn(Optional.of(existing));
        when(authService.buildLoginResponse(existing))
                .thenReturn(new LoginResponse("jwt", "ghuser", "ghuser@example.com"));

        LoginResponse response = oAuthService.loginWithGithub(new GithubRequest("valid-code", null));

        assertEquals("ghuser", response.username());
        assertEquals("ghuser@example.com", response.email());
        assertEquals("jwt", response.token());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGithub_WhenEmailIsPrivate_ShouldUseVerifiedPrimaryEmail() {
        stubTokenResponse(new GithubTokenResponse("gho_token", "bearer", "read:user user:email", null, null));
        stubUserResponse(new GithubUser(99L, "octocat", "Mona Lisa Octocat", null));
        stubEmailsResponse(List.of(
                new GithubUserEmail("secondary@example.com", false, true),
                new GithubUserEmail("mona@example.com", true, true)
        ));

        when(userRepository.findByProviderIdAndAuthProvider("99", AuthProvider.GITHUB))
                .thenReturn(Optional.empty());
        when(userRepository.getByEmail("mona@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(authService.buildLoginResponse(any(User.class)))
                .thenReturn(new LoginResponse("jwt", "octocat", "mona@example.com"));

        LoginResponse response = oAuthService.loginWithGithub(new GithubRequest("valid-code", null));

        assertEquals("mona@example.com", response.email());

        User created = captor.getValue();
        assertEquals(AuthProvider.GITHUB, created.getAuthProvider());
        assertEquals("99", created.getProviderId());
        assertTrue(created.getEmailVerified());
        // username is derived from the email local part, "mona" being shorter than the minimum
        assertEquals("monauser", created.getUsername());
        assertEquals("Mona", created.getFirstName());
        assertEquals("Lisa Octocat", created.getLastName());
    }

    @Test
    void loginWithGithub_WhenOnlyUnverifiedEmail_ShouldThrowAuthenticationException() {
        stubTokenResponse(new GithubTokenResponse("gho_token", "bearer", "read:user", null, null));
        stubUserResponse(new GithubUser(5L, "ghost", "Ghost User", null));
        stubEmailsResponse(List.of(new GithubUserEmail("unverified@example.com", true, false)));

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> oAuthService.loginWithGithub(new GithubRequest("valid-code", null)));

        assertTrue(exception.getMessage().contains("scope user:email"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithGithub_WhenExistingLocalAccount_ShouldLinkProvider() {
        User existing = new User();
        existing.setId(3L);
        existing.setUsername("localuser");
        existing.setEmail("local@example.com");
        existing.setAuthProvider(AuthProvider.LOCAL);
        existing.setPassword("encoded");

        stubHappyPath(existing, "local@example.com", 11L, "localuser", "Local User");

        oAuthService.loginWithGithub(new GithubRequest("valid-code", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(AuthProvider.GITHUB, captor.getValue().getAuthProvider());
        assertEquals("11", captor.getValue().getProviderId());
        assertTrue(captor.getValue().getEmailVerified());
    }

    @Test
    void loginWithGithub_WhenAccountLinkedToOtherProvider_ShouldReject() {
        User existing = new User();
        existing.setId(4L);
        existing.setUsername("multiuser");
        existing.setEmail("multi@example.com");
        existing.setAuthProvider(AuthProvider.GOOGLE);
        existing.setProviderId("google-sub");

        stubHappyPath(existing, "multi@example.com", 12L, "multiuser", "Multi User");

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> oAuthService.loginWithGithub(new GithubRequest("valid-code", null)));

        assertTrue(exception.getMessage().contains("GOOGLE"));
        verify(userRepository, never()).save(any());
    }

    /**
     * Stubs token, profile and email responses for the given GitHub identity,
     * wiring the repository lookups performed after the profile is read.
     *
     * @param existing   the account that will be found by email
     * @param email      the email exposed by GitHub
     * @param githubId   the GitHub numeric user id
     * @param login      the GitHub login
     * @param fullName   the GitHub display name
     */
    private void stubHappyPath(User existing, String email, long githubId, String login, String fullName) {
        stubTokenResponse(new GithubTokenResponse("gho_token", "bearer", "read:user user:email", null, null));
        stubUserResponse(new GithubUser(githubId, login, fullName, email));
        stubEmailsResponse(List.of(new GithubUserEmail(email, true, true)));

        when(userRepository.findByProviderIdAndAuthProvider(eq(String.valueOf(githubId)), any()))
                .thenReturn(Optional.empty());
        when(userRepository.getByEmail(email)).thenReturn(Optional.of(existing));
        when(authService.buildLoginResponse(any(User.class)))
                .thenReturn(new LoginResponse("jwt", existing.getUsername(), email));
    }

    /**
     * Same as {@link #stubHappyPath(User, String, long, String, String)} for the
     * standard {@code ghuser} identity.
     */
    private void stubHappyPath(User existing, String email) {
        stubHappyPath(existing, email, 1L, "ghuser", "Gh User");
    }

    private User existingGithubUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ghuser");
        user.setEmail("ghuser@example.com");
        user.setAuthProvider(AuthProvider.GITHUB);
        user.setProviderId("1");
        return user;
    }

    private void stubTokenResponse(GithubTokenResponse response) {
        when(tokenResponseSpec.bodyToMono(GithubTokenResponse.class)).thenReturn(Mono.just(response));
    }

    private void stubUserResponse(GithubUser user) {
        when(userResponseSpec.bodyToMono(GithubUser.class)).thenReturn(Mono.just(user));
    }

    private void stubEmailsResponse(List<GithubUserEmail> emails) {
        when(emailsResponseSpec.bodyToFlux(GithubUserEmail.class)).thenReturn(Flux.fromIterable(emails));
    }
}