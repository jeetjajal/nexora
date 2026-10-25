package com.nexora.phase3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.dto.LoginRequest;
import com.nexora.user.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WHAT MAKES THIS DIFFERENT FROM UserServiceTest / AuthServiceTest?
 * Those mock their collaborators to test ONE class in isolation.
 * This test instead loads the FULL Spring application context — real
 * SecurityConfig, real JwtAuthenticationFilter, real
 * DaoAuthenticationProvider, real BCrypt — against a real (in-memory
 * H2) database, and drives it through actual HTTP requests via
 * MockMvc. This is what proves the WHOLE chain in the architecture
 * diagram (AuthController -> AuthService -> AuthenticationManager ->
 * PasswordEncoder/UserRepository -> JwtService -> token, and then the
 * reverse trip through JwtAuthenticationFilter on a protected request)
 * genuinely works end-to-end, not just each piece individually.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 3: Auth end-to-end integration tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest newRegisterRequest(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Integration Test User");
        request.setEmail(email);
        request.setPassword("CorrectPass123");
        request.setPhone("9876500000");
        return request;
    }

    @Test
    @DisplayName("POST /register then POST /login should succeed and return a usable JWT")
    void registerThenLoginShouldSucceed() throws Exception {
        String email = "flow.success@example.com";

        // Step 1: Register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));

        // Step 2: Login with the same credentials
        LoginRequest loginRequest = new LoginRequest(email, "CorrectPass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    @DisplayName("Registering with an already-used email should return 409 Conflict")
    void registerWithDuplicateEmailShouldFail() throws Exception {
        String email = "flow.duplicate@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest(email))))
                .andExpect(status().isCreated());

        // Second registration attempt with the SAME email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Login with the wrong password should return 401 Unauthorized")
    void loginWithWrongPasswordShouldFail() throws Exception {
        String email = "flow.wrongpass@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest(email))))
                .andExpect(status().isCreated());

        LoginRequest wrongPasswordLogin = new LoginRequest(email, "TotallyWrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Login with an email that was never registered should return 401 Unauthorized")
    void loginWithUnknownEmailShouldFail() throws Exception {
        LoginRequest unknownEmailLogin = new LoginRequest("never.registered@example.com", "whatever123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownEmailLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Calling a protected endpoint WITHOUT a token should return 401 Unauthorized")
    void protectedEndpointWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Calling a protected endpoint WITH a valid token should succeed")
    void protectedEndpointWithValidTokenShouldSucceed() throws Exception {
        String email = "flow.protected@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest(email))))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, "CorrectPass123");

        String loginResponseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponseJson)
                .path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    @DisplayName("Calling a protected endpoint with a garbage token should return 401 Unauthorized")
    void protectedEndpointWithInvalidTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer this.is.not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }
}
