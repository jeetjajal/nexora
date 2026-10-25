package com.nexora.phase20;

import com.nexora.ai.tool.RecommendationAiTools;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.recommendation.dto.RecommendationResponse;
import com.nexora.recommendation.service.RecommendationService;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 20: RecommendationAiTools Unit Tests")
class RecommendationAiToolsTest {

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RecommendationAiTools recommendationAiTools;

    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("Food Lover").email("food@nexora.com").build();
        testUserPrincipal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("getPersonalizedRecommendations tool outputs formatted text with reasons and match score")
    void testGetPersonalizedRecommendationsTool() {
        RecommendationResponse item = RecommendationResponse.builder()
                .productId(1L)
                .productName("Truffle Pasta")
                .price(BigDecimal.valueOf(450.0))
                .storeName("Pasta Fresca")
                .score(0.92)
                .reason("Because you love Italian")
                .build();

        when(recommendationService.getPersonalizedRecommendations(any(), anyInt()))
                .thenReturn(List.of(item));

        String result = recommendationAiTools.getPersonalizedRecommendations(testUserPrincipal, 5);

        assertNotNull(result);
        assertTrue(result.contains("Truffle Pasta"));
        assertTrue(result.contains("450.00"));
        assertTrue(result.contains("Because you love Italian"));
    }

    @Test
    @DisplayName("getTrendingRecommendations tool handles empty list gracefully")
    void testGetTrendingRecommendationsEmpty() {
        when(recommendationService.getTrendingRecommendations(anyInt())).thenReturn(Collections.emptyList());

        String result = recommendationAiTools.getTrendingRecommendations(5);

        assertNotNull(result);
        assertTrue(result.contains("No trending items found"));
    }
}
