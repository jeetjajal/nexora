package com.nexora.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexora.ai.dto.AiChatMessage;
import com.nexora.ai.tool.OrderAiTools;
import com.nexora.ai.tool.ProductAiTools;
import com.nexora.ai.tool.StoreAiTools;
import com.nexora.auth.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Real Google Gemini AI Client with native Function Calling.
 * Communicates with the Google Generative Language API.
 * Never connects directly to MySQL — only executes approved Java tool methods.
 */
@Slf4j
@Component
public class GeminiAiClient implements AiClient {

    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final OrderAiTools orderAiTools;
    private final StoreAiTools storeAiTools;
    private final ProductAiTools productAiTools;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiAiClient(
            @Value("${nexora.ai.gemini.api-key:}") String apiKey,
            @Value("${nexora.ai.gemini.model:gemini-1.5-flash}") String model,
            @Value("${nexora.ai.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}") String apiUrl,
            OrderAiTools orderAiTools,
            StoreAiTools storeAiTools,
            ProductAiTools productAiTools,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-1.5-flash";
        this.apiUrl = apiUrl != null && !apiUrl.isBlank() ? apiUrl.trim() : "https://generativelanguage.googleapis.com/v1beta/models";
        this.orderAiTools = orderAiTools;
        this.storeAiTools = storeAiTools;
        this.productAiTools = productAiTools;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String chat(String userMessage, List<AiChatMessage> history, UserPrincipal caller, List<String> toolsExecutedOut) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API Key is not configured. Please set the GEMINI_API_KEY environment variable.");
        }

        try {
            String url = String.format("%s/%s:generateContent", apiUrl, model);

            ObjectNode requestBody = buildGeminiRequest(userMessage, history);
            String requestJson = objectMapper.writeValueAsString(requestBody);

            String responseBody = restClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidate = root.path("candidates").path(0);
            JsonNode part = candidate.path("content").path("parts").path(0);

            // Check if Gemini invoked a function call
            if (part.has("functionCall")) {
                JsonNode functionCall = part.path("functionCall");
                String functionName = functionCall.path("name").asText();
                JsonNode args = functionCall.path("args");

                toolsExecutedOut.add(functionName);
                String toolResult = executeTool(functionName, args, caller);

                // Send tool result back to Gemini to synthesize natural final response
                return synthesizeFinalResponse(userMessage, functionName, toolResult);
            }

            // Normal text response
            if (part.has("text")) {
                return part.path("text").asText();
            }

            return "I received your message, but no response was generated.";
        } catch (Exception e) {
            String safeError = sanitizeErrorMessage(e.getMessage());
            log.error("Gemini API call failed: {}", safeError);
            throw new RuntimeException("Gemini AI service error: " + safeError);
        }
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank() && !"test-gemini-key".equalsIgnoreCase(apiKey);
    }

    private String executeTool(String functionName, JsonNode args, UserPrincipal caller) {
        switch (functionName) {
            case "getOrderStatus": {
                Long orderId = args.has("orderId") ? args.path("orderId").asLong() : null;
                return orderAiTools.getOrderStatus(orderId, caller);
            }
            case "getOrderDetails": {
                Long orderId = args.has("orderId") ? args.path("orderId").asLong() : null;
                return orderAiTools.getOrderDetails(orderId, caller);
            }
            case "getUserOrderHistory": {
                return orderAiTools.getUserOrderHistory(caller);
            }
            case "getAvailableStores": {
                String category = args.has("category") ? args.path("category").asText() : null;
                return storeAiTools.getAvailableStores(category);
            }
            case "searchProducts": {
                String query = args.has("query") ? args.path("query").asText() : null;
                String category = args.has("category") ? args.path("category").asText() : null;
                Double maxPrice = args.has("maxPrice") ? args.path("maxPrice").asDouble() : null;
                return productAiTools.searchProducts(query, category, maxPrice);
            }
            default:
                return "Unknown function tool: " + functionName;
        }
    }

    private String synthesizeFinalResponse(String userMessage, String functionName, String toolResult) {
        try {
            String url = String.format("%s/%s:generateContent", apiUrl, model);

            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode contents = body.putArray("contents");

            ObjectNode userTurn = contents.addObject();
            userTurn.put("role", "user");
            ArrayNode userParts = userTurn.putArray("parts");
            userParts.addObject().put("text", String.format(
                    "User asked: \"%s\". We executed tool '%s' with real Nexora database output:\n\"%s\"\nPlease provide a friendly, helpful, concise answer to the user based strictly on this information.",
                    userMessage, functionName, toolResult));

            String responseBody = restClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textPart = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!textPart.isMissingNode() && !textPart.asText().isBlank()) {
                return textPart.asText();
            }
        } catch (Exception ex) {
            log.warn("Could not synthesize final Gemini response, returning raw tool output: {}", sanitizeErrorMessage(ex.getMessage()));
        }
        return toolResult;
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        String sanitized = message;
        if (!apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "[REDACTED_API_KEY]");
        }
        sanitized = sanitized.replaceAll("AIza[0-9A-Za-z-_]{35}", "[REDACTED_API_KEY]");
        return sanitized;
    }

    private ObjectNode buildGeminiRequest(String userMessage, List<AiChatMessage> history) {
        ObjectNode root = objectMapper.createObjectNode();

        // System Instruction
        ObjectNode systemInstruction = root.putObject("system_instruction");
        ArrayNode siParts = systemInstruction.putArray("parts");
        siParts.addObject().put("text",
                "You are Nexora AI Assistant, a friendly and accurate e-commerce customer support assistant for Nexora. "
                        + "You help customers track orders, browse open stores, and discover food. "
                        + "Always call the provided tools when the user asks about order status, order history, stores, or food menu items. "
                        + "Never fabricate order numbers, prices, or inventory.");

        // Contents (History + User Message)
        ArrayNode contents = root.putArray("contents");
        if (history != null) {
            for (AiChatMessage msg : history) {
                ObjectNode turn = contents.addObject();
                turn.put("role", "model".equalsIgnoreCase(msg.getRole()) || "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user");
                turn.putArray("parts").addObject().put("text", msg.getContent());
            }
        }

        ObjectNode currentTurn = contents.addObject();
        currentTurn.put("role", "user");
        currentTurn.putArray("parts").addObject().put("text", userMessage);

        // Tools / Function Declarations
        ArrayNode tools = root.putArray("tools");
        ObjectNode toolObj = tools.addObject();
        ArrayNode funcDecls = toolObj.putArray("function_declarations");

        // getOrderStatus
        ObjectNode getOrderStatus = funcDecls.addObject();
        getOrderStatus.put("name", "getOrderStatus");
        getOrderStatus.put("description", "Get the real-time delivery status of a customer order by its ID");
        ObjectNode gosParams = getOrderStatus.putObject("parameters");
        gosParams.put("type", "object");
        ObjectNode gosProps = gosParams.putObject("properties");
        gosProps.putObject("orderId").put("type", "number").put("description", "The numeric order ID");
        gosParams.putArray("required").add("orderId");

        // getOrderDetails
        ObjectNode getOrderDetails = funcDecls.addObject();
        getOrderDetails.put("name", "getOrderDetails");
        getOrderDetails.put("description", "Get full details of an order including line items, prices, and delivery address");
        ObjectNode godParams = getOrderDetails.putObject("parameters");
        godParams.put("type", "object");
        ObjectNode godProps = godParams.putObject("properties");
        godProps.putObject("orderId").put("type", "number").put("description", "The numeric order ID");
        godParams.putArray("required").add("orderId");

        // getUserOrderHistory
        ObjectNode getUserOrderHistory = funcDecls.addObject();
        getUserOrderHistory.put("name", "getUserOrderHistory");
        getUserOrderHistory.put("description", "Retrieve previous recent orders placed by the current logged-in customer");
        ObjectNode gohParams = getUserOrderHistory.putObject("parameters");
        gohParams.put("type", "object");
        gohParams.putObject("properties");

        // getAvailableStores
        ObjectNode getAvailableStores = funcDecls.addObject();
        getAvailableStores.put("name", "getAvailableStores");
        getAvailableStores.put("description", "List open stores and restaurants on Nexora, optionally filtered by category");
        ObjectNode gasParams = getAvailableStores.putObject("parameters");
        gasParams.put("type", "object");
        ObjectNode gasProps = gasParams.putObject("properties");
        gasProps.putObject("category").put("type", "string").put("description", "Category such as Pizza, Italian, Burger, Fast Food");

        // searchProducts
        ObjectNode searchProducts = funcDecls.addObject();
        searchProducts.put("name", "searchProducts");
        searchProducts.put("description", "Search food products by name/query, category, and maximum budget/price");
        ObjectNode spParams = searchProducts.putObject("parameters");
        spParams.put("type", "object");
        ObjectNode spProps = spParams.putObject("properties");
        spProps.putObject("query").put("type", "string").put("description", "Search keywords like pizza, pasta, burger");
        spProps.putObject("category").put("type", "string").put("description", "Category filter");
        spProps.putObject("maxPrice").put("type", "number").put("description", "Maximum price limit in INR");

        return root;
    }
}
