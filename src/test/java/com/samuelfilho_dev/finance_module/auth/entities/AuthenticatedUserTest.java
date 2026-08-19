package com.samuelfilho_dev.finance_module.auth.entities;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedUserTest {

    @Test
    void shouldExposeUserDetailsContract() {
        var user = new AuthenticatedUser(
                "user-1",
                "user@test.com",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertEquals("user-1", user.getId());
        assertEquals("user@test.com", user.getUsername());
        assertEquals("secret", user.getPassword());
        assertEquals("ROLE_USER", user.getAuthorities().iterator().next().getAuthority());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }
}
