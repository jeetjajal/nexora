package com.nexora.phase28;

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
class SystemDesignIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoint /api/v1/system-design/interview-checklist is accessible without JWT")
    void testPublicChecklistAccess() throws Exception {
        mockMvc.perform(get("/api/v1/system-design/interview-checklist")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(16));
    }

    @Test
    @DisplayName("Public endpoint /api/v1/system-design/failure-matrix is accessible without JWT")
    void testPublicFailureMatrixAccess() throws Exception {
        mockMvc.perform(get("/api/v1/system-design/failure-matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    @DisplayName("Static dashboard /system-design.html is publicly accessible")
    void testStaticDashboardAccess() throws Exception {
        mockMvc.perform(get("/system-design.html"))
                .andExpect(status().isOk());
    }
}
