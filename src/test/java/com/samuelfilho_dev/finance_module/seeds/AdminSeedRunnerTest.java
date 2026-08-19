package com.samuelfilho_dev.finance_module.seeds;

import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeedRunnerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeedRunner adminSeedRunner;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminSeedRunner, "adminPassword", "admin-secret");
    }

    @Test
    void run_shouldCreateAdminWhenMissing() throws Exception {
        when(userRepository.findUserByEmail("admin@admin.com.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin-secret")).thenReturn("hashed");

        adminSeedRunner.run();

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("Admin", captor.getValue().getName());
        assertEquals("admin@admin.com.br", captor.getValue().getEmail());
        assertEquals("hashed", captor.getValue().getPassword());
        assertEquals("ROLE_ADMIN", captor.getValue().getRole());
    }

    @Test
    void run_shouldSkipWhenAdminAlreadyExists() throws Exception {
        when(userRepository.findUserByEmail("admin@admin.com.br"))
                .thenReturn(Optional.of(User.builder().email("admin@admin.com.br").build()));

        adminSeedRunner.run();

        verify(userRepository, never()).save(any());
    }
}
