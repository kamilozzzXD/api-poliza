package com.bolivar.api_polizas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

// Filtro de seguridad para validar cabecera API Key
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${app.api-key:123456}")
    private String expectedApiKey;

    // Omite el filtro para la consola H2
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/h2-console") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    // Valida presencia de x-api-key o api-key
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("x-api-key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = request.getHeader("api-key");
        }

        if (apiKey != null && apiKey.equals(expectedApiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Header de seguridad invalido o ausente (x-api-key o api-key requerido)",
                request.getRequestURI()
        );

        response.getWriter().write(jsonResponse);
    }
}
