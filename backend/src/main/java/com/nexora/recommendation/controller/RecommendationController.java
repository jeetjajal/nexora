package com.nexora.recommendation.controller;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.recommendation.dto.RecommendationResponse;
import com.nexora.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Nexora Recommendation Engine (Phase 20).
 * Provides endpoints for personalized "Recommended For You",
 * Frequently Bought Together (FBT), platform trending items, and category-level recommendations.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Personalized "Recommended For You" for the authenticated customer.
     * Evaluates purchase frequency, category affinity, price affinity, and favorite stores.
     * Cold-start fallback applies automatically if the customer has 0 past orders.
     */
    @GetMapping("/for-you")
    public ResponseEntity<List<RecommendationResponse>> getPersonalizedRecommendations(
            @AuthenticationPrincipal UserPrincipal caller,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RecommendationResponse> recommendations =
                recommendationService.getPersonalizedRecommendations(caller, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Frequently Bought Together products for a given product ID.
     * Computes co-occurrence frequencies from completed multi-item orders.
     */
    @GetMapping("/frequently-bought-together")
    public ResponseEntity<List<RecommendationResponse>> getFrequentlyBoughtTogether(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<RecommendationResponse> recommendations =
                recommendationService.getFrequentlyBoughtTogether(productId, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Platform-wide Trending and Popular recommendations across open stores.
     */
    @GetMapping("/trending")
    public ResponseEntity<List<RecommendationResponse>> getTrendingRecommendations(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RecommendationResponse> recommendations =
                recommendationService.getTrendingRecommendations(limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Top product recommendations for a specific category name.
     */
    @GetMapping("/category")
    public ResponseEntity<List<RecommendationResponse>> getCategoryRecommendations(
            @RequestParam String name,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RecommendationResponse> recommendations =
                recommendationService.getCategoryRecommendations(name, limit);
        return ResponseEntity.ok(recommendations);
    }
}
