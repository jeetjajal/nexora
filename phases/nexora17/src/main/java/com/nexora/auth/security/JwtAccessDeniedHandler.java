package com.nexora.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fires when a request IS authenticated (has a valid token) but the
 * authenticated user's role doesn't have permission for the endpoint
 * (e.g. a CUSTOMER hitting an ADMIN-only route in a later phase).
 * Distinct from JwtAuthenticationEntryPoint (401 = "who are you?")
 * — this is 403 = "I know who you are, and you're not allowed."
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Object> body = ApiResponse.error(
                "Access denied. Your account does not have permission to perform this action.");

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
