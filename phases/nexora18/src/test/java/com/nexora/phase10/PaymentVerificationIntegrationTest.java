package com.nexora.phase10;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PHASE 10: the complete verification and webhook flow, through the
 * real Spring Security chain and real HMAC-SHA256 computation (no
 * mocking of RazorpaySignatureVerifier here — signatures are computed
 * the same way a real Razorpay server would, using the fixed test
 * secrets from src/test/resources/application.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 10: Payment verification and webhook end-to-end tests")
class PaymentVerificationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    private String customerToken;
    private User owner;

    @BeforeEach
    void seedUsersAndTokens() {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CUSTOMER).build()));
        Role storeOwnerRole = roleRepository.findByName(RoleName.STORE_OWNER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STORE_OWNER).build()));

        User customer = seedUser("customer", customerRole);
        owner = seedUser("owner", storeOwnerRole);

        customerToken = jwtService.generateToken(new UserPrincipal(customer));
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
                .name("Verify Test Store " + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.OPEN)
                .build());

        Product product = productRepository.save(Product.builder()
                .store(store)
                .name("Verify Test Product " + UUID.randomUUID().toString().substring(0, 8))
                .price(price)
                .available(true)
                .build());

        inventoryRepository.save(Inventory.builder().product(product).quantity(stock).build());
        return product;
    }

    /** Places a real order and initiates payment for it, returning [orderId, paymentId, razorpayOrderId]. */
    private String[] placeOrderAndInitiatePayment(BigDecimal price) throws Exception {
        Product product = seedProduct(price, 10);

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

        String checkoutJson = mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deliveryAddressId", addressId))))
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(checkoutJson).path("data").get(0).path("id").asText();

        String paymentJson = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentJson).path("data").path("id").asText();
        String razorpayOrderId = objectMapper.readTree(paymentJson).path("data").path("razorpayOrderId").asText();

        return new String[]{orderId, paymentId, razorpayOrderId};
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
    // Client-callback signature — the genuine success path
    // ==============================================================

    @Test
    @DisplayName("A VALID client-callback signature confirms the payment: SUCCESS + order PAID")
    void validClientSignatureConfirmsPayment() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("100.00"));
        String orderId = ids[0], paymentId = ids[1], razorpayOrderId = ids[2];

        String razorpayPaymentId = "pay_valid_" + UUID.randomUUID().toString().substring(0, 8);
        String validSignature = hmacSha256Hex(razorpayOrderId + "|" + razorpayPaymentId, keySecret);

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/result")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "razorpayPaymentId", razorpayPaymentId,
                                "razorpaySignature", validSignature))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("Calling the client-result endpoint twice with the same valid signature is safe (idempotent)")
    void repeatedValidClientSignatureIsIdempotent() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("100.00"));
        String orderId = ids[0], paymentId = ids[1], razorpayOrderId = ids[2];

        String razorpayPaymentId = "pay_repeat_" + UUID.randomUUID().toString().substring(0, 8);
        String validSignature = hmacSha256Hex(razorpayOrderId + "|" + razorpayPaymentId, keySecret);

        Map<String, Object> body = Map.of("razorpayPaymentId", razorpayPaymentId, "razorpaySignature", validSignature);

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/result")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Second call with the identical valid signature must not error
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/result")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    // ==============================================================
    // Webhook — the resilient, authoritative confirmation path
    // ==============================================================

    @Test
    @DisplayName("A VALID webhook 'payment.captured' event confirms the payment: SUCCESS + order PAID")
    void validWebhookCapturedConfirmsPayment() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("150.00"));
        String orderId = ids[0], razorpayOrderId = ids[2];
        String razorpayPaymentId = "pay_webhook_" + UUID.randomUUID().toString().substring(0, 8);

        String body = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"%s\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayPaymentId, razorpayOrderId);
        String signature = hmacSha256Hex(body, webhookSecret);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("A VALID webhook 'payment.failed' event marks the payment FAILED but does NOT cancel the order")
    void validWebhookFailedMarksPaymentFailedOnly() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("80.00"));
        String orderId = ids[0], razorpayOrderId = ids[2];
        String razorpayPaymentId = "pay_failed_" + UUID.randomUUID().toString().substring(0, 8);

        String body = ("{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"%s\",\"order_id\":\"%s\",\"status\":\"failed\"}}}}")
                .formatted(razorpayPaymentId, razorpayOrderId);
        String signature = hmacSha256Hex(body, webhookSecret);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Order is untouched — still PAYMENT_PENDING, free to retry, NOT auto-cancelled
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"));
    }

    @Test
    @DisplayName("A webhook with an INVALID signature is rejected with 400, and nothing changes")
    void invalidWebhookSignatureRejected() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("60.00"));
        String orderId = ids[0], razorpayOrderId = ids[2];

        String body = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_fake\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayOrderId);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", "totally-fabricated-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        // Order remains completely untouched
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"));
    }

    @Test
    @DisplayName("A webhook whose body is tampered with AFTER signing is rejected (signature no longer matches)")
    void tamperedWebhookBodyRejected() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("60.00"));
        String razorpayOrderId = ids[2];

        String originalBody = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_x\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayOrderId);
        String signatureForOriginal = hmacSha256Hex(originalBody, webhookSecret);

        // Send a DIFFERENT body than the one the signature was computed for
        String tamperedBody = originalBody.replace("payment.captured", "payment.failed");

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signatureForOriginal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A duplicate webhook delivery of the same captured event is handled safely (idempotent, no error)")
    void duplicateWebhookHandledSafely() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("70.00"));
        String orderId = ids[0], razorpayOrderId = ids[2];
        String razorpayPaymentId = "pay_dup_" + UUID.randomUUID().toString().substring(0, 8);

        String body = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"%s\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayPaymentId, razorpayOrderId);
        String signature = hmacSha256Hex(body, webhookSecret);

        // First delivery
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Razorpay redelivers the SAME event (at-least-once delivery guarantee)
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()); // must NOT error on the duplicate

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.data.status").value("PAID")); // still correctly PAID, not corrupted
    }

    @Test
    @DisplayName("A validly-signed webhook for an unknown Razorpay order id returns 404")
    void webhookForUnknownOrderReturns404() throws Exception {
        String body = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_x\",\"order_id\":\"order_does_not_exist_anywhere\",\"status\":\"captured\"}}}}";
        String signature = hmacSha256Hex(body, webhookSecret);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The webhook endpoint requires no JWT (public, secured by signature instead)")
    void webhookEndpointRequiresNoJwt() throws Exception {
        String[] ids = placeOrderAndInitiatePayment(new BigDecimal("60.00"));
        String razorpayOrderId = ids[2];

        String body = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_nojwt\",\"order_id\":\"%s\",\"status\":\"captured\"}}}}")
                .formatted(razorpayOrderId);
        String signature = hmacSha256Hex(body, webhookSecret);

        // Deliberately NO Authorization header at all
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()); // succeeds without any JWT
    }
}
