package com.nexora.store.repository;

import com.nexora.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StoreRepository
        extends JpaRepository<Store, Long>,
                JpaSpecificationExecutor<Store> {

    /**
     * Find all stores owned by a particular user.
     */
    List<Store> findByOwnerId(Long ownerId);

    /**
     * Check whether a category is currently used by any store.
     */
    boolean existsByCategoriesId(Long categoryId);

    /**
     * Case-insensitive substring search by store name.
     *
     * Example:
     *
     * Database:
     * Rajkot Pizza Hub ABC123
     *
     * Search:
     * pizza abc123
     *
     * Result:
     * Rajkot Pizza Hub ABC123
     */
    Page<Store> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
}