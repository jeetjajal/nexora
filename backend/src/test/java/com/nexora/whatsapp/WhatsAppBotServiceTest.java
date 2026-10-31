package com.nexora.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import com.nexora.whatsapp.dto.WhatsAppBotResponse;
import com.nexora.whatsapp.entity.WhatsAppConversation;
import com.nexora.whatsapp.entity.WhatsAppMessage;
import com.nexora.whatsapp.repository.WhatsAppConversationRepository;
import com.nexora.whatsapp.repository.WhatsAppMessageRepository;
import com.nexora.whatsapp.service.WhatsAppBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 3 - WhatsAppBotService Multilingual NLP Tests")
class WhatsAppBotServiceTest {

    @Mock
    private WhatsAppConversationRepository conversationRepository;

    @Mock
    private WhatsAppMessageRepository messageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WhatsAppBotService botService;

    private Product potatoProduct;
    private Product milkProduct;
    private Product breadProduct;
    private Store testStore;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(botService, "frontendBaseUrl", "https://nexora-mu-eosin.vercel.app");

        potatoProduct = Product.builder()
                .id(101L)
                .name("Fresh Farm Potatoes (Batata / Aloo)")
                .price(new BigDecimal("35.00"))
                .available(true)
                .build();

        milkProduct = Product.builder()
                .id(102L)
                .name("Amul Taaza Homogenised Toned Milk 500ml")
                .price(new BigDecimal("27.00"))
                .available(true)
                .build();

        breadProduct = Product.builder()
                .id(103L)
                .name("Whole Wheat Sandwich Bread 400g")
                .price(new BigDecimal("45.00"))
                .available(true)
                .build();

        testStore = Store.builder().id(4L).name("SG Highway Dark Store Hub").build();
        testUser = User.builder().id(1L).name("Devendra Patel").phone("+919876543210").build();
    }

    @Test
    @DisplayName("Parses Gujarati text with quantity and regional terms (batata, dudh)")
    void testGujaratiOrdering() {
        when(conversationRepository.findByWaPhoneNumber("+919876543210"))
                .thenReturn(Optional.of(WhatsAppConversation.builder()
                        .id(1L)
                        .waPhoneNumber("+919876543210")
                        .customerName("Devendra Patel")
                        .currentState("IDLE")
                        .preferredLanguage("gu")
                        .lastActiveAt(LocalDateTime.now())
                        .build()));

        when(productRepository.findByNameContainingIgnoreCase("potato")).thenReturn(List.of(potatoProduct));
        when(productRepository.findByNameContainingIgnoreCase("milk")).thenReturn(List.of(milkProduct));

        WhatsAppBotResponse response = botService.processInboundMessage(
                "+919876543210", "Devendra Patel", "2 kilo batata ane 1 packet dudh moko"
        );

        assertNotNull(response);
        assertEquals("AWAITING_CONFIRMATION", response.getConversationState());
        assertEquals("gu", response.getDetectedLanguage());
        assertNotNull(response.getMatchedItems());
        assertEquals(2, response.getMatchedItems().size());

        // Verify potato item: 2 * 35 = 70
        var item0 = response.getMatchedItems().get(0);
        assertEquals("Fresh Farm Potatoes (Batata / Aloo)", item0.getProductName());
        assertEquals(2, item0.getQuantity());
        assertEquals(70.0, item0.getSubtotal());

        // Verify milk item: 1 * 27 = 27
        var item1 = response.getMatchedItems().get(1);
        assertEquals("Amul Taaza Homogenised Toned Milk 500ml", item1.getProductName());
        assertEquals(1, item1.getQuantity());
        assertEquals(27.0, item1.getSubtotal());

        assertTrue(response.getReplyMessage().contains("નેક્સોરા 10-મિનિટ ઓર્ડર સમરી"));
        verify(messageRepository, times(2)).save(any(WhatsAppMessage.class));
    }

    @Test
    @DisplayName("Parses Hindi text with regional terms (aalu, doodh)")
    void testHindiOrdering() {
        when(conversationRepository.findByWaPhoneNumber("+919876543210"))
                .thenReturn(Optional.of(WhatsAppConversation.builder()
                        .id(1L)
                        .waPhoneNumber("+919876543210")
                        .customerName("Rakesh Sharma")
                        .currentState("IDLE")
                        .preferredLanguage("hi")
                        .lastActiveAt(LocalDateTime.now())
                        .build()));

        when(productRepository.findByNameContainingIgnoreCase("potato")).thenReturn(List.of(potatoProduct));
        when(productRepository.findByNameContainingIgnoreCase("milk")).thenReturn(List.of(milkProduct));

        WhatsAppBotResponse response = botService.processInboundMessage(
                "+919876543210", "Rakesh Sharma", "1 packet doodh aur 2 kg aalu bhejo"
        );

        assertNotNull(response);
        assertEquals("AWAITING_CONFIRMATION", response.getConversationState());
        assertEquals("hi", response.getDetectedLanguage());
        assertTrue(response.getReplyMessage().contains("नेक्सोरा 10-मिनट आर्डर सारांश"));
    }

    @Test
    @DisplayName("Confirm intent converts drafted cart into real Order with Razorpay payment link")
    void testOrderConfirmationFlow() throws Exception {
        String draftJson = "{\"items\":[{\"productId\":101,\"productName\":\"Potatoes\",\"quantity\":2,\"unitPrice\":35.0,\"subtotal\":70.0}],"
                + "\"subtotal\":70.0,\"deliveryFee\":25.0,\"grandTotal\":95.0}";

        WhatsAppConversation conv = WhatsAppConversation.builder()
                .id(1L)
                .waPhoneNumber("+919876543210")
                .customerName("Devendra Patel")
                .user(testUser)
                .currentState("AWAITING_CONFIRMATION")
                .preferredLanguage("en")
                .sessionContext(draftJson)
                .lastActiveAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findByWaPhoneNumber("+919876543210")).thenReturn(Optional.of(conv));
        when(storeRepository.findById(4L)).thenReturn(Optional.of(testStore));
        when(productRepository.findById(101L)).thenReturn(Optional.of(potatoProduct));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(99201L);
            if (o.getItems() == null) o.setItems(new java.util.HashSet<>());
            return o;
        });

        WhatsAppBotResponse response = botService.processInboundMessage(
                "+919876543210", "Devendra Patel", "CONFIRM"
        );

        assertNotNull(response);
        assertEquals("ORDER_PLACED", response.getConversationState());
        assertEquals(99201L, response.getOrderId());
        assertNotNull(response.getPaymentUrl());
        assertTrue(response.getPaymentUrl().contains("checkoutOrder=99201"));
        assertTrue(response.getReplyMessage().contains("10 minutes"));
    }

    @Test
    @DisplayName("Cancel intent discards drafted order context and resets to IDLE")
    void testOrderCancellationFlow() {
        WhatsAppConversation conv = WhatsAppConversation.builder()
                .id(1L)
                .waPhoneNumber("+919876543210")
                .customerName("Devendra Patel")
                .currentState("AWAITING_CONFIRMATION")
                .preferredLanguage("en")
                .sessionContext("{\"items\":[]}")
                .lastActiveAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findByWaPhoneNumber("+919876543210")).thenReturn(Optional.of(conv));

        WhatsAppBotResponse response = botService.processInboundMessage(
                "+919876543210", "Devendra Patel", "cancel"
        );

        assertNotNull(response);
        assertEquals("IDLE", response.getConversationState());
        assertNull(conv.getSessionContext());
        assertTrue(response.getReplyMessage().contains("cancelled"));
    }
}
