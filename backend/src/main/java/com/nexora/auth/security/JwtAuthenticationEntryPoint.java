package com.nexora.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fires whenever an unauthenticated request hits a PROTECTED endpoint
 * (no token, or an invalid/expired one). Without this class, Spring
 * Security's default behavior is to return an empty 401 response body
 * — this makes it consistent with the rest of Nexora's API by wrapping
 * it in our standard ApiResponse envelope instead.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Object> body = ApiResponse.error(
                "Authentication required. Provide a valid Bearer token in the Authorization header.");

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
