package com.nexora.user.mapper;

import com.nexora.user.dto.UserResponse;
import com.nexora.user.entity.User;

/**
 * WHAT IS A MAPPER?
 * A small, focused class whose only job is converting between
 * Entity <-> DTO. Keeping this logic out of the Service class keeps
 * each class focused on one responsibility (Single Responsibility
 * Principle from SOLID).
 *
 * In later phases, this could be replaced with MapStruct (a library
 * that auto-generates mapper code), but a plain manual mapper is the
 * clearest way to learn what's actually happening.
 */
public class UserMapper {

    private UserMapper() {
        // utility class — prevent instantiation
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
