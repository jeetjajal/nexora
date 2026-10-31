package com.nexora.whatsapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.ApiResponse;
import com.nexora.whatsapp.dto.WhatsAppBotResponse;
import com.nexora.whatsapp.dto.WhatsAppConversationSummaryResponse;
import com.nexora.whatsapp.dto.WhatsAppSimulateRequest;
import com.nexora.whatsapp.service.WhatsAppBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppBotService botService;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.verify-token:nexora_whatsapp_verify_token_2026}")
    private String verifyToken;

    @Value("${whatsapp.app-secret:}")
    private String appSecret;

    /**
     * Webhook Verification Handshake for Meta WhatsApp Cloud API.
     */
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("WhatsApp webhook verification received: mode={}, token={}", mode, token);

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verify token");
    }

    /**
     * Inbound WhatsApp Cloud API webhook receiver.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleInboundMessage(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        // Signature verification if appSecret is configured
        if (appSecret != null && !appSecret.isBlank() && signature != null) {
            if (!isValidSignature(rawPayload, signature, appSecret)) {
                log.warn("Invalid WhatsApp webhook signature received");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode entry = root.path("entry");
            if (entry.isArray() && !entry.isEmpty()) {
                JsonNode changes = entry.get(0).path("changes");
                if (changes.isArray() && !changes.isEmpty()) {
                    JsonNode value = changes.get(0).path("value");
                    JsonNode messages = value.path("messages");

                    if (messages.isArray() && !messages.isEmpty()) {
                        JsonNode message = messages.get(0);
                        String from = message.path("from").asText();
                        String text = message.path("text").path("body").asText();

                        String senderName = "Valued Customer";
                        JsonNode contacts = value.path("contacts");
                        if (contacts.isArray() && !contacts.isEmpty()) {
                            senderName = contacts.get(0).path("profile").path("name").asText(senderName);
                        }

                        botService.processInboundMessage(from, senderName, text);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse WhatsApp webhook message: {}", ex.getMessage());
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /**
     * Interactive Simulation Endpoint for Control Tower and QA verification.
     */
    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<WhatsAppBotResponse>> simulateCustomerChat(
            @RequestBody WhatsAppSimulateRequest request) {

        String phone = (request.getPhone() != null && !request.getPhone().isBlank()) 
                ? request.getPhone() 
                : "+919876543210";
        String name = (request.getCustomerName() != null && !request.getCustomerName().isBlank()) 
                ? request.getCustomerName() 
                : "Customer";

        WhatsAppBotResponse response = botService.processInboundMessage(phone, name, request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("Processed by NEXORA AI WhatsApp Bot", response));
    }

    /**
     * Lists live conversation logs for Super Admin Control Tower.
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<WhatsAppConversationSummaryResponse>>> getConversations() {
        List<WhatsAppConversationSummaryResponse> list = botService.getAllConversations();
        return ResponseEntity.ok(ApiResponse.success("WhatsApp conversations retrieved", list));
    }

    private boolean isValidSignature(String payload, String signatureHeader, String secret) {
        try {
            if (!signatureHeader.startsWith("sha256=")) {
                return false;
            }
            String actualSignature = signatureHeader.substring(7);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.UTF_8),
                    actualSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
