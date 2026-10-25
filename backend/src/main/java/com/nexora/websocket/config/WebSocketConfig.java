package com.nexora.websocket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * PHASE 17: WEBSOCKET & STOMP MESSAGE BROKER CONFIGURATION
 *
 * Configures full-duplex WebSocket messaging for real-time order tracking
 * and delivery partner location updates.
 *
 * - Endpoints:
 *     /ws-nexora       -> STOMP over SockJS fallback endpoint
 * - Destination Prefixes:
 *     /app             -> Application destination prefix for client-to-server messages
 *     /topic           -> In-memory broadcast broker for group/public subscriptions
 *     /queue           -> In-memory broker for private user-targeted messages
 *     /user            -> User destination prefix for private messaging
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${nexora.websocket.endpoint:/ws-nexora}")
    private String websocketEndpoint;

    @Value("${nexora.websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory message broker that pushes messages to clients subscribed to /topic or /queue
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages routed to @MessageMapping-annotated methods in @Controller classes
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix used for user-specific messaging (e.g. convertAndSendToUser)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main STOMP handshake endpoint with SockJS fallback support
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS();

        // Plain WebSocket endpoint for clients that do not use SockJS (e.g. mobile apps / standard WebSocket)
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }
}
