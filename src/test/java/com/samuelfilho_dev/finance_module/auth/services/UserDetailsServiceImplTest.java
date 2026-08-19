package com.samuelfilho_dev.finance_module.auth.services;

import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_shouldReturnAuthenticatedUser() {
        var user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .password("hashed")
                .role("ROLE_ADMIN")
                .build();
        when(userRepository.findUserByEmail("user@test.com")).thenReturn(Optional.of(user));

        var details = userDetailsService.loadUserByUsername("user@test.com");

        assertEquals("user@test.com", details.getUsername());
        assertEquals("hashed", details.getPassword());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_shouldThrowWhenMissing() {
        when(userRepository.findUserByEmail("missing@test.com")).thenReturn(Optional.empty());

        var exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@test.com"));
        assertEquals("Usuário não encontrado: missing@test.com", exception.getMessage());
    }
}
