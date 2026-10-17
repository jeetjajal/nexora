package com.nexora.phase19;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.ai.dto.AiChatRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 19: AI Assistant Spring Boot Integration Test")
class AiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    private String validJwtToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));

        User user = userRepository.save(User.builder()
                .name("AI Test Customer")
                .email("ai_customer_" + UUID.randomUUID() + "@nexora.test")
                .password("encoded_pass")
                .phone("9998887776")
                .roles(Set.of(customerRole))
                .build());

        validJwtToken = jwtService.generateToken(new UserPrincipal(user));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat: authenticated request succeeds with 200 OK")
    void chat_AuthenticatedSuccess() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Hi, which pizza stores are currently open?")
                .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reply").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat: unauthenticated request is rejected with 401 Unauthorized")
    void chat_UnauthenticatedRejected() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Where is my order?")
                .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
