package com.nexora.phase23;

import com.nexora.auth.security.JwtAuthenticationEntryPoint;
import com.nexora.auth.security.JwtAuthenticationFilter;
import com.nexora.auth.security.JwtService;
import com.nexora.docker.controller.DockerController;
import com.nexora.docker.dto.DockerConceptExplanation;
import com.nexora.docker.dto.DockerContainerSpec;
import com.nexora.docker.dto.DockerTopologyResponse;
import com.nexora.docker.service.DockerTopologyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DockerController.class)
@AutoConfigureMockMvc(addFilters = false)
class DockerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DockerTopologyService dockerTopologyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/docker/containers returns container list")
    void testGetContainers() throws Exception {
        DockerContainerSpec spec = DockerContainerSpec.builder()
                .containerName("nexora-mysql")
                .serviceName("mysql")
                .image("mysql:8.0.35")
                .portMapping("3306:3306")
                .status("HEALTHY")
                .build();

        when(dockerTopologyService.getAllContainers()).thenReturn(Collections.singletonList(spec));

        mockMvc.perform(get("/api/v1/docker/containers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].containerName").value("nexora-mysql"))
                .andExpect(jsonPath("$.data[0].status").value("HEALTHY"));
    }

    @Test
    @DisplayName("GET /api/v1/docker/topology returns full architecture")
    void testGetTopology() throws Exception {
        DockerTopologyResponse topology = DockerTopologyResponse.builder()
                .composeVersion("3.8")
                .networkName("nexora-net")
                .volumes(Collections.singletonList("mysql_data"))
                .containers(Collections.emptyList())
                .concepts(Collections.emptyList())
                .securitySummary("Zero hardcoded secrets")
                .build();

        when(dockerTopologyService.getTopology()).thenReturn(topology);

        mockMvc.perform(get("/api/v1/docker/topology")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.composeVersion").value("3.8"))
                .andExpect(jsonPath("$.data.networkName").value("nexora-net"));
    }

    @Test
    @DisplayName("GET /api/v1/docker/concepts returns concept explanations")
    void testGetConcepts() throws Exception {
        DockerConceptExplanation concept = DockerConceptExplanation.builder()
                .concept("Image vs Container")
                .definition("An Image is immutable...")
                .build();

        when(dockerTopologyService.getConceptExplanations()).thenReturn(Collections.singletonList(concept));

        mockMvc.perform(get("/api/v1/docker/concepts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].concept").value("Image vs Container"));
    }
}
