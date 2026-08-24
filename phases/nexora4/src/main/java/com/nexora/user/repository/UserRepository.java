package com.nexora.user.repository;

import com.nexora.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * WHAT IS A REPOSITORY?
 * Spring Data JPA lets you skip writing SQL for common operations.
 * By extending JpaRepository<User, Long> we automatically get:
 *   save(user), findById(id), findAll(), deleteById(id), count(), etc.
 *
 * For anything custom (like "find a user by email"), we just declare
 * a method signature following Spring Data's naming convention, and
 * Spring Data JPA generates the SQL query for us behind the scenes.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
