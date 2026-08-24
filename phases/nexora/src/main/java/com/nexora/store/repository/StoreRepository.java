package com.nexora.store.repository;

import com.nexora.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByOwnerId(Long ownerId);

    /**
     * Used by CategoryService to check "is any store still using this
     * category?" before allowing a delete — Spring Data JPA derives
     * the join query automatically from the property path
     * Store.categories -> Category.id.
     */
    boolean existsByCategoriesId(Long categoryId);
}
