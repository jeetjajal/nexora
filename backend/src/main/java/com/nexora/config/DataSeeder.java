package com.nexora.config;

import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * WHY DOES THIS EXIST?
 * The `roles` table needs to contain exactly the 4 fixed roles
 * (CUSTOMER, STORE_OWNER, DELIVERY_PARTNER, ADMIN) before any user
 * can be assigned one — a row in `user_roles` can't point to a role
 * that doesn't exist yet.
 *
 * CommandLineRunner is a Spring Boot interface: any bean implementing
 * it has its run() method executed once, automatically, right after
 * the application starts up. This is the standard way to seed
 * "reference data" (fixed lookup values) that the app always needs.
 *
 * It's safe to restart the app repeatedly — we check existsByName()
 * first, so roles are only inserted once, not duplicated on every boot.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(
                            Role.builder().name(roleName).build()
                    ));
        }
    }
}
