package com.nexora.recommendation.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.category.entity.Category;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.product.spec.ProductSpecifications;
import com.nexora.recommendation.dto.RecommendationResponse;
import com.nexora.redis.service.RedisCacheService;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of Nexora AI & Algorithmic Recommendation Engine.
 *
 * Scoring Signals:
 *   1. Repurchase Frequency (High weight): Reorders of favorite products.
 *   2. Category Affinity (Medium-High weight): Preferences across cuisines/categories.
 *   3. Price Affinity (Moderate weight): Proximity to customer's historical average order price.
 *   4. Store Affinity (Moderate weight): Preference for familiar local merchants.
 *   5. Co-occurrence Patterns: Frequently Bought Together analysis from basket data.
 *   6. Cold-Start Fallback: Trending best-sellers for new users without orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final RedisCacheService redisCacheService;

    @Value("${nexora.recommendations.cache.ttl:900}")
    private long cacheTtlSeconds;

    @Value("${nexora.recommendations.default-limit:10}")
    private int defaultLimit;

    @Override
    public List<RecommendationResponse> getPersonalizedRecommendations(UserPrincipal user, int limit) {
        int effectiveLimit = sanitizeLimit(limit);
        if (user == null || user.getId() == null) {
            return getTrendingRecommendations(effectiveLimit);
        }

        Long userId = user.getId();
        String cacheKey = "nexora:rec:user:" + userId + ":" + effectiveLimit;

        try {
            Optional<List> cached = redisCacheService.get(cacheKey, List.class);
            if (cached.isPresent()) {
                log.debug("Cache HIT for personalized recommendations for user {}", userId);
                return (List<RecommendationResponse>) cached.get();
            }
        } catch (Exception ex) {
            log.warn("Redis read exception for recommendation cache: {}", ex.getMessage());
        }

        List<Order> userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Cold-start strategy: user has never ordered before
        if (userOrders.isEmpty()) {
            log.info("User {} has no order history. Employing Cold-Start Trending Strategy.", userId);
            List<RecommendationResponse> trending = getTrendingRecommendations(effectiveLimit);
            trending.forEach(r -> r.setReason("Popular with Nexora foodies in your area"));
            cacheRecommendations(cacheKey, trending);
            return trending;
        }

        // Hot-start: Compute preferences from historical purchases
        Map<Long, Integer> productPurchaseCounts = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        Map<Long, Integer> storeCounts = new HashMap<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        int totalItemsBought = 0;

        for (Order order : userOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Long prodId = item.getProduct() != null ? item.getProduct().getId() : null;
                    if (prodId != null) {
                        productPurchaseCounts.merge(prodId, item.getQuantity() != null ? item.getQuantity() : 1, Integer::sum);
                    }

                    if (item.getProduct() != null && item.getProduct().getCategories() != null) {
                        for (Category cat : item.getProduct().getCategories()) {
                            categoryCounts.merge(cat.getName(), 1, Integer::sum);
                        }
                    }

                    if (order.getStore() != null) {
                        storeCounts.merge(order.getStore().getId(), 1, Integer::sum);
                    }

                    if (item.getUnitPrice() != null) {
                        totalSpent = totalSpent.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1)));
                        totalItemsBought += (item.getQuantity() != null ? item.getQuantity() : 1);
                    }
                }
            }
        }

        BigDecimal avgPrice = totalItemsBought > 0
                ? totalSpent.divide(BigDecimal.valueOf(totalItemsBought), 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(250.0);

        // Fetch candidate active products from open stores
        List<Product> availableProducts = productRepository.findByAvailableTrue();

        List<ScoredProduct> scoredList = new ArrayList<>();
        for (Product product : availableProducts) {
            Store store = product.getStore();
            if (store == null || store.getStatus() != StoreStatus.OPEN) {
                continue; // Only recommend from currently open stores
            }

            double score = 0.0;
            String dominantReason = "Recommended based on your preferences";

            // Signal 1: Repurchase frequency (High weight)
            int reorderCount = productPurchaseCounts.getOrDefault(product.getId(), 0);
            if (reorderCount > 0) {
                double reorderWeight = Math.min(reorderCount * 4.0, 20.0);
                score += reorderWeight;
                dominantReason = "Frequently reordered by you (" + reorderCount + " previous purchases)";
            }

            // Signal 2: Category Affinity (Medium-High weight)
            int bestCatCount = 0;
            String bestCategory = null;
            if (product.getCategories() != null) {
                for (Category cat : product.getCategories()) {
                    int cCount = categoryCounts.getOrDefault(cat.getName(), 0);
                    if (cCount > 0) {
                        score += Math.min(cCount * 2.5, 15.0);
                        if (cCount > bestCatCount) {
                            bestCatCount = cCount;
                            bestCategory = cat.getName();
                        }
                    }
                }
            }
            if (reorderCount == 0 && bestCategory != null) {
                dominantReason = "Because you love " + bestCategory;
            }

            // Signal 3: Price Affinity (Moderate weight)
            if (product.getPrice() != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
                double diffRatio = Math.abs(product.getPrice().doubleValue() - avgPrice.doubleValue()) / avgPrice.doubleValue();
                if (diffRatio <= 0.3) {
                    score += 5.0; // Within 30% of average spend
                    if (reorderCount == 0 && bestCategory == null) {
                        dominantReason = "Matches your typical spending range (~₹" + avgPrice.intValue() + ")";
                    }
                } else if (diffRatio <= 0.6) {
                    score += 2.0;
                }
            }

            // Signal 4: Store Affinity
            int sCount = storeCounts.getOrDefault(store.getId(), 0);
            if (sCount > 0) {
                score += Math.min(sCount * 1.5, 10.0);
            }

            // Signal 5: Special Discounts & Deals (Boost)
            if (product.getDiscount() != null && product.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                score += Math.min(product.getDiscount().doubleValue() * 0.2, 5.0);
            }

            if (score > 0.0) {
                scoredList.add(new ScoredProduct(product, score, dominantReason));
            }
        }

        // Sort descending by score
        scoredList.sort((a, b) -> Double.compare(b.score, a.score));

        // Limit results
        List<RecommendationResponse> recommendations = scoredList.stream()
                .limit(effectiveLimit)
                .map(sp -> toResponse(sp.product, sp.score, sp.reason))
                .collect(Collectors.toList());

        // If scored list is less than limit, backfill with popular items
        if (recommendations.size() < effectiveLimit) {
            List<RecommendationResponse> trending = getTrendingRecommendations(effectiveLimit - recommendations.size());
            Set<Long> existingIds = recommendations.stream().map(RecommendationResponse::getProductId).collect(Collectors.toSet());
            for (RecommendationResponse t : trending) {
                if (!existingIds.contains(t.getProductId())) {
                    t.setReason("Trending choice you might enjoy");
                    recommendations.add(t);
                    existingIds.add(t.getProductId());
                }
            }
        }

        cacheRecommendations(cacheKey, recommendations);
        return recommendations;
    }

    @Override
    public List<RecommendationResponse> getFrequentlyBoughtTogether(Long productId, int limit) {
        int effectiveLimit = sanitizeLimit(limit);
        if (productId == null) {
            return Collections.emptyList();
        }

        String cacheKey = "nexora:rec:fbt:" + productId + ":" + effectiveLimit;
        try {
            Optional<List> cached = redisCacheService.get(cacheKey, List.class);
            if (cached.isPresent()) {
                return (List<RecommendationResponse>) cached.get();
            }
        } catch (Exception ex) {
            log.warn("Redis read exception for FBT cache: {}", ex.getMessage());
        }

        List<Object[]> coOccurring = orderItemRepository.findCoOccurringProductIds(productId, PageRequest.of(0, effectiveLimit));

        List<RecommendationResponse> results = new ArrayList<>();
        if (!coOccurring.isEmpty()) {
            for (Object[] row : coOccurring) {
                Long relatedId = (Long) row[0];
                Long count = (Long) row[1];
                productRepository.findById(relatedId).ifPresent(p -> {
                    if (p.isAvailable() && p.getStore() != null && p.getStore().getStatus() == StoreStatus.OPEN) {
                        results.add(toResponse(p, count.doubleValue() * 5.0, "Customers often buy this together"));
                    }
                });
            }
        }

        // Fallback: If product is new or has no co-occurrences, suggest items from same store or category
        if (results.size() < effectiveLimit) {
            productRepository.findById(productId).ifPresent(baseProduct -> {
                if (baseProduct.getStore() != null) {
                    List<Product> storeProducts = productRepository.findByStoreId(baseProduct.getStore().getId());
                    for (Product p : storeProducts) {
                        if (results.size() >= effectiveLimit) break;
                        if (!p.getId().equals(productId) && p.isAvailable()) {
                            boolean alreadyIncluded = results.stream().anyMatch(r -> r.getProductId().equals(p.getId()));
                            if (!alreadyIncluded) {
                                results.add(toResponse(p, 5.0, "Popular from " + baseProduct.getStore().getName()));
                            }
                        }
                    }
                }
            });
        }

        cacheRecommendations(cacheKey, results);
        return results;
    }

    @Override
    public List<RecommendationResponse> getTrendingRecommendations(int limit) {
        int effectiveLimit = sanitizeLimit(limit);
        String cacheKey = "nexora:rec:trending:" + effectiveLimit;
        try {
            Optional<List> cached = redisCacheService.get(cacheKey, List.class);
            if (cached.isPresent()) {
                return (List<RecommendationResponse>) cached.get();
            }
        } catch (Exception ex) {
            log.warn("Redis read exception for trending cache: {}", ex.getMessage());
        }

        List<Object[]> topPurchased = orderItemRepository.findTopPurchasedProductIds(PageRequest.of(0, effectiveLimit));
        List<RecommendationResponse> results = new ArrayList<>();

        if (!topPurchased.isEmpty()) {
            for (Object[] row : topPurchased) {
                Long prodId = (Long) row[0];
                Long count = (Long) row[1];
                productRepository.findById(prodId).ifPresent(p -> {
                    if (p.isAvailable() && p.getStore() != null && p.getStore().getStatus() == StoreStatus.OPEN) {
                        results.add(toResponse(p, count.doubleValue() * 2.0, "Best seller on Nexora"));
                    }
                });
            }
        }

        // Backfill if few or no orders exist in DB yet
        if (results.size() < effectiveLimit) {
            List<Product> available = productRepository.findByAvailableTrue();
            Set<Long> existingIds = results.stream().map(RecommendationResponse::getProductId).collect(Collectors.toSet());
            for (Product p : available) {
                if (results.size() >= effectiveLimit) break;
                if (!existingIds.contains(p.getId()) && p.getStore() != null && p.getStore().getStatus() == StoreStatus.OPEN) {
                    results.add(toResponse(p, 1.0, "Trending top pick"));
                    existingIds.add(p.getId());
                }
            }
        }

        cacheRecommendations(cacheKey, results);
        return results;
    }

    @Override
    public List<RecommendationResponse> getCategoryRecommendations(String category, int limit) {
        int effectiveLimit = sanitizeLimit(limit);
        if (category == null || category.isBlank()) {
            return getTrendingRecommendations(effectiveLimit);
        }

        Specification<Product> spec = Specification.where(ProductSpecifications.hasCategoryName(category))
                .and(ProductSpecifications.isAvailable(true));

        List<Product> products = productRepository.findAll(spec);
        List<RecommendationResponse> results = products.stream()
                .filter(p -> p.getStore() != null && p.getStore().getStatus() == StoreStatus.OPEN)
                .limit(effectiveLimit)
                .map(p -> toResponse(p, 10.0, "Top rated in " + category))
                .toList();

        return results;
    }

    private void cacheRecommendations(String key, Object data) {
        try {
            redisCacheService.set(key, data, cacheTtlSeconds);
        } catch (Exception ex) {
            log.debug("Redis cache write bypassed: {}", ex.getMessage());
        }
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) return defaultLimit;
        return Math.min(limit, 50);
    }

    private RecommendationResponse toResponse(Product p, double score, String reason) {
        List<String> categories = p.getCategories() != null
                ? p.getCategories().stream().map(Category::getName).toList()
                : List.of();

        return RecommendationResponse.builder()
                .productId(p.getId())
                .productName(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .discount(p.getDiscount())
                .imageUrl(p.getImageUrl())
                .storeId(p.getStore() != null ? p.getStore().getId() : null)
                .storeName(p.getStore() != null ? p.getStore().getName() : null)
                .categories(categories)
                .score(Math.round(score * 10.0) / 10.0)
                .reason(reason)
                .build();
    }

    private static class ScoredProduct {
        final Product product;
        final double score;
        final String reason;

        ScoredProduct(Product product, double score, String reason) {
            this.product = product;
            this.score = score;
            this.reason = reason;
        }
    }
}
