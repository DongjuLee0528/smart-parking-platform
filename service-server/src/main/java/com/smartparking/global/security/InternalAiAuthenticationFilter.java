package com.smartparking.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalAiAuthenticationFilter extends OncePerRequestFilter {
    private static final Pattern BEARER = Pattern.compile("^Bearer ([^\\s]+)$", Pattern.CASE_INSENSITIVE);
    private final byte[] expectedToken;
    private final ObjectMapper mapper;

    public InternalAiAuthenticationFilter(@Value("${AI_INTERNAL_TOKEN:}") String token, ObjectMapper mapper) {
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") ||
            !request.getRequestURI().equals("/internal/v1/occupancy-results");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        var matcher = header == null ? null : BEARER.matcher(header);
        if (expectedToken.length == 0 || matcher == null || !matcher.matches() ||
            !MessageDigest.isEqual(expectedToken, matcher.group(1).getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new ErrorResponse(ErrorCode.AUTH_TOKEN_INVALID,
                "Internal service authentication failed", UUID.randomUUID(), Map.of()));
            return;
        }
        chain.doFilter(request, response);
    }
}
