package com.nexora.ai.tool;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.recommendation.dto.RecommendationResponse;
import com.nexora.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Approved AI Tool for Nexora AI Assistant to query personalized recommendations,
 * trending items, and frequently bought together items.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationAiTools {

    private final RecommendationService recommendationService;

    /**
     * Get personalized food and product recommendations for the user.
     */
    public String getPersonalizedRecommendations(UserPrincipal caller, int limit) {
        try {
            List<RecommendationResponse> recs = recommendationService.getPersonalizedRecommendations(caller, limit > 0 ? limit : 5);
            if (recs.isEmpty()) {
                return "No recommendations currently available.";
            }

            StringBuilder sb = new StringBuilder("Here are your personalized recommendations:\n");
            for (RecommendationResponse r : recs) {
                sb.append(String.format("- %s (₹%.2f) from %s\n", r.getProductName(), r.getPrice(), r.getStoreName()));
                if (r.getReason() != null && !r.getReason().isBlank()) {
                    sb.append(String.format("  Reason: %s (Match score: %.0f%%)\n", r.getReason(), r.getScore() != null ? r.getScore() * 100 : 0));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("getPersonalizedRecommendations error: {}", e.getMessage());
            return "Unable to load recommendations at the moment: " + e.getMessage();
        }
    }

    /**
     * Get platform trending / popular items.
     */
    public String getTrendingRecommendations(int limit) {
        try {
            List<RecommendationResponse> recs = recommendationService.getTrendingRecommendations(limit > 0 ? limit : 5);
            if (recs.isEmpty()) {
                return "No trending items found.";
            }

            StringBuilder sb = new StringBuilder("Trending & Popular items on Nexora right now:\n");
            for (RecommendationResponse r : recs) {
                sb.append(String.format("- %s (₹%.2f) from %s\n", r.getProductName(), r.getPrice(), r.getStoreName()));
                if (r.getReason() != null) {
                    sb.append(String.format("  %s\n", r.getReason()));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("getTrendingRecommendations error: {}", e.getMessage());
            return "Unable to load trending items: " + e.getMessage();
        }
    }
}
