package com.nexora.phase20;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.recommendation.controller.RecommendationController;
import com.nexora.recommendation.dto.RecommendationResponse;
import com.nexora.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 20: RecommendationController Unit Tests")
class RecommendationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RecommendationController recommendationController;

    private RecommendationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recommendationController).build();

        sampleResponse = RecommendationResponse.builder()
                .productId(10L)
                .productName("Wood-fired Pizza")
                .description("Authentic Italian thin crust")
                .price(BigDecimal.valueOf(320.0))
                .storeId(2L)
                .storeName("Bella Napoli")
                .score(18.5)
                .reason("Frequently reordered by you")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/for-you: returns 200 with personalized items")
    void testGetPersonalizedRecommendations() throws Exception {
        when(recommendationService.getPersonalizedRecommendations(any(), anyInt()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/recommendations/for-you?limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(10))
                .andExpect(jsonPath("$[0].productName").value("Wood-fired Pizza"))
                .andExpect(jsonPath("$[0].price").value(320.0))
                .andExpect(jsonPath("$[0].reason").value("Frequently reordered by you"));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/frequently-bought-together: returns 200 with co-occurring items")
    void testGetFrequentlyBoughtTogether() throws Exception {
        when(recommendationService.getFrequentlyBoughtTogether(eq(10L), anyInt()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/recommendations/frequently-bought-together?productId=10&limit=3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(10))
                .andExpect(jsonPath("$[0].productName").value("Wood-fired Pizza"));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/trending: returns 200 with platform top sellers")
    void testGetTrendingRecommendations() throws Exception {
        when(recommendationService.getTrendingRecommendations(anyInt()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/recommendations/trending?limit=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(10))
                .andExpect(jsonPath("$[0].storeName").value("Bella Napoli"));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/category: returns 200 with category picks")
    void testGetCategoryRecommendations() throws Exception {
        when(recommendationService.getCategoryRecommendations(eq("Pizza"), anyInt()))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/recommendations/category?name=Pizza&limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(10))
                .andExpect(jsonPath("$[0].productName").value("Wood-fired Pizza"));
    }
}
