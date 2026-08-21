package com.nexora.store.spec;

import com.nexora.category.entity.Category;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class StoreSpecifications {

    private StoreSpecifications() {
    }

    /**
     * Searches the store name using all words supplied by the user.
     *
     * Example:
     *
     * Search:
     * "pizza abc123"
     *
     * Store:
     * "Rajkot Pizza Hub abc123"
     *
     * Both search terms must occur somewhere in the store name.
     */
    public static Specification<Store> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String[] searchTerms = name
                .trim()
                .toLowerCase()
                .split("\\s+");

        return (root, query, cb) -> {

            Predicate[] predicates = new Predicate[searchTerms.length];

            for (int i = 0; i < searchTerms.length; i++) {
                String likePattern = "%" + searchTerms[i] + "%";

                predicates[i] = cb.like(
                        cb.lower(root.<String>get("name")),
                        cb.literal(likePattern)
                );
            }

            return cb.and(predicates);
        };
    }

    /**
     * Filters stores by category name.
     */
    public static Specification<Store> hasCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }

        return (root, query, cb) -> {
            query.distinct(true);

            Join<Store, Category> categoryJoin =
                    root.join("categories");

            return cb.equal(
                    cb.lower(categoryJoin.get("name")),
                    categoryName.trim().toLowerCase()
            );
        };
    }

    /**
     * Filters stores by status.
     */
    public static Specification<Store> hasStatus(StoreStatus status) {
        if (status == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}