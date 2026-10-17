package com.nexora.phase24;

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
class AwsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoint /api/v1/aws/topology is accessible without JWT")
    void testPublicTopologyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/aws/topology")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.services").isArray())
                .andExpect(jsonPath("$.data.region").value("ap-south-1 (Mumbai)"));
    }

    @Test
    @DisplayName("Static dashboard /aws.html is publicly accessible")
    void testStaticDashboardAccess() throws Exception {
        mockMvc.perform(get("/aws.html"))
                .andExpect(status().isOk());
    }
}
