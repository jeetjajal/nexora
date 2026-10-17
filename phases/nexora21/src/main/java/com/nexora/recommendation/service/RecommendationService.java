package com.nexora.recommendation.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.recommendation.dto.RecommendationResponse;

import java.util.List;

/**
 * Service interface for Nexora AI & algorithmic product recommendations.
 */
public interface RecommendationService {

    /**
     * Get personalized "Recommended For You" products for the authenticated customer.
     * Evaluates order history, category affinity, price affinity, and favorite stores.
     * Uses cold-start fallback (trending/best-sellers) if the customer has no previous orders.
     */
    List<RecommendationResponse> getPersonalizedRecommendations(UserPrincipal user, int limit);

    /**
     * Get products frequently bought together with the given product.
     * Computes co-occurrence frequencies from completed multi-item orders.
     */
    List<RecommendationResponse> getFrequentlyBoughtTogether(Long productId, int limit);

    /**
     * Get platform trending/popular recommendations across all open stores.
     */
    List<RecommendationResponse> getTrendingRecommendations(int limit);

    /**
     * Get top product recommendations for a specific category name.
     */
    List<RecommendationResponse> getCategoryRecommendations(String category, int limit);
}
