package com.cityguard.backend;

import com.cityguard.backend.entity.User;
import com.cityguard.backend.entity.UserAuthProvider;
import com.cityguard.backend.repository.UserAuthProviderRepository;
import com.cityguard.backend.repository.UserFirebaseAccountRepository;
import com.cityguard.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class FirebaseAuthIntegrationTest {

    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFirebaseAccountRepository userFirebaseAccountRepository;

    @Autowired
    private UserAuthProviderRepository userAuthProviderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(webContext)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", webContext))
                .build();
    }

    @Test
    void firebaseLogin_createsAccountLinksSecondProviderAndBlocksNullPasswordLogin() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String firebaseUid = "fb-it-" + suffix;
        String email = "fb_" + suffix + "@example.com";

        FirebaseToken googleToken = mockToken(
                firebaseUid,
                email,
                "Firebase User",
                "https://example.com/pic.jpg",
                "google.com",
                "google-sub-" + suffix
        );

        FirebaseToken facebookToken = mockToken(
                firebaseUid,
                email,
                "Firebase User FB",
                "https://example.com/fb-pic.jpg",
                "facebook.com",
                "fb-sub-" + suffix
        );

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("google-token-" + suffix)).thenReturn(googleToken);
            when(firebaseAuth.verifyIdToken("fb-token-" + suffix)).thenReturn(facebookToken);

            // 1. Đăng nhập lần đầu bằng GOOGLE qua /api/v1/auth/firebase
            String googleResp = mvc.perform(post("/api/v1/auth/firebase")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"google-token-" + suffix + "\",\"provider\":\"GOOGLE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.userInfo.id").isNumber())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode googleData = objectMapper.readTree(googleResp).path("data");
            Long userId = googleData.path("userInfo").path("id").asLong();
            assertTrue(userFirebaseAccountRepository.findByFirebaseUid(firebaseUid).isPresent());

            // 2. Đăng nhập tiếp bằng FACEBOOK cùng firebaseUid qua alias /api/v1/auth/firebase-login
            mvc.perform(post("/api/v1/auth/firebase-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"fb-token-" + suffix + "\",\"provider\":\"FACEBOOK\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userInfo.id").value(userId));

            List<UserAuthProvider> providers = userAuthProviderRepository.findByUserId(userId);
            assertEquals(2, providers.size());

            // 3. Đăng nhập thường với tài khoản OAuth (passwordHash == null) phải trả về 401
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"account\":\"" + email + "\",\"password\":\"anySecret123\"}"))
                    .andExpect(status().isUnauthorized());

            // 4. Khóa tài khoản -> đăng nhập Firebase phải trả về 403 FORBIDDEN
            User user = userRepository.findById(userId).orElseThrow();
            user.setStatus("LOCKED");
            userRepository.save(user);

            mvc.perform(post("/api/v1/auth/firebase")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"google-token-" + suffix + "\",\"provider\":\"GOOGLE\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void firebaseLogin_duplicateEmailWithLocalAccount_returnsConflict409() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "local_" + suffix;
        String email = "local_" + suffix + "@example.com";

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"password\":\"secret123\",\"displayName\":\"Local User\"}"))
                .andExpect(status().isCreated());

        FirebaseToken token = mockToken(
                "fb-dup-" + suffix,
                email,
                "Attacker",
                null,
                "google.com",
                "google-dup-" + suffix
        );

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("dup-token-" + suffix)).thenReturn(token);

            mvc.perform(post("/api/v1/auth/firebase")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"dup-token-" + suffix + "\",\"provider\":\"GOOGLE\"}"))
                    .andExpect(status().isConflict());
        }
    }

    private FirebaseToken mockToken(
            String uid,
            String email,
            String name,
            String picture,
            String signInProvider,
            String providerUid
    ) {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getEmail()).thenReturn(email);
        when(token.getName()).thenReturn(name);
        when(token.getPicture()).thenReturn(picture);
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of(
                        "sign_in_provider", signInProvider,
                        "identities", Map.of(signInProvider, List.of(providerUid))
                )
        ));
        return token;
    }
}
