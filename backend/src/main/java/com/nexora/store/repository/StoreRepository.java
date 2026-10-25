package com.nexora.store.repository;

import com.nexora.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * PHASE 5 ADDITION: JpaSpecificationExecutor<Store> — same reasoning
 * as ProductRepository. Lets StoreService build a dynamic query for
 * name/category/status filtering without a combinatorial explosion of
 * derived query methods.
 */
public interface StoreRepository extends JpaRepository<Store, Long>, JpaSpecificationExecutor<Store> {

    List<Store> findByOwnerId(Long ownerId);

    /**
     * Used by CategoryService to check "is any store still using this
     * category?" before allowing a delete — Spring Data JPA derives
     * the join query automatically from the property path
     * Store.categories -> Category.id.
     */
    boolean existsByCategoriesId(Long categoryId);
}
