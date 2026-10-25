package com.nexora.phase28;

import com.nexora.auth.security.JwtAuthenticationEntryPoint;
import com.nexora.auth.security.JwtAuthenticationFilter;
import com.nexora.auth.security.JwtService;
import com.nexora.systemdesign.controller.SystemDesignController;
import com.nexora.systemdesign.dto.FailureScenarioMatrix;
import com.nexora.systemdesign.dto.InterviewQuestionAnswer;
import com.nexora.systemdesign.dto.ScaleToMillionBlueprint;
import com.nexora.systemdesign.dto.SystemArchitectureDiagram;
import com.nexora.systemdesign.service.SystemDesignService;
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

@WebMvcTest(SystemDesignController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemDesignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemDesignService systemDesignService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/system-design/interview-checklist returns questions")
    void testGetChecklist() throws Exception {
        InterviewQuestionAnswer qa = InterviewQuestionAnswer.builder()
                .questionId("q1_spring_boot_mvc")
                .question("Why Spring Boot?")
                .build();

        when(systemDesignService.getInterviewChecklist()).thenReturn(Collections.singletonList(qa));

        mockMvc.perform(get("/api/v1/system-design/interview-checklist")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].questionId").value("q1_spring_boot_mvc"));
    }

    @Test
    @DisplayName("GET /api/v1/system-design/architecture returns architectural blueprint")
    void testGetArchitecture() throws Exception {
        SystemArchitectureDiagram diagram = SystemArchitectureDiagram.builder()
                .architectureType("Distributed Microservices")
                .build();

        when(systemDesignService.getArchitectureDiagram()).thenReturn(diagram);

        mockMvc.perform(get("/api/v1/system-design/architecture")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.architectureType").value("Distributed Microservices"));
    }

    @Test
    @DisplayName("GET /api/v1/system-design/scale-to-1m returns 1M users blueprint")
    void testGetScaleBlueprint() throws Exception {
        ScaleToMillionBlueprint scale = ScaleToMillionBlueprint.builder()
                .targetScale("1,000,000 DAU")
                .dailyActiveUsers(1000000.0)
                .build();

        when(systemDesignService.getScaleToMillionBlueprint()).thenReturn(scale);

        mockMvc.perform(get("/api/v1/system-design/scale-to-1m")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetScale").value("1,000,000 DAU"));
    }

    @Test
    @DisplayName("GET /api/v1/system-design/failure-matrix returns failure recovery matrix")
    void testGetFailureMatrix() throws Exception {
        FailureScenarioMatrix failure = FailureScenarioMatrix.builder()
                .failureEvent("MySQL Primary Database Crashes")
                .build();

        when(systemDesignService.getFailureScenarios()).thenReturn(Collections.singletonList(failure));

        mockMvc.perform(get("/api/v1/system-design/failure-matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].failureEvent").value("MySQL Primary Database Crashes"));
    }
}
