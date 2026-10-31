package com.nexora.whatsapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.address.entity.Address;
import com.nexora.order.entity.Order;
import com.nexora.order.entity.OrderItem;
import com.nexora.order.entity.OrderStatus;
import com.nexora.order.repository.OrderRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.role.entity.Role;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import com.nexora.whatsapp.dto.WhatsAppBotResponse;
import com.nexora.whatsapp.dto.WhatsAppConversationSummaryResponse;
import com.nexora.whatsapp.entity.WhatsAppConversation;
import com.nexora.whatsapp.entity.WhatsAppMessage;
import com.nexora.whatsapp.repository.WhatsAppConversationRepository;
import com.nexora.whatsapp.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppBotService {

    private final WhatsAppConversationRepository conversationRepository;
    private final WhatsAppMessageRepository messageRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    @Value("${nexora.frontend.url:https://nexora-mu-eosin.vercel.app}")
    private String frontendBaseUrl;

    // Multilingual Dictionary mapping regional terms to canonical catalogue search keys
    private static final Map<String, String> GUJARATI_LEXICON = Map.ofEntries(
            Map.entry("batata", "potato"),
            Map.entry("bataka", "potato"),
            Map.entry("tameta", "tomato"),
            Map.entry("tamata", "tomato"),
            Map.entry("dudh", "milk"),
                        Map.entry("kanda", "onion"),
            Map.entry("dungri", "onion"),
            Map.entry("cha", "tea"),
            Map.entry("kela", "banana"),
            Map.entry("safarchand", "apple"),
            Map.entry("dahi", "curd"),
            Map.entry("makhan", "butter"),
            Map.entry("paneer", "paneer"),
            Map.entry("bread", "bread"),
            Map.entry("biscuit", "biscuit"),
            Map.entry("tel", "oil"),
            Map.entry("chawal", "rice"),
            Map.entry("bhat", "rice")
    );

    private static final Map<String, String> HINDI_LEXICON = Map.ofEntries(
            Map.entry("aalu", "potato"),
            Map.entry("aloo", "potato"),
            Map.entry("tamatar", "tomato"),
            Map.entry("doodh", "milk"),
            Map.entry("pyaz", "onion"),
            Map.entry("chai", "tea"),
            Map.entry("kela", "banana"),
            Map.entry("seb", "apple"),
            Map.entry("dahi", "curd"),
            Map.entry("makkhan", "butter"),
            Map.entry("paneer", "paneer"),
            Map.entry("bread", "bread"),
            Map.entry("cheeni", "sugar"),
            Map.entry("namak", "salt"),
            Map.entry("tel", "oil"),
            Map.entry("chawal", "rice"),
            Map.entry("atta", "flour")
    );

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)\\s*(kilo|kg|packet|pack|l|liter|litre|gm|g|unit|nag|doz|dozen)?", Pattern.CASE_INSENSITIVE);

    @Transactional
    public WhatsAppBotResponse processInboundMessage(String senderPhone, String senderName, String messageText) {
        String cleanPhone = senderPhone.replaceAll("[^0-9+]", "");
        String rawText = (messageText != null) ? messageText.trim() : "";
        String lowerText = rawText.toLowerCase();

        // 1. Get or create conversation session
        WhatsAppConversation conversation = conversationRepository.findByWaPhoneNumber(cleanPhone)
                .orElseGet(() -> {
                    User user = userRepository.findByPhone(cleanPhone).orElse(null);
                    return conversationRepository.save(WhatsAppConversation.builder()
                            .waPhoneNumber(cleanPhone)
                            .customerName(senderName != null ? senderName : "Valued Customer")
                            .user(user)
                            .currentState("IDLE")
                            .preferredLanguage(detectLanguage(rawText))
                            .lastActiveAt(LocalDateTime.now())
                            .build());
                });

        if (senderName != null && !senderName.isBlank() && "Valued Customer".equals(conversation.getCustomerName())) {
            conversation.setCustomerName(senderName);
        }

        // Update language if detected with high confidence
        String detectedLang = detectLanguage(rawText);
        if (!"en".equals(detectedLang) || "en".equals(conversation.getPreferredLanguage())) {
            conversation.setPreferredLanguage(detectedLang);
        }

        // Log inbound message
        messageRepository.save(WhatsAppMessage.builder()
                .conversation(conversation)
                .direction("INBOUND")
                .messageType("TEXT")
                .messageBody(rawText)
                .build());

        WhatsAppBotResponse response;

        // 2. Intent Routing & State Machine
        if (isGreeting(lowerText)) {
            response = handleGreeting(conversation);
        } else if (isOrderConfirmation(lowerText) && "AWAITING_CONFIRMATION".equals(conversation.getCurrentState())) {
            response = handleOrderConfirmation(conversation);
        } else if (isOrderCancellation(lowerText)) {
            response = handleOrderCancellation(conversation);
        } else if (isTrackingInquiry(lowerText)) {
            response = handleTrackingInquiry(conversation);
        } else {
            // Default: Attempt to parse grocery items from the message
            response = handleProductParsingAndDrafting(conversation, rawText);
        }

        // Log outbound message response
        messageRepository.save(WhatsAppMessage.builder()
                .conversation(conversation)
                .direction("OUTBOUND")
                .messageType(response.getPaymentUrl() != null ? "ORDER_LINK" : "TEXT")
                .messageBody(response.getReplyMessage())
                .build());

        conversation.setLastActiveAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return response;
    }

    private boolean isGreeting(String text) {
        return text.matches(".*\\b(hi|hello|hey|namaste|kem\\s*chho|kem\\s*cho|kemcho|ram\\s*ram|pranam|start|menu)\\b.*");
    }

    private boolean isOrderConfirmation(String text) {
        return text.matches(".*\\b(confirm|yes|ha|haan|sachu|ok|done|thik|theek|ha\\s*bhai|pay|proceed)\\b.*");
    }

    private boolean isOrderCancellation(String text) {
        return text.matches(".*\\b(cancel|no|na|nahi|nathi|stop|cancel\\s*order|reject)\\b.*");
    }

    private boolean isTrackingInquiry(String text) {
        return text.matches(".*\\b(track|status|order\\s*status|kahan\\s*hai|kyan\\s*chhe|where\\s*is|delivery)\\b.*");
    }

    private WhatsAppBotResponse handleGreeting(WhatsAppConversation conversation) {
        conversation.setCurrentState("IDLE");
        String lang = conversation.getPreferredLanguage();
        String reply;

        if ("gu".equals(lang)) {
            reply = "🙏 *નમસ્તે " + conversation.getCustomerName() + "! નેક્સોરા ક્વિક કોમર્સમાં સ્વાગત છે.*\n\n"
                    + "હું તમારો AI ગ્રોસરી સહાયક છું. તમે મને ગુજરાતીમાં સીધો ઓર્ડર લખી શકો છો:\n"
                    + "👉 *ઉદાહરણ:* '2 કિલો બટાટા અને 1 પેકેટ અમુલ તાજા દૂધ મોકલો'\n\n"
                    + "તમારા ઓર્ડરનું લાઈવ સ્ટેટસ જાણવા માટે *'TRACK'* લખો.";
        } else if ("hi".equals(lang)) {
            reply = "🙏 *नमस्ते " + conversation.getCustomerName() + "! नेक्सोरा 10-मिनट डिलीवरी में आपका स्वागत है.*\n\n"
                    + "आप सीधे अपनी भाषा में राशन का आर्डर लिख सकते हैं:\n"
                    + "👉 *उदाहरण:* '1 पैकेट दूध, 2 किलो आलू और 1 किलो टमाटर भेजो'\n\n"
                    + "अपने चालू आर्डर की स्थिति देखने के लिए *'TRACK'* लिखें.";
        } else {
            reply = "⚡ *Hello " + conversation.getCustomerName() + "! Welcome to NEXORA 10-Minute Quick Commerce.*\n\n"
                    + "You can order directly via chat in English, Hindi, or Gujarati:\n"
                    + "👉 *Example:* 'Send 1L Amul Milk, 2kg Potatoes, and 1 loaf Bread'\n\n"
                    + "Reply *'TRACK'* to view your active order or *'MENU'* for catalogue categories.";
        }

        return WhatsAppBotResponse.builder()
                .replyMessage(reply)
                .conversationState("IDLE")
                .detectedLanguage(lang)
                .build();
    }

    private WhatsAppBotResponse handleOrderCancellation(WhatsAppConversation conversation) {
        conversation.setCurrentState("IDLE");
        conversation.setSessionContext(null);

        String lang = conversation.getPreferredLanguage();
        String reply;

        if ("gu".equals(lang)) {
            reply = "❌ *તમારો ડ્રાફ્ટ ઓર્ડર રદ કરવામાં આવ્યો છે.*\nજ્યારે પણ જરૂર હોય ત્યારે નવો ઓર્ડર લખી મોકલો.";
        } else if ("hi".equals(lang)) {
            reply = "❌ *आपका ड्राफ्ट आर्डर रद्द कर दिया गया है.*\nजब भी ज़रूरत हो, नया आर्डर लिखकर भेजें.";
        } else {
            reply = "❌ *Your drafted order has been cancelled.*\nFeel free to type your grocery requirements whenever you are ready.";
        }

        return WhatsAppBotResponse.builder()
                .replyMessage(reply)
                .conversationState("IDLE")
                .detectedLanguage(lang)
                .build();
    }

    private WhatsAppBotResponse handleTrackingInquiry(WhatsAppConversation conversation) {
        String lang = conversation.getPreferredLanguage();
        List<Order> userOrders = Collections.emptyList();

        if (conversation.getUser() != null) {
            userOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(conversation.getUser().getId());
        }

        if (userOrders.isEmpty()) {
            String msg = "gu".equals(lang)
                    ? "🔍 તમારા મોબાઈલ નંબર પર કોઈ સક્રિય ઓર્ડર મળ્યો નથી."
                    : "hi".equals(lang)
                    ? "🔍 आपके नंबर पर कोई सक्रिय आर्डर नहीं मिला."
                    : "🔍 No active orders found linked to your phone number.";
            return WhatsAppBotResponse.builder()
                    .replyMessage(msg)
                    .conversationState(conversation.getCurrentState())
                    .detectedLanguage(lang)
                    .build();
        }

        Order latest = userOrders.get(0);
        String trackUrl = frontendBaseUrl + "/?trackingOrder=" + latest.getId();

        String reply;
        if ("gu".equals(lang)) {
            reply = "📦 *ઓર્ડર સ્ટેટસ (NX-" + latest.getId() + "):*\n"
                    + "• સ્થિતિ: *" + latest.getStatus() + "*\n"
                    + "• રકમ: *₹" + latest.getTotalAmount() + "*\n"
                    + "• ડિલિવરી હબ: *" + latest.getStore().getName() + "*\n\n"
                    + "⚡ લાઈવ GPS ટ્રેકિંગ મેપ:\n" + trackUrl;
        } else if ("hi".equals(lang)) {
            reply = "📦 *आर्डर ट्रैकिंग (NX-" + latest.getId() + "):*\n"
                    + "• स्टेटस: *" + latest.getStatus() + "*\n"
                    + "• कुल राशि: *₹" + latest.getTotalAmount() + "*\n"
                    + "• डार्क स्टोर: *" + latest.getStore().getName() + "*\n\n"
                    + "⚡ लाइव GPS ट्रैकिंग देखें:\n" + trackUrl;
        } else {
            reply = "📦 *Order Status Update (NX-" + latest.getId() + "):*\n"
                    + "• Status: *" + latest.getStatus() + "*\n"
                    + "• Amount: *₹" + latest.getTotalAmount() + "*\n"
                    + "• Dark Store: *" + latest.getStore().getName() + "*\n\n"
                    + "⚡ Live GPS Scooter Tracking:\n" + trackUrl;
        }

        return WhatsAppBotResponse.builder()
                .replyMessage(reply)
                .conversationState(conversation.getCurrentState())
                .detectedLanguage(lang)
                .orderId(latest.getId())
                .paymentUrl(trackUrl)
                .build();
    }

    private WhatsAppBotResponse handleProductParsingAndDrafting(WhatsAppConversation conversation, String text) {
        String lang = conversation.getPreferredLanguage();
        List<WhatsAppBotResponse.MatchedItemDto> matchedItems = resolveCatalogueItems(text);

        if (matchedItems.isEmpty()) {
            String fallbackMsg;
            if ("gu".equals(lang)) {
                fallbackMsg = "🤔 મને તમે લખેલી વસ્તુઓ સમજવામાં મુશ્કેલી પડી. કૃપા કરીને આ રીતે લખો:\n"
                        + "👉 *'2 કિલો બટાટા અને 1 પેકેટ દૂધ'*\n"
                        + "અથવા કેટેગરીઝ જોવા *'MENU'* લખો.";
            } else if ("hi".equals(lang)) {
                fallbackMsg = "🤔 मुझे आपके संदेश में सामान की पहचान नहीं हो सकी. कृपया इस तरह लिखें:\n"
                        + "👉 *'2 किलो आलू और 1 पैकेट दूध'*\n"
                        + "या सहायता के लिए *'HELP'* लिखें.";
            } else {
                fallbackMsg = "🤔 I couldn't identify specific items from your message. Please try:\n"
                        + "👉 *'2kg Potato and 1 pack Milk'*\n"
                        + "Or reply *'MENU'* to browse catalogue categories.";
            }

            return WhatsAppBotResponse.builder()
                    .replyMessage(fallbackMsg)
                    .conversationState(conversation.getCurrentState())
                    .detectedLanguage(lang)
                    .build();
        }

        // Calculate totals
        double subtotal = 0.0;
        for (var item : matchedItems) {
            subtotal += item.getSubtotal();
        }

        double deliveryFee = subtotal > 199.0 ? 0.0 : 25.0;
        double grandTotal = subtotal + deliveryFee;

        // Store draft context in conversation
        try {
            Map<String, Object> draft = new HashMap<>();
            draft.put("items", matchedItems);
            draft.put("subtotal", subtotal);
            draft.put("deliveryFee", deliveryFee);
            draft.put("grandTotal", grandTotal);
            conversation.setSessionContext(objectMapper.writeValueAsString(draft));
            conversation.setCurrentState("AWAITING_CONFIRMATION");
        } catch (Exception ex) {
            log.error("Failed to serialize session draft: {}", ex.getMessage());
        }

        // Format itemized bill in customer language
        StringBuilder sb = new StringBuilder();
        if ("gu".equals(lang)) {
            sb.append("🧾 *નેક્સોરા 10-મિનિટ ઓર્ડર સમરી:*\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            for (var it : matchedItems) {
                sb.append("• ").append(it.getProductName())
                        .append(" × ").append(it.getQuantity())
                        .append(" = *₹").append(String.format("%.2f", it.getSubtotal())).append("*\n");
            }
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("સબટોટલ: ₹").append(String.format("%.2f", subtotal)).append("\n");
            sb.append("ડિલિવરી ફી: ").append(deliveryFee == 0.0 ? "*FREE*" : "₹" + deliveryFee).append("\n");
            sb.append("💰 *કુલ ચૂકવવાપાત્ર રકમ: ₹").append(String.format("%.2f", grandTotal)).append("*\n\n");
            sb.append("👉 ઓર્ડર કન્ફર્મ કરવા *'HA'* અથવા *'CONFIRM'* લખો.\n");
            sb.append("👉 રદ કરવા *'NA'* અથવા *'CANCEL'* લખો.");
        } else if ("hi".equals(lang)) {
            sb.append("🧾 *नेक्सोरा 10-मिनट आर्डर सारांश:*\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            for (var it : matchedItems) {
                sb.append("• ").append(it.getProductName())
                        .append(" × ").append(it.getQuantity())
                        .append(" = *₹").append(String.format("%.2f", it.getSubtotal())).append("*\n");
            }
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("सामान राशि: ₹").append(String.format("%.2f", subtotal)).append("\n");
            sb.append("डिलीवरी शुल्क: ").append(deliveryFee == 0.0 ? "*मुफ़्त (FREE)*" : "₹" + deliveryFee).append("\n");
            sb.append("💰 *कुल देय राशि: ₹").append(String.format("%.2f", grandTotal)).append("*\n\n");
            sb.append("👉 आर्डर की पुष्टि के लिए *'HAAN'* या *'CONFIRM'* लिखें.\n");
            sb.append("👉 रद्द करने के लिए *'NAHI'* या *'CANCEL'* लिखें.");
        } else {
            sb.append("🧾 *NEXORA 10-Minute Order Summary:*\n");
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            for (var it : matchedItems) {
                sb.append("• ").append(it.getProductName())
                        .append(" × ").append(it.getQuantity())
                        .append(" = *₹").append(String.format("%.2f", it.getSubtotal())).append("*\n");
            }
            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("Subtotal: ₹").append(String.format("%.2f", subtotal)).append("\n");
            sb.append("Instant Delivery: ").append(deliveryFee == 0.0 ? "*FREE*" : "₹" + deliveryFee).append("\n");
            sb.append("💰 *Final Total: ₹").append(String.format("%.2f", grandTotal)).append("*\n\n");
            sb.append("👉 Reply *'CONFIRM'* to place delivery or *'CANCEL'* to discard.");
        }

        return WhatsAppBotResponse.builder()
                .replyMessage(sb.toString())
                .conversationState("AWAITING_CONFIRMATION")
                .detectedLanguage(lang)
                .matchedItems(matchedItems)
                .build();
    }

    private WhatsAppBotResponse handleOrderConfirmation(WhatsAppConversation conversation) {
        String lang = conversation.getPreferredLanguage();

        try {
            if (conversation.getSessionContext() == null) {
                return handleGreeting(conversation);
            }

            Map<String, Object> draft = objectMapper.readValue(conversation.getSessionContext(), new TypeReference<>() {});
            List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) draft.get("items");

            if (itemMaps == null || itemMaps.isEmpty()) {
                return handleGreeting(conversation);
            }

            // Ensure User exists
            User user = conversation.getUser();
            if (user == null) {
                user = userRepository.findByPhone(conversation.getWaPhoneNumber()).orElseGet(() -> {
                    User newUser = User.builder()
                            .phone(conversation.getWaPhoneNumber())
                            .name(conversation.getCustomerName())
                            .email("wa_" + conversation.getWaPhoneNumber() + "@nexora.local")
                            .password("$2a$10$wACyqgB0K28Y6oU0p6JdXe") // synthetic placeholder
                            .build();
                    return userRepository.save(newUser);
                });
                conversation.setUser(user);
            }

            // Find default dark store
            Store store = storeRepository.findById(4L)
                    .or(() -> storeRepository.findAll().stream().findFirst())
                    .orElseThrow(() -> new RuntimeException("No active store found"));

            // Get or create customer delivery address
            Address address = user.getAddresses() != null && !user.getAddresses().isEmpty()
                    ? user.getAddresses().iterator().next()
                    : Address.builder()
                    .user(user)
                    .label("WhatsApp Quick Drop")
                    .addressLine1("Vastrapur / SG Highway Central")
                    .city("Ahmedabad")
                    .state("Gujarat")
                    .pincode("380015")
                    .latitude(23.0373)
                    .longitude(72.5118)
                    .isDefault(true)
                    .build();

            double subtotal = Double.parseDouble(draft.get("subtotal").toString());
            double deliveryFee = Double.parseDouble(draft.get("deliveryFee").toString());
            double total = Double.parseDouble(draft.get("grandTotal").toString());

            // Create Order
            Order order = Order.builder()
                    .user(user)
                    .store(store)
                    .deliveryAddress(address)
                    .status(OrderStatus.CONFIRMED)
                    .subtotal(BigDecimal.valueOf(subtotal).setScale(2, RoundingMode.HALF_UP))
                    .deliveryFee(BigDecimal.valueOf(deliveryFee).setScale(2, RoundingMode.HALF_UP))
                    .totalAmount(BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP))
                    .paymentMethod("RAZORPAY")
                    .paymentStatus("PENDING")
                    .build();

            order = orderRepository.save(order);

            // Create Order Items
            for (var itMap : itemMaps) {
                Long pId = Long.parseLong(itMap.get("productId").toString());
                int qty = Integer.parseInt(itMap.get("quantity").toString());
                double price = Double.parseDouble(itMap.get("unitPrice").toString());
                String pName = itMap.get("productName").toString();

                Product product = productRepository.findById(pId).orElse(null);

                OrderItem item = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .productNameSnapshot(pName)
                        .unitPrice(BigDecimal.valueOf(price))
                        .quantity(qty)
                        .subtotal(BigDecimal.valueOf(price * qty))
                        .build();

                order.getItems().add(item);
            }

            orderRepository.save(order);

            // Clear draft context
            conversation.setCurrentState("ORDER_PLACED");
            conversation.setSessionContext(null);

            String checkoutUrl = frontendBaseUrl + "/?checkoutOrder=" + order.getId() + "&total=" + total;

            String reply;
            if ("gu".equals(lang)) {
                reply = "🎉 *અભિનંદન! તમારો ઓર્ડર #NX-" + order.getId() + " સફળતાપૂર્વક કન્ફર્મ થયો છે!*\n\n"
                        + "🚴‍♂️ અમારો રાઇડર *10 મિનિટમાં* તમારા સરનામે પહોંચશે.\n"
                        + "💰 કુલ રકમ: *₹" + String.format("%.2f", total) + "*\n\n"
                        + "👉 ઓનલાઇન સુરક્ષિત પેમેન્ટ કરવા અથવા GPS ટ્રેકિંગ માટે લિંક ક્લિક કરો:\n"
                        + checkoutUrl;
            } else if ("hi".equals(lang)) {
                reply = "🎉 *बधाई हो! आपका आर्डर #NX-" + order.getId() + " सफलतापूर्वक बुक हो गया है!*\n\n"
                        + "🚴‍♂️ डिलीवरी पार्टनर *10 मिनट में* आपके पते पर पहुंचेगा.\n"
                        + "💰 कुल देय राशि: *₹" + String.format("%.2f", total) + "*\n\n"
                        + "👉 ऑनलाइन Razorpay / UPI भुगतान व लाइव GPS ट्रैकिंग के लिए क्लिक करें:\n"
                        + checkoutUrl;
            } else {
                reply = "🎉 *Confirmed! Your NEXORA Order #NX-" + order.getId() + " is confirmed!*\n\n"
                        + "🚴‍♂️ A delivery partner is assigned. Expected delivery in *10 minutes*.\n"
                        + "💰 Amount Due: *₹" + String.format("%.2f", total) + "*\n\n"
                        + "👉 Complete UPI/Razorpay payment or view live GPS map here:\n"
                        + checkoutUrl;
            }

            return WhatsAppBotResponse.builder()
                    .replyMessage(reply)
                    .conversationState("ORDER_PLACED")
                    .detectedLanguage(lang)
                    .orderId(order.getId())
                    .paymentUrl(checkoutUrl)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to confirm WhatsApp order: {}", ex.getMessage(), ex);
            String errReply = "gu".equals(lang)
                    ? "❌ ઓર્ડર પ્રોસેસ કરવામાં ક્ષતિ આવી. કૃપા કરીને થોડીવાર પછી પ્રયાસ કરો."
                    : "❌ Error processing your order confirmation. Please try again.";
            return WhatsAppBotResponse.builder()
                    .replyMessage(errReply)
                    .conversationState("IDLE")
                    .detectedLanguage(lang)
                    .build();
        }
    }

    /**
     * Authoritatively resolves product tokens from text against real products in TiDB.
     * Prevents pricing or product hallucination by matching only real catalogue items.
     */
    private List<WhatsAppBotResponse.MatchedItemDto> resolveCatalogueItems(String text) {
        List<WhatsAppBotResponse.MatchedItemDto> matched = new ArrayList<>();
        String[] parts = text.split("[,+&\\n]|\\band\\b|\\bane\\b|\\baur\\b");

        for (String segment : parts) {
            String seg = segment.trim().toLowerCase();
            if (seg.isBlank()) continue;

            // Extract requested quantity
            int quantity = 1;
            Matcher qMatcher = QUANTITY_PATTERN.matcher(seg);
            if (qMatcher.find()) {
                try {
                    quantity = Integer.parseInt(qMatcher.group(1));
                    if (quantity <= 0) quantity = 1;
                } catch (Exception ignored) {}
            }

            // Clean keywords to search for product name
            String searchKeyword = seg
                    .replaceAll("\\b(\\d+)\\b", "")
                    .replaceAll("\\b(kilo|kg|packet|pack|l|liter|litre|gm|g|unit|nag|doz|dozen|bhejo|moko|chahiye|aapo|send|need|want|order)\\b", "")
                    .trim();

            if (searchKeyword.isBlank()) continue;

            // Map regional words to canonical product names
            String mappedName = searchKeyword;
            for (var entry : GUJARATI_LEXICON.entrySet()) {
                if (searchKeyword.contains(entry.getKey())) {
                    mappedName = entry.getValue();
                    break;
                }
            }
            if (mappedName.equals(searchKeyword)) {
                for (var entry : HINDI_LEXICON.entrySet()) {
                    if (searchKeyword.contains(entry.getKey())) {
                        mappedName = entry.getValue();
                        break;
                    }
                }
            }

            // Query database for authoritative product
            List<Product> candidates = productRepository.findByNameContainingIgnoreCase(mappedName);
            if (candidates.isEmpty()) {
                // Fallback: search by original keyword
                candidates = productRepository.findByNameContainingIgnoreCase(searchKeyword);
            }

            if (!candidates.isEmpty()) {
                Product prod = candidates.get(0);
                double unitPrice = prod.getPrice() != null ? prod.getPrice().doubleValue() : 50.0;
                double subtotal = unitPrice * quantity;

                matched.add(WhatsAppBotResponse.MatchedItemDto.builder()
                        .productId(prod.getId())
                        .productName(prod.getName())
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .subtotal(subtotal)
                        .inStock(prod.isAvailable())
                        .build());
            }
        }

        return matched;
    }

        private String detectLanguage(String text) {
        String lower = " " + text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", " ") + " ";
        int guScore = 0;
        int hiScore = 0;

        for (String guWord : List.of("batata", "bataka", "tameta", "dudh", "kanda", "dungri", "cha", "kela", "safarchand", "moko", "aapo", "kem", "chho", "ane", "nathi")) {
            if (lower.contains(" " + guWord + " ")) guScore++;
        }
        for (String hiWord : List.of("aalu", "aloo", "tamatar", "doodh", "pyaz", "chai", "seb", "bhejo", "chahiye", "namaste", "aur", "nahi", "karo")) {
            if (lower.contains(" " + hiWord + " ")) hiScore++;
        }

        if (guScore > hiScore) return "gu";
        if (hiScore > guScore) return "hi";
        return "en";
    }

    public List<WhatsAppConversationSummaryResponse> getAllConversations() {
        return conversationRepository.findAllByOrderByLastActiveAtDesc().stream().map(c -> {
            List<WhatsAppMessage> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
            String lastMsg = msgs.isEmpty() ? "No messages" : msgs.get(msgs.size() - 1).getMessageBody();
            return WhatsAppConversationSummaryResponse.builder()
                    .id(c.getId())
                    .waPhoneNumber(c.getWaPhoneNumber())
                    .customerName(c.getCustomerName())
                    .currentState(c.getCurrentState())
                    .preferredLanguage(c.getPreferredLanguage())
                    .lastMessage(lastMsg)
                    .lastActiveAt(c.getLastActiveAt())
                    .messageCount(msgs.size())
                    .build();
        }).collect(Collectors.toList());
    }
}
