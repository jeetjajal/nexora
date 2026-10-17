package com.nexora.phase21;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.concurrency.dto.ConcurrencySimulationRequest;
import com.nexora.concurrency.model.ConcurrencyStrategy;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 21: Concurrency Endpoints SpringBoot Integration Test")
class ConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        User merchant = userRepository.save(User.builder()
                .name("Integration Merchant")
                .email("int_merchant_" + UUID.randomUUID() + "@nexora.test")
                .password("encoded_pass")
                .phone("9112233445")
                .build());

        Store store = storeRepository.save(Store.builder()
                .name("Integration Store")
                .owner(merchant)
                .status(StoreStatus.OPEN)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Test Flash Item")
                .description("Item for integration test")
                .price(BigDecimal.valueOf(999.0))
                .store(store)
                .available(true)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .quantity(5)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/concurrency/strategies: public endpoint returns 200 OK with list")
    void testGetStrategiesEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/concurrency/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].strategy").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/concurrency/simulate: runs end-to-end benchmark and returns 200 OK")
    void testSimulateEndpoint() throws Exception {
        ConcurrencySimulationRequest request = ConcurrencySimulationRequest.builder()
                .productId(testProduct.getId())
                .initialStock(2)
                .totalThreads(10)
                .strategy(ConcurrencyStrategy.PESSIMISTIC_LOCK)
                .build();

        mockMvc.perform(post("/api/v1/concurrency/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.initialStock").value(2))
                .andExpect(jsonPath("$.successfulPurchases").value(2))
                .andExpect(jsonPath("$.failedPurchases").value(8))
                .andExpect(jsonPath("$.finalStock").value(0))
                .andExpect(jsonPath("$.oversellingPrevented").value(true));
    }

    @Test
    @DisplayName("GET /concurrency.html: simulation web page is accessible publicly (200 OK)")
    void testConcurrencyStaticPageAccessible() throws Exception {
        mockMvc.perform(get("/concurrency.html"))
                .andExpect(status().isOk());
    }
}
