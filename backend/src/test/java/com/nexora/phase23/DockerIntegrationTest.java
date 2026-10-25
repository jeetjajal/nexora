package com.nexora.phase23;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DockerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoint /api/v1/docker/topology is accessible without JWT")
    void testPublicTopologyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/docker/topology")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.containers").isArray())
                .andExpect(jsonPath("$.data.composeVersion").value("3.8"));
    }

    @Test
    @DisplayName("Static dashboard /docker.html is publicly accessible")
    void testStaticDashboardAccess() throws Exception {
        mockMvc.perform(get("/docker.html"))
                .andExpect(status().isOk());
    }
}
