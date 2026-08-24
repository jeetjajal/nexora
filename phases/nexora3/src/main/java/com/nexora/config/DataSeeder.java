package com.nexora.config;

import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.user.entity.User;
import com.nexora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // 1. Seed all fixed roles
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .name(roleName)
                                    .build()
                    ));
        }

        // 2. Create default ADMIN user if it doesn't exist
        if (!userRepository.existsByEmail("admin@example.com")) {

            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow(() ->
                            new IllegalStateException("ADMIN role not found"));

            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("AdminPassword123"))
                    .phone("9999999999")
                    .status("ACTIVE")
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(admin);

            System.out.println("======================================");
            System.out.println("Default ADMIN user created");
            System.out.println("Email: admin@example.com");
            System.out.println("Password: AdminPassword123");
            System.out.println("======================================");
        }
    }
}