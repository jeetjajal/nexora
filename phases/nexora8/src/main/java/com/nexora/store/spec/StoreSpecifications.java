package com.nexora.store.spec;

import com.nexora.category.entity.Category;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications used for Store search and filtering.
 */
public final class StoreSpecifications {

    private StoreSpecifications() {
    }

    /**
     * Case-insensitive substring search on store name.
     *
     * Example:
     *
     * Store name:
     * Rajkot Pizza Hub abc123
     *
     * Search:
     * pizza
     *
     * Result:
     * MATCH
     */
    public static Specification<Store> nameContains(String name) {

        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String search = name.trim().toLowerCase();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.<String>get("name")
                        ),
                        "%" + search + "%"
                );
    }

    /**
     * Filter stores by category name.
     *
     * Case-insensitive exact category-name match.
     */
    public static Specification<Store> hasCategoryName(
            String categoryName) {

        if (categoryName == null ||
                categoryName.trim().isEmpty()) {

            return null;
        }

        return (root, query, criteriaBuilder) -> {

            query.distinct(true);

            Join<Store, Category> category =
                    root.join(
                            "categories",
                            JoinType.INNER
                    );

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(
                            category.<String>get("name")
                    ),
                    categoryName.trim().toLowerCase()
            );
        };
    }

    /**
     * Filter stores by status.
     */
    public static Specification<Store> hasStatus(
            StoreStatus status) {

        if (status == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }
}