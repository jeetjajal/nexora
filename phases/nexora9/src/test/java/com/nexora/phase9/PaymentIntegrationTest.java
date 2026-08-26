package com.nexora.phase9;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.security.JwtService;
import com.nexora.auth.security.UserPrincipal;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PHASE 9: full payment-initiation flow through the real Spring
 * Security chain (JWT, Phase 3) and the real Order/Cart/Address
 * machinery (Phases 2, 7, 8), with StubPaymentGatewayClient standing
 * in for Razorpay itself (see that class's javadoc — active under the
 * "test" profile, no real network call ever happens in this suite).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 9: Payment initiation end-to-end tests")
class PaymentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private String customerToken;
    private String customerTwoToken;
    private User owner;

    @BeforeEach
    void seedUsersAndTokens() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));

        User customer = seedUser("customer", customerRole);
        User customerTwo = seedUser("customer-two", customerRole);
        owner = seedUser("owner", storeOwnerRole);

        customerToken = jwtService.generateToken(new UserPrincipal(customer));
        customerTwoToken = jwtService.generateToken(new UserPrincipal(customerTwo));
    }

    private User seedUser(String prefix, Role role) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .name(prefix + " test user")
                .email(prefix + "." + unique + "@example.com")
                .password(passwordEncoder.encode("Password123"))
                .phone("9000000000")
                .status("ACTIVE")
                .roles(Set.of(role))
                .build());
    }

    private Product seedProduct(BigDecimal price, int stock) {
        Store store = storeRepository.save(Store.builder()
                .owner(owner)
                .name("Payment Test Store " + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.OPEN)
                .build());

        Product product = productRepository.save(Product.builder()
                .store(store)
                .name("Payment Test Product " + UUID.randomUUID().toString().substring(0, 8))
                .price(price)
                .available(true)
                .build());

        inventoryRepository.save(Inventory.builder().product(product).quantity(stock).build());
        return product;
    }

    /** Places a real order for the customer and returns its id. */
    private Long placeOrder(String customerAuthToken, BigDecimal price) throws Exception {
        Product product = seedProduct(price, 10);

        String addressJson = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "1 Test Street", "city", "Rajkot",
                                "state", "Gujarat", "pincode", "360001"))))
                .andReturn().getResponse().getContentAsString();
        Long addressId = objectMapper.readTree(addressJson).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asLong();
    }

    // ==============================================================
    // Happy path
    // ==============================================================

    @Test
    @DisplayName("Initiating payment creates a Payment, returns gateway details, and moves order to PAYMENT_PENDING")
    void initiatePaymentHappyPath() throws Exception {
        Long orderId = placeOrder(customerToken, new BigDecimal("150.00"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.razorpayOrderId").value(org.hamcrest.Matchers.startsWith("order_test_")))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.razorpayKeyId").exists())
                .andExpect(jsonPath("$.data.amount").value(190.00)); // 150 + 40 delivery fee

        // The underlying order really did transition
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"));
    }

    @Test
    @DisplayName("Calling initiate-payment twice for the same order returns the SAME payment, not a duplicate")
    void initiatingPaymentTwiceIsIdempotent() throws Exception {
        Long orderId = placeOrder(customerToken, new BigDecimal("80.00"));

        String firstJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String firstRazorpayOrderId = objectMapper.readTree(firstJson).path("data").path("razorpayOrderId").asText();

        String secondJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String secondRazorpayOrderId = objectMapper.readTree(secondJson).path("data").path("razorpayOrderId").asText();

        org.assertj.core.api.Assertions.assertThat(secondRazorpayOrderId).isEqualTo(firstRazorpayOrderId);

        // Only ONE payment attempt exists for this order
        mockMvc.perform(get("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("A different customer cannot initiate payment for someone else's order")
    void otherCustomerCannotInitiatePayment() throws Exception {
        Long orderId = placeOrder(customerToken, new BigDecimal("60.00"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerTwoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Initiating payment without a token returns 401")
    void initiatingPaymentWithoutTokenReturns401() throws Exception {
        Long orderId = placeOrder(customerToken, new BigDecimal("60.00"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments"))
                .andExpect(status().isUnauthorized());
    }

    // ==============================================================
    // Client-reported result — never trusted for SUCCESS
    // ==============================================================

    @Test
    @DisplayName("Recording a client-reported result succeeds but the order status does NOT advance to PAID")
    void clientReportedResultDoesNotAdvanceOrder() throws Exception {
        Long orderId = placeOrder(customerToken, new BigDecimal("100.00"));

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        Long paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/result")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "razorpayPaymentId", "pay_fake_client_reported",
                                "razorpaySignature", "totally_fake_signature"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING")); // still PENDING, not SUCCESS

        // The order itself is still just PAYMENT_PENDING, never PAID —
        // the client's claim alone changes nothing about fulfillment state.
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"));
    }

    @Test
    @DisplayName("A different customer cannot report a result for someone else's payment")
    void otherCustomerCannotReportResult() throws Exception {
        Long orderId = placeOrder(customerToken, new BigDecimal("100.00"));

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        Long paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/result")
                        .header("Authorization", "Bearer " + customerTwoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "razorpayPaymentId", "pay_x", "razorpaySignature", "sig_x"))))
                .andExpect(status().isForbidden());
    }

    // ==============================================================
    // Visibility
    // ==============================================================

    @Test
    @DisplayName("The order's store owner can view its payments; a different customer cannot")
    void paymentVisibility() throws Exception {
        Long orderId = placeOrder(customerToken, new BigDecimal("50.00"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated());

        String ownerToken = jwtService.generateToken(new UserPrincipal(owner));

        mockMvc.perform(get("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerTwoToken))
                .andExpect(status().isForbidden());
    }
}
