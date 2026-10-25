package com.nexora.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing an AI/algorithm recommendation item for the customer.
 * Contains product metadata, ranking score, and user-facing explanation reason.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private BigDecimal discount;
    private String imageUrl;
    private Long storeId;
    private String storeName;
    private List<String> categories;
    private Double score;
    private String reason;
}
