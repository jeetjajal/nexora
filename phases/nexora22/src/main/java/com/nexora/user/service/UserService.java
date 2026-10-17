package com.nexora.user.service;

import com.nexora.exception.DuplicateEmailException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.role.entity.Role;
import com.nexora.role.entity.RoleName;
import com.nexora.role.repository.RoleRepository;
import com.nexora.user.dto.RegisterRequest;
import com.nexora.user.dto.UserResponse;
import com.nexora.user.entity.User;
import com.nexora.user.mapper.UserMapper;
import com.nexora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * WHERE BUSINESS LOGIC LIVES
 * The Service layer sits between the Controller (HTTP concerns) and
 * the Repository (database concerns). All "rules" about how
 * registration should behave live here, e.g.:
 *   - Reject duplicate emails
 *   - Hash the password before saving
 *   - Decide the default role
 *
 * Controllers stay thin and simply delegate to this class.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // BCrypt is a one-way hashing algorithm designed for passwords.
    // Even we (the developers) can never "decrypt" a hash back to the
    // original password — we can only compare a new password attempt
    // against the stored hash (used later in Phase 3 login).
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // Every self-registered user starts as a CUSTOMER. The role
        // row itself is guaranteed to exist because DataSeeder inserts
        // all 4 fixed roles on application startup (see config/DataSeeder.java).
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException(
                        "CUSTOMER role not found — DataSeeder should have created it on startup"));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .status("ACTIVE")
                .roles(Set.of(customerRole))
                .build();

        User savedUser = userRepository.save(user);

        return UserMapper.toResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserMapper.toResponse(user);
    }
}
