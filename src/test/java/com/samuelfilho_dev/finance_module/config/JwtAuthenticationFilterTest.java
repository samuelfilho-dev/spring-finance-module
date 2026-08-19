package com.samuelfilho_dev.finance_module.config;

import com.samuelfilho_dev.finance_module.auth.entities.AuthenticatedUser;
import com.samuelfilho_dev.finance_module.auth.services.JwtService;
import com.samuelfilho_dev.finance_module.support.TestSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private Claims claims;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).parseAndValidate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).parseAndValidate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAuthenticateAccessToken() throws Exception {
        var request = requestWithBearer("/api/v1/accounts", "access-token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("access-token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(JwtService.TYPE_ACCESS);
        when(jwtService.extractUserId(claims)).thenReturn("user-1");
        when(jwtService.extractEmail(claims)).thenReturn("user@test.com");
        when(jwtService.extractRoles(claims)).thenReturn(List.of("ROLE_USER"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var principal = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertInstanceOf(AuthenticatedUser.class, principal);
        assertEquals("user-1", principal.getId());
        assertEquals("user@test.com", principal.getUsername());
    }

    @Test
    void shouldAllowPreAuthTokenOnlyOnVerifyRoute() throws Exception {
        var request = requestWithBearer("/api/v1/auth/mfa/verify", "pre-token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("pre-token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(JwtService.TYPE_PRE_AUTH);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectPreAuthTokenOnOtherRoutes() throws Exception {
        var request = requestWithBearer("/api/v1/accounts", "pre-token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("pre-token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(JwtService.TYPE_PRE_AUTH);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("PRE_AUTH"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldAllowSetupTokenOnlyOnEnableRoute() throws Exception {
        var request = requestWithBearer("/api/v1/auth/mfa/enable", "setup-token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("setup-token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(JwtService.TYPE_SETUP);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectSetupTokenOnOtherRoutes() throws Exception {
        var request = requestWithBearer("/api/v1/accounts", "setup-token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("setup-token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(JwtService.TYPE_SETUP);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("SETUP_2FA"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldKeepExistingAuthenticationForAccessToken() throws Exception {
        TestSupport.authenticate("already-auth");
        var request = requestWithBearer("/api/v1/accounts", "access-token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("access-token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(JwtService.TYPE_ACCESS);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUserId(claims);
        var principal = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals("already-auth", principal.getId());
    }

    @Test
    void shouldRejectAccessTokenWithoutUserId() throws Exception {
        var request = requestWithBearer("/api/v1/accounts", "access-token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("access-token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(JwtService.TYPE_ACCESS);
        when(jwtService.extractUserId(claims)).thenReturn(" ");
        when(jwtService.extractEmail(claims)).thenReturn("user@test.com");
        when(jwtService.extractRoles(claims)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token JWT Inválido"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldRejectUnknownTokenType() throws Exception {
        var request = requestWithBearer("/api/v1/accounts", "token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn("OTHER");

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Tipo de Token Desconhecido"));
    }

    @Test
    void shouldRejectInvalidJwt() throws Exception {
        var request = requestWithBearer("/api/v1/accounts", "bad");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("bad")).thenThrow(new JwtException("invalid"));

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token JWT Inválido"));
    }

    @Test
    void shouldRejectTokenWithoutType() throws Exception {
        var request = requestWithBearer("/api/v1/accounts", "token");
        var response = new MockHttpServletResponse();
        when(jwtService.parseAndValidate("token")).thenReturn(claims);
        when(jwtService.extractType(claims)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    private static MockHttpServletRequest requestWithBearer(String path, String token) {
        var request = new MockHttpServletRequest();
        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
