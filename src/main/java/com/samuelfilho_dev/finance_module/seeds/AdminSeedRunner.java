package com.samuelfilho_dev.finance_module.seeds;

import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements CommandLineRunner {

    @Value("${app.secret.admin-password}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        var email = "admin@admin.com.br";

        if (userRepository.findUserByEmail(email).isEmpty()) {
            userRepository.save(
                    User.builder()
                            .name("Admin")
                            .email(email)
                            .password(passwordEncoder.encode(adminPassword))
                            .role("ROLE_ADMIN")
                            .build()
            );
        }
    }
}
