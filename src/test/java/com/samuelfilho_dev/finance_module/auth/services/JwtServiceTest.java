package com.samuelfilho_dev.finance_module.auth.services;

import com.samuelfilho_dev.finance_module.support.TestSupport;
import com.samuelfilho_dev.finance_module.users.entities.User;
import io.jsonwebtoken.JwtException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        var keys = TestSupport.rsaKeyResources();
        jwtService = new JwtService(
                "finance_module",
                keys.privateKey(),
                keys.publicKey(),
                3_600_000L,
                300_000L,
                600_000L
        );
        user = User.builder()
                .id(new ObjectId().toHexString())
                .email("user@test.com")
                .role("ROLE_USER")
                .build();
    }

    @Test
    void generateAccessToken_shouldContainAccessClaims() {
        var token = jwtService.generateAccessToken(user);
        var claims = jwtService.parseAndValidate(token);

        assertEquals(user.getEmail(), jwtService.extractEmail(claims));
        assertEquals(JwtService.TYPE_ACCESS, jwtService.extractType(claims));
        assertEquals(user.getId(), jwtService.extractUserId(claims));
        assertEquals(List.of("ROLE_USER"), jwtService.extractRoles(claims));
        assertEquals("finance_module", claims.getIssuer());
    }

    @Test
    void generatePreAuthToken_shouldContainPreAuthType() {
        var claims = jwtService.parseAndValidate(jwtService.generatePreAuthToken(user));
        assertEquals(JwtService.TYPE_PRE_AUTH, jwtService.extractType(claims));
    }

    @Test
    void generateSetupToken_shouldContainSetupType() {
        var claims = jwtService.parseAndValidate(jwtService.generateSetupToken(user));
        assertEquals(JwtService.TYPE_SETUP, jwtService.extractType(claims));
    }

    @Test
    void parseAndValidate_shouldRejectTamperedToken() {
        var token = jwtService.generateAccessToken(user);
        assertThrows(JwtException.class, () -> jwtService.parseAndValidate(token + "x"));
    }

    @Test
    void extractRoles_shouldReturnNullForPreAuthToken() {
        var claims = jwtService.parseAndValidate(jwtService.generatePreAuthToken(user));
        assertNull(jwtService.extractRoles(claims));
        assertEquals(user.getId(), jwtService.extractUserId(claims));
    }

    @Test
    void constructor_shouldFailWhenKeysCannotBeLoaded() {
        var invalid = new org.springframework.core.io.ByteArrayResource("not-a-key".getBytes());
        assertThrows(RuntimeException.class, () -> new JwtService(
                "finance_module",
                invalid,
                invalid,
                1_000L,
                1_000L,
                1_000L
        ));
    }

    @Test
    void parseAndValidate_shouldRejectExpiredToken() {
        var keys = TestSupport.rsaKeyResources();
        var expiredJwtService = new JwtService(
                "finance_module",
                keys.privateKey(),
                keys.publicKey(),
                -1_000L,
                -1_000L,
                -1_000L
        );

        var token = expiredJwtService.generateAccessToken(user);
        assertThrows(JwtException.class, () -> expiredJwtService.parseAndValidate(token));
    }
}
