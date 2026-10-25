package com.nexora.phase20;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.category.entity.Category;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.repository.OrderItemRepository;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.recommendation.dto.RecommendationResponse;
import com.nexora.recommendation.service.RecommendationServiceImpl;
import com.nexora.redis.service.RedisCacheService;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 20: RecommendationService Unit Tests")
class RecommendationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private UserPrincipal testUserPrincipal;
    private Store openStore;
    private Product pizzaProduct;
    private Product burgerProduct;
    private Category pizzaCategory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recommendationService, "cacheTtlSeconds", 900L);
        ReflectionTestUtils.setField(recommendationService, "defaultLimit", 10);

        User user = User.builder().id(42L).name("Test Gourmet").email("gourmet@nexora.com").build();
        testUserPrincipal = new UserPrincipal(user);

        openStore = Store.builder().id(100L).name("Mario Pizzeria").status(StoreStatus.OPEN).build();
        pizzaCategory = Category.builder().id(10L).name("Pizza").build();

        pizzaProduct = Product.builder()
                .id(1L)
                .name("Margherita Pizza")
                .description("Cheesy delight")
                .price(BigDecimal.valueOf(250.0))
                .discount(BigDecimal.valueOf(10.0))
                .available(true)
                .store(openStore)
                .categories(Set.of(pizzaCategory))
                .build();

        burgerProduct = Product.builder()
                .id(2L)
                .name("Veggie Crunch Burger")
                .description("Crispy patty")
                .price(BigDecimal.valueOf(180.0))
                .discount(BigDecimal.ZERO)
                .available(true)
                .store(openStore)
                .categories(Set.of(Category.builder().id(11L).name("Burger").build()))
                .build();
    }

    @Test
    @DisplayName("Personalized Recommendations: Cold-start fallback for new user with zero orders")
    void testColdStartFallbackWhenNoOrders() {
        when(redisCacheService.get(anyString(), eq(List.class))).thenReturn(Optional.empty());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(Collections.emptyList());
        when(orderItemRepository.findTopPurchasedProductIds(any())).thenReturn(Collections.emptyList());
        when(productRepository.findByAvailableTrue()).thenReturn(List.of(pizzaProduct, burgerProduct));

        List<RecommendationResponse> recommendations = recommendationService.getPersonalizedRecommendations(testUserPrincipal, 5);

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(r -> r.getReason().contains("Popular with Nexora foodies")));
    }

    @Test
    @DisplayName("Personalized Recommendations: Scores repurchase frequency and category affinity")
    void testPersonalizedRecommendationsWithOrderHistory() {
        when(redisCacheService.get(anyString(), eq(List.class))).thenReturn(Optional.empty());

        // Create historical order where user bought pizza
        OrderItem item = OrderItem.builder()
                .id(101L)
                .product(pizzaProduct)
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(250.0))
                .build();
        Order order = Order.builder()
                .id(501L)
                .store(openStore)
                .items(Set.of(item))
                .build();

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(order));
        when(productRepository.findByAvailableTrue()).thenReturn(List.of(pizzaProduct, burgerProduct));
        when(orderItemRepository.findTopPurchasedProductIds(any())).thenReturn(Collections.emptyList());

        List<RecommendationResponse> recommendations = recommendationService.getPersonalizedRecommendations(testUserPrincipal, 5);

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        RecommendationResponse top = recommendations.get(0);
        assertEquals(pizzaProduct.getId(), top.getProductId());
        assertTrue(top.getReason().contains("Frequently reordered by you") || top.getReason().contains("Pizza"));
        assertTrue(top.getScore() > 0);
    }

    @Test
    @DisplayName("Personalized Recommendations: Filters out products from closed stores")
    void testFiltersOutClosedStores() {
        when(redisCacheService.get(anyString(), eq(List.class))).thenReturn(Optional.empty());

        Store closedStore = Store.builder().id(200L).name("Closed Cafe").status(StoreStatus.CLOSED).build();
        Product closedStoreProduct = Product.builder()
                .id(99L)
                .name("Cold Coffee")
                .price(BigDecimal.valueOf(100.0))
                .available(true)
                .store(closedStore)
                .build();

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(Collections.emptyList());
        when(orderItemRepository.findTopPurchasedProductIds(any())).thenReturn(Collections.emptyList());
        when(productRepository.findByAvailableTrue()).thenReturn(List.of(closedStoreProduct));

        List<RecommendationResponse> recommendations = recommendationService.getPersonalizedRecommendations(testUserPrincipal, 5);

        assertTrue(recommendations.stream().noneMatch(r -> r.getProductId().equals(99L)));
    }

    @Test
    @DisplayName("Frequently Bought Together: Returns co-occurring products from order history")
    void testFrequentlyBoughtTogetherWithCoOccurrence() {
        when(redisCacheService.get(anyString(), eq(List.class))).thenReturn(Optional.empty());

        // Co-occurrence: Product 1 (Pizza) frequently bought with Product 2 (Burger) 8 times
        List<Object[]> coOccurring = Collections.singletonList(new Object[]{2L, 8L});
        when(orderItemRepository.findCoOccurringProductIds(eq(1L), any(PageRequest.class))).thenReturn(coOccurring);
        when(productRepository.findById(2L)).thenReturn(Optional.of(burgerProduct));

        List<RecommendationResponse> fbt = recommendationService.getFrequentlyBoughtTogether(1L, 5);

        assertNotNull(fbt);
        assertEquals(1, fbt.size());
        assertEquals(burgerProduct.getId(), fbt.get(0).getProductId());
        assertEquals("Customers often buy this together", fbt.get(0).getReason());
    }

    @Test
    @DisplayName("Trending Recommendations: Returns top purchased products")
    void testTrendingRecommendations() {
        when(redisCacheService.get(anyString(), eq(List.class))).thenReturn(Optional.empty());

        List<Object[]> topPurchased = Collections.singletonList(new Object[]{1L, 25L});
        when(orderItemRepository.findTopPurchasedProductIds(any(PageRequest.class))).thenReturn(topPurchased);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pizzaProduct));

        List<RecommendationResponse> trending = recommendationService.getTrendingRecommendations(5);

        assertNotNull(trending);
        assertFalse(trending.isEmpty());
        assertEquals(pizzaProduct.getId(), trending.get(0).getProductId());
        assertEquals("Best seller on Nexora", trending.get(0).getReason());
    }

    @Test
    @DisplayName("Category Recommendations: Returns available products in category")
    void testCategoryRecommendations() {
        when(productRepository.findAll(any(Specification.class))).thenReturn(List.of(pizzaProduct));

        List<RecommendationResponse> recs = recommendationService.getCategoryRecommendations("Pizza", 5);

        assertNotNull(recs);
        assertEquals(1, recs.size());
        assertEquals(pizzaProduct.getId(), recs.get(0).getProductId());
        assertTrue(recs.get(0).getReason().contains("Top rated in Pizza"));
    }

    @Test
    @DisplayName("Redis Cache Hit: Returns cached result without executing DB queries")
    void testRedisCacheHit() {
        RecommendationResponse cachedItem = RecommendationResponse.builder()
                .productId(1L)
                .productName("Cached Pizza")
                .score(99.0)
                .build();
        when(redisCacheService.get(contains("trending"), eq(List.class))).thenReturn(Optional.of(List.of(cachedItem)));

        List<RecommendationResponse> result = recommendationService.getTrendingRecommendations(5);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Cached Pizza", result.get(0).getProductName());
        verify(orderItemRepository, never()).findTopPurchasedProductIds(any());
    }
}
