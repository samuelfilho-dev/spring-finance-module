package com.samuelfilho_dev.finance_module.config;

import com.samuelfilho_dev.finance_module.auth.entities.AuthenticatedUser;
import com.samuelfilho_dev.finance_module.auth.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String ROUTE_2FA_VERIFY = "/api/v1/auth/mfa/verify";
    private static final String ROUTE_2FA_ENABLE = "/api/v1/auth/mfa/enable";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        var header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = header.substring(7);
        var path = request.getServletPath();

        try {
            var claims = jwtService.parseAndValidate(token);
            var type = jwtService.extractType(claims);

            if (type == null) {
                denyAccess(response, "Token JWT Inválido");
                return;
            }

            switch (type) {
                case JwtService.TYPE_ACCESS -> {
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        authenticate(request, claims);
                    }
                }
                case JwtService.TYPE_PRE_AUTH -> {
                    if (!ROUTE_2FA_VERIFY.equals(path)) {
                        denyAccess(response, "Token de pré-autenticação (PRE_AUTH) só pode ser usado em " + ROUTE_2FA_VERIFY);
                        return;
                    }
                }
                case JwtService.TYPE_SETUP -> {
                    if (!ROUTE_2FA_ENABLE.equals(path)) {
                        denyAccess(response, "Token de configuração (SETUP_2FA) só pode ser usado em  " + ROUTE_2FA_ENABLE);
                        return;
                    }
                }
                default -> {
                    denyAccess(response, "Tipo de Token Desconhecido" + type);
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            denyAccess(response, "Token JWT Inválido");
        }
    }

    private void authenticate(HttpServletRequest request, Claims claims) {
        var userId = jwtService.extractUserId(claims);
        var email = jwtService.extractEmail(claims);
        var roles = jwtService.extractRoles(claims);

        List<SimpleGrantedAuthority> authorities = roles == null ? List.of() :
                roles.stream().map(SimpleGrantedAuthority::new).toList();

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Token JWT sem userId");
        }

        var principal = new AuthenticatedUser(userId, email, null, authorities);
        var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void denyAccess(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
