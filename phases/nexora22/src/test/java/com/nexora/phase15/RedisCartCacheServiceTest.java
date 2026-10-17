package com.nexora.phase15;

import com.nexora.cart.dto.CartItemResponse;
import com.nexora.cart.dto.CartResponse;
import com.nexora.redis.service.RedisCacheService;
import com.nexora.redis.service.RedisCartCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 15 - RedisCartCacheService Unit Tests")
class RedisCartCacheServiceTest {

    @Mock
    private RedisCacheService cacheService;

    private RedisCartCacheService cartCacheService;

    @BeforeEach
    void setUp() {
        cartCacheService = new RedisCartCacheService(cacheService);
        ReflectionTestUtils.setField(cartCacheService, "cartTtlSeconds", 1800L);
    }

    @Test
    @DisplayName("Should cache user cart with TTL")
    void testCacheCart() {
        CartResponse cart = CartResponse.builder()
                .id(1L)
                .totalItemCount(2)
                .subtotal(new BigDecimal("150.00"))
                .items(List.of(CartItemResponse.builder().id(10L).quantity(2).build()))
                .build();

        cartCacheService.cacheCart(42L, cart);

        verify(cacheService).set("nexora:cart:user:42", cart, 1800L);
    }

    @Test
    @DisplayName("Should retrieve cached cart if present")
    void testGetCachedCart() {
        CartResponse cart = CartResponse.builder().id(1L).build();
        when(cacheService.get("nexora:cart:user:42", CartResponse.class)).thenReturn(Optional.of(cart));

        Optional<CartResponse> result = cartCacheService.getCachedCart(42L);

        assertThat(result).isPresent().contains(cart);
    }

    @Test
    @DisplayName("Should evict user cart from cache")
    void testEvictCart() {
        cartCacheService.evictCart(42L);
        verify(cacheService).delete("nexora:cart:user:42");
    }
}
