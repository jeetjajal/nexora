package com.nexora.ai.client;

import com.nexora.ai.dto.AiChatMessage;
import com.nexora.ai.tool.OrderAiTools;
import com.nexora.ai.tool.ProductAiTools;
import com.nexora.ai.tool.StoreAiTools;
import com.nexora.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance offline / fallback AI Client.
 * Routes user intents safely to approved backend tools without external LLM network dependency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StubAiClient implements AiClient {

    private final OrderAiTools orderAiTools;
    private final StoreAiTools storeAiTools;
    private final ProductAiTools productAiTools;

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("(?:order|#)\\s*#?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAX_PRICE_PATTERN = Pattern.compile("(?:under|below|<|less than|rs\\.?|inr|₹)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String chat(String userMessage, List<AiChatMessage> history, UserPrincipal caller, List<String> toolsExecutedOut) {
        if (userMessage == null || userMessage.isBlank()) {
            return "How can I help you today? You can ask about order status, browse stores, or search for food.";
        }

        String msg = userMessage.trim().toLowerCase();

        // 1. Order Status Intent
        if (msg.contains("status") || (msg.contains("where") && msg.contains("order"))) {
            Long orderId = extractOrderId(msg);
            if (orderId != null) {
                toolsExecutedOut.add("getOrderStatus");
                String toolResult = orderAiTools.getOrderStatus(orderId, caller);
                return "Here is the status of your order:\n" + toolResult;
            } else {
                toolsExecutedOut.add("getUserOrderHistory");
                String historyResult = orderAiTools.getUserOrderHistory(caller);
                return "You didn't specify an order number. Here are your recent orders:\n" + historyResult;
            }
        }

        // 2. Order Details Intent
        if (msg.contains("detail") && msg.contains("order")) {
            Long orderId = extractOrderId(msg);
            if (orderId != null) {
                toolsExecutedOut.add("getOrderDetails");
                return orderAiTools.getOrderDetails(orderId, caller);
            }
        }

        // 3. User Order History Intent
        if (msg.contains("recent order") || msg.contains("my order") || msg.contains("order history") || msg.contains("past order")) {
            toolsExecutedOut.add("getUserOrderHistory");
            return orderAiTools.getUserOrderHistory(caller);
        }

        // 4. Stores Intent
        if (msg.contains("store") || msg.contains("restaurant") || msg.contains("open") || msg.contains("shop")) {
            String category = extractCategory(msg);
            toolsExecutedOut.add("getAvailableStores");
            return storeAiTools.getAvailableStores(category);
        }

        // 5. Product Search Intent
        if (msg.contains("pizza") || msg.contains("burger") || msg.contains("food") || msg.contains("find")
                || msg.contains("search") || msg.contains("menu") || msg.contains("recommend") || msg.contains("veg")) {
            Double maxPrice = extractMaxPrice(msg);
            String category = extractCategory(msg);
            String query = extractSearchQuery(msg);
            toolsExecutedOut.add("searchProducts");
            return productAiTools.searchProducts(query, category, maxPrice);
        }

        // 6. Greetings
        if (msg.contains("hello") || msg.contains("hi") || msg.contains("hey")) {
            String name = caller != null ? caller.getUser().getName() : "Customer";
            return String.format("Hello %s! I am Nexora AI Assistant. I can help you track your orders, view recent purchases, find open stores, or search for food items on our menu. What would you like to do?", name);
        }

        // 7. General Assistance / Fallback
        return "I can assist you with your orders, open stores, and menu items on Nexora. For example, try asking:\n"
                + "- 'Where is my order #1?'\n"
                + "- 'Show my recent orders'\n"
                + "- 'Which pizza stores are open?'\n"
                + "- 'Find food under ₹300'";
    }

    @Override
    public String getProviderName() {
        return "LOCAL_STUB";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private Long extractOrderId(String msg) {
        Matcher matcher = ORDER_ID_PATTERN.matcher(msg);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Double extractMaxPrice(String msg) {
        Matcher matcher = MAX_PRICE_PATTERN.matcher(msg);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String extractCategory(String msg) {
        if (msg.contains("pizza")) return "Pizza";
        if (msg.contains("burger")) return "Burger";
        if (msg.contains("italian")) return "Italian";
        if (msg.contains("fast food")) return "Fast Food";
        if (msg.contains("mexican")) return "Mexican";
        if (msg.contains("dessert")) return "Dessert";
        return null;
    }

    private String extractSearchQuery(String msg) {
        if (msg.contains("pizza")) return "pizza";
        if (msg.contains("burger")) return "burger";
        if (msg.contains("taco")) return "taco";
        if (msg.contains("truffle")) return "truffle";
        return msg.replace("find", "").replace("search", "").replace("for", "").trim();
    }
}
