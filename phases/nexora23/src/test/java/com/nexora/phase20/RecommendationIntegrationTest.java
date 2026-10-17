package com.nexora.phase20;

import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 20: AI Recommendation Engine Integration Tests")
class RecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    private String customerJwtToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));

        User user = userRepository.save(User.builder()
                .name("Recommendation Tester")
                .email("rec_customer_" + UUID.randomUUID() + "@nexora.test")
                .password("encoded_pass")
                .phone("9991112223")
                .roles(Set.of(customerRole))
                .build());

        customerJwtToken = jwtService.generateToken(new UserPrincipal(user));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/trending: accessible publicly (200 OK)")
    void testTrendingPublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/trending?limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/frequently-bought-together: accessible publicly (200 OK)")
    void testFrequentlyBoughtTogetherPublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/frequently-bought-together?productId=1&limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/category: accessible publicly (200 OK)")
    void testCategoryPublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/category?name=Pizza&limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/for-you: unauthenticated request is rejected (401)")
    void testPersonalizedUnauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/for-you?limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/for-you: authenticated request succeeds (200 OK)")
    void testPersonalizedAuthenticatedSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/for-you?limit=5")
                        .header("Authorization", "Bearer " + customerJwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /recommendations.html: static frontend page is accessible publicly (200 OK)")
    void testRecommendationsStaticPageAccessible() throws Exception {
        mockMvc.perform(get("/recommendations.html"))
                .andExpect(status().isOk());
    }
}
