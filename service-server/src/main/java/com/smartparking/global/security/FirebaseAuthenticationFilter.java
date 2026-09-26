package com.smartparking.global.security;

import com.smartparking.auth.application.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Pattern BEARER_TOKEN = Pattern.compile("^Bearer ([^\\s]+)$", Pattern.CASE_INSENSITIVE);

    private final FirebaseTokenVerifier tokenVerifier;
    private final AuthService authService;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    public FirebaseAuthenticationFilter(
        FirebaseTokenVerifier tokenVerifier,
        AuthService authService,
        JsonAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.tokenVerifier = tokenVerifier;
        this.authService = authService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) {
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Missing bearer token")
            );
            return;
        }

        Matcher matcher = BEARER_TOKEN.matcher(header);
        try {
            if (!matcher.matches()) {
                throw new BadCredentialsException("Malformed bearer token");
            }
            String firebaseUid = tokenVerifier.verify(matcher.group(1));
            CurrentUserPrincipal principal = authService.authenticate(firebaseUid);
            SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    null,
                    principal.role().authorities()
                )
            );
            filterChain.doFilter(request, response);
        } catch (FirebaseTokenVerificationException | BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new BadCredentialsException("Invalid authentication"));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
            || path.equals("/actuator/info")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/scalar");
    }
}
