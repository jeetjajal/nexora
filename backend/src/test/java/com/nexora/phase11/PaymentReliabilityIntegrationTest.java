package com.nexora.phase11;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PHASE 11: payment retry, refund, and Idempotency-Key deduplication —
 * the complete flow through the real Spring Security chain, real
 * database state, and (for the webhook-driven scenarios) real
 * HMAC-SHA256 computation, same as Phase 10's verification tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 11: Payment reliability end-to-end tests")
class PaymentReliabilityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    private String customerToken;
    private String customerTwoToken;
    private String ownerToken;
    private String otherOwnerToken;
    private String adminToken;
    private User owner;

    @BeforeEach
    void seedUsersAndTokens() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));

        User customer = seedUser("customer", customerRole);
        User customerTwo = seedUser("customer-two", customerRole);
        owner = seedUser("owner", storeOwnerRole);
        User otherOwner = seedUser("other-owner", storeOwnerRole);
        User admin = seedUser("admin", adminRole);

        customerToken = jwtService.generateToken(new UserPrincipal(customer));
        customerTwoToken = jwtService.generateToken(new UserPrincipal(customerTwo));
        ownerToken = jwtService.generateToken(new UserPrincipal(owner));
        otherOwnerToken = jwtService.generateToken(new UserPrincipal(otherOwner));
        adminToken = jwtService.generateToken(new UserPrincipal(admin));
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
                .name("Reliability Test Store " + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.OPEN)
                .build());

        Product product = productRepository.save(Product.builder()
                .store(store)
                .name("Reliability Test Product " + UUID.randomUUID().toString().substring(0, 8))
                .price(price)
                .available(true)
                .build());

        inventoryRepository.save(Inventory.builder().product(product).quantity(stock).build());
        return product;
    }

    private String[] placeOrder(String customerAuthToken, BigDecimal price) throws Exception {
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
        String orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asText();
        Long productId = product.getId();

        return new String[]{orderId, String.valueOf(productId), String.valueOf(addressId)};
    }

    private String hmacSha256Hex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ==============================================================
    // Payment retry after failure — the Phase 11 bug fix
    // ==============================================================

    @Test
    @DisplayName("After a failed payment, the customer CAN retry - a fresh Payment attempt succeeds")
    void customerCanRetryAfterFailedPayment() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("100.00"));
        String orderId = ids[0];

        String firstPaymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstRazorpayOrderId = objectMapper.readTree(firstPaymentJson).path("data").path("razorpayOrderId").asText();

        String failedBody = ("{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_failed_1\",\"order_id\":\"%s\",\"status\":\"failed\"}}}}")
                .formatted(firstRazorpayOrderId);
        String failedSignature = hmacSha256Hex(failedBody, webhookSecret);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", failedSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failedBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"));

        String secondPaymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondRazorpayOrderId = objectMapper.readTree(secondPaymentJson).path("data").path("razorpayOrderId").asText();

        assertThat(secondRazorpayOrderId).isNotEqualTo(firstRazorpayOrderId);

        String successBody = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_success_1\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(secondRazorpayOrderId);
        String successSignature = hmacSha256Hex(successBody, webhookSecret);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", successSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(successBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(get("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ==============================================================
    // Idempotency-Key - checkout
    // ==============================================================

    @Test
    @DisplayName("Retrying checkout with the SAME Idempotency-Key returns the identical result, no duplicate orders")
    void repeatedCheckoutWithSameKeyDoesNotDuplicate() throws Exception {
        Product product = seedProduct(new BigDecimal("50.00"), 10);
        String idempotencyKey = UUID.randomUUID().toString();

        String addressJson = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "1 Test Street", "city", "Rajkot",
                                "state", "Gujarat", "pincode", "360001"))))
                .andReturn().getResponse().getContentAsString();
        Long addressId = objectMapper.readTree(addressJson).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        Map<String, Object> checkoutBody = Map.of("deliveryAddressId", addressId);

        String firstResponse = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstOrderId = objectMapper.readTree(firstResponse).path("data").get(0).path("id").asText();

        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(9);

        String secondResponse = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondOrderId = objectMapper.readTree(secondResponse).path("data").get(0).path("id").asText();

        assertThat(secondOrderId).isEqualTo(firstOrderId);
        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(9);

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("Checkout WITHOUT an Idempotency-Key header behaves exactly as before Phase 11 (no tracking)")
    void checkoutWithoutKeyBehavesAsBefore() throws Exception {
        Product product = seedProduct(new BigDecimal("30.00"), 10);

        String addressJson = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "1 Test Street", "city", "Rajkot",
                                "state", "Gujarat", "pincode", "360001"))))
                .andReturn().getResponse().getContentAsString();
        Long addressId = objectMapper.readTree(addressJson).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("Reusing an Idempotency-Key with a DIFFERENT deliveryAddressId is rejected as a conflict")
    void reusingKeyWithDifferentAddressRejected() throws Exception {
        Product product = seedProduct(new BigDecimal("40.00"), 10);
        String idempotencyKey = UUID.randomUUID().toString();

        String addressOneJson = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "Address One", "city", "Rajkot",
                                "state", "Gujarat", "pincode", "360001"))))
                .andReturn().getResponse().getContentAsString();
        Long addressOneId = objectMapper.readTree(addressOneJson).path("data").path("id").asLong();

        String addressTwoJson = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressLine1", "Address Two", "city", "Rajkot",
                                "state", "Gujarat", "pincode", "360002"))))
                .andReturn().getResponse().getContentAsString();
        Long addressTwoId = objectMapper.readTree(addressTwoJson).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", product.getId(), "quantity", 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressOneId))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressTwoId))))
                .andExpect(status().isConflict());
    }

    // ==============================================================
    // Idempotency-Key - payment initiation
    // ==============================================================

    @Test
    @DisplayName("Retrying payment initiation with the SAME Idempotency-Key returns the identical payment")
    void repeatedPaymentInitiationWithSameKeyDoesNotDuplicate() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("90.00"));
        String orderId = ids[0];
        String idempotencyKey = UUID.randomUUID().toString();

        String firstResponse = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstRazorpayOrderId = objectMapper.readTree(firstResponse).path("data").path("razorpayOrderId").asText();

        String secondResponse = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondRazorpayOrderId = objectMapper.readTree(secondResponse).path("data").path("razorpayOrderId").asText();

        assertThat(secondRazorpayOrderId).isEqualTo(firstRazorpayOrderId);

        mockMvc.perform(get("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ==============================================================
    // Refund flow
    // ==============================================================

    @Test
    @DisplayName("STORE_OWNER can refund a SUCCESS payment: payment REFUNDED, order CANCELLED, stock restored")
    void storeOwnerCanRefundSuccessfulPayment() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("120.00"));
        String orderId = ids[0];
        Long productId = Long.valueOf(ids[1]);

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asText();
        String razorpayOrderId = objectMapper.readTree(paymentJson).path("data").path("razorpayOrderId").asText();

        String capturedBody = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_refund_test\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayOrderId);
        String signature = hmacSha256Hex(capturedBody, webhookSecret);
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedBody))
                .andExpect(status().isOk());

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantity()).isEqualTo(9);

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("A CUSTOMER cannot refund their own payment - refunds are store/admin-authorized only")
    void customerCannotSelfRefund() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("60.00"));
        String orderId = ids[0];

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A DIFFERENT store owner cannot refund a payment for an order that isn't theirs")
    void otherStoreOwnerCannotRefund() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("60.00"));
        String orderId = ids[0];

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asText();
        String razorpayOrderId = objectMapper.readTree(paymentJson).path("data").path("razorpayOrderId").asText();

        String capturedBody = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_x\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayOrderId);
        String signature = hmacSha256Hex(capturedBody, webhookSecret);
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + otherOwnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can refund any payment")
    void adminCanRefundAnyPayment() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("60.00"));
        String orderId = ids[0];

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asText();
        String razorpayOrderId = objectMapper.readTree(paymentJson).path("data").path("razorpayOrderId").asText();

        String capturedBody = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_admin_refund\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayOrderId);
        String signature = hmacSha256Hex(capturedBody, webhookSecret);
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("A payment that hasn't succeeded yet cannot be refunded")
    void cannotRefundNonSuccessfulPayment() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("60.00"));
        String orderId = ids[0];

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A payment cannot be refunded twice")
    void cannotDoubleRefund() throws Exception {
        String[] ids = placeOrder(customerToken, new BigDecimal("60.00"));
        String orderId = ids[0];

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asText();
        String razorpayOrderId = objectMapper.readTree(paymentJson).path("data").path("razorpayOrderId").asText();

        String capturedBody = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_double_refund\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayOrderId);
        String signature = hmacSha256Hex(capturedBody, webhookSecret);
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(capturedBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict());
    }
}
