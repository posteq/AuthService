package com.innowise.academy.authservice.integration;

import com.innowise.academy.authservice.client.UserServiceClient;
import com.innowise.academy.authservice.entity.CredentialEntity;
import com.innowise.academy.authservice.entity.Role;
import com.innowise.academy.authservice.repository.CredentialRepository;
import com.innowise.academy.authservice.security.util.JwtKeys;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class AuthControllerIT {

    private static final String PASSWORD = "Password1!";
    public static final String LOGIN = "/api/auth/login";
    public static final String REFRESH = "/api/auth/refresh";
    public static final String REGISTER = "/api/auth/register";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CredentialRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtKeys keys;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }


    private static String registerJson(String username) {
        return """
                {"username":"%s","password":"%s","firstname":"Ann","surname":"Lee",
                 "birthday":"1990-01-01","email":"%s@mail.com"}
                """.formatted(username, PASSWORD, username);
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void saveCredential(String username, long userId, Role role) {
        CredentialEntity entity = new CredentialEntity();
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setPasswordHash(passwordEncoder.encode(PASSWORD));
        entity.setRole(role);

        repository.save(entity);
    }

    private String loginResponse(String username) throws Exception {
        return postJson(LOGIN, """
                {"username":"%s","password":"%s"}""".formatted(username, PASSWORD))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private PublicKey publicKeyFromJwks() throws Exception {
        String body = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        BigInteger n = new BigInteger(1, Base64.getUrlDecoder()
                                                        .decode(JsonPath.<String>read(body, "$.keys[0].n")));
        BigInteger e = new BigInteger(1, Base64.getUrlDecoder()
                                                        .decode(JsonPath.<String>read(body, "$.keys[0].e")));
        return KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(n, e));
    }


    @Test
    void register_savesBcryptHash_andRoleUser() throws Exception {
        when(userServiceClient.createUser(any())).thenReturn(100L);

        postJson(REGISTER, registerJson("ann"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.username").value("ann"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        CredentialEntity saved = repository.findByUsername("ann").orElseThrow();

        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void register_usernameTaken_returns409_andDoesNotCallUserService() throws Exception {
        saveCredential("ann", 1L, Role.USER);

        postJson(REGISTER, registerJson("ann")).andExpect(status().isConflict());

        verifyNoInteractions(userServiceClient);
    }

    @Test
    void register_savingFails_profileIsDeleted() throws Exception {
        saveCredential("other", 100L, Role.USER);
        when(userServiceClient.createUser(any())).thenReturn(100L);

        postJson(REGISTER, registerJson("ann")).andExpect(status().isConflict());

        assertThat(repository.existsByUsername("ann")).isFalse();
        verify(userServiceClient).deleteUser(100L);
    }

    @Test
    void register_invalidBody_returns400() throws Exception {
        postJson(REGISTER, "{\"username\":\"ab\",\"password\":\"short\"}")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userServiceClient);
    }

    @Test
    void login_tokenCanBeVerifiedWithTheKeyFromJwks() throws Exception {
        saveCredential("ann", 77L, Role.ADMIN);

        String accessToken = JsonPath.read(loginResponse("ann"), "$.accessToken");

        Jws<Claims> jws = Jwts.parser().verifyWith(publicKeyFromJwks()).build().parseSignedClaims(accessToken);

        Claims claims = jws.getPayload();

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(jws.getHeader().getKeyId()).isEqualTo(keys.getKid());
        assertThat(claims.get("user_id", Number.class).longValue()).isEqualTo(77L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void login_wrongPassword_and_unknownUser_return401() throws Exception {
        saveCredential("ann", 77L, Role.USER);

        postJson(LOGIN, "{\"username\":\"ann\",\"password\":\"WrongPassword1\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        postJson(LOGIN, "{\"username\":\"ghost\",\"password\":\"WrongPassword1\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void refresh_validRefreshToken_returnsNewAccessToken() throws Exception {
        saveCredential("ann", 77L, Role.USER);
        String refreshToken = JsonPath.read(loginResponse("ann"), "$.refreshToken");

        String body = postJson(REFRESH, "{\"refreshToken\":\"%s\"}".formatted(refreshToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Claims claims = Jwts.parser().verifyWith(publicKeyFromJwks()).build()
                .parseSignedClaims(JsonPath.<String>read(body, "$.accessToken")).getPayload();
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("user_id", Number.class).longValue()).isEqualTo(77L);
    }

    @Test
    void refresh_accessTokenInsteadOfRefreshToken_returns401() throws Exception {
        saveCredential("ann", 77L, Role.USER);
        String accessToken = JsonPath.read(loginResponse("ann"), "$.accessToken");

        postJson(REFRESH, "{\"refreshToken\":\"%s\"}".formatted(accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_garbageToken_returns401() throws Exception {
        postJson(REFRESH, "{\"refreshToken\":\"definitely-not-a-jwt\"}")
                .andExpect(status().isUnauthorized());
    }


    @Test
    void jwks_containsOnlyThePublicKey() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys.length()").value(1))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].kid").value(keys.getKid()))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    @Test
    void anyOtherUrl_isDenied() throws Exception {
        mockMvc.perform(get("/api/anything")).andExpect(status().isUnauthorized());
    }
}
