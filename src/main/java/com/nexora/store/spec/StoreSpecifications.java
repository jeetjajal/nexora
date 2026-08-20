package com.nexora.store.spec;

import com.nexora.category.entity.Category;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * PHASE 5/6:
 * Dynamic search and filtering specifications for Store.
 *
 * Supports:
 * - Name search
 * - Category filtering
 * - Status filtering
 *
 * Name search is token-based so a request such as:
 *
 *     ?name=pizza ABC123
 *
 * can find:
 *
 *     Rajkot Pizza Hub ABC123
 *
 * Each search word must be present somewhere in the store name.
 */
public class StoreSpecifications {

    private StoreSpecifications() {
    }

    /**
     * Searches store names using individual words.
     *
     * Example:
     *
     * Input:
     *     "pizza abc123"
     *
     * Database name:
     *     "Rajkot Pizza Hub ABC123"
     *
     * Both "pizza" and "abc123" are found, so the store matches.
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

            var nameExpression =
                    cb.lower(root.<String>get("name"));

            List<Predicate> predicates = new ArrayList<>();

            for (String term : searchTerms) {

                if (!term.isBlank()) {
                    predicates.add(
                            cb.like(
                                    nameExpression,
                                    "%" + term + "%"
                            )
                    );
                }
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }

    /**
     * Filters stores by category name.
     *
     * Example:
     *
     *     ?categoryName=Fast Food
     */
    public static Specification<Store> hasCategoryName(
            String categoryName) {

        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }

        String normalizedCategoryName =
                categoryName.trim().toLowerCase();

        return (root, query, cb) -> {

            query.distinct(true);

            Join<Store, Category> categoryJoin =
                    root.join("categories");

            return cb.equal(
                    cb.lower(categoryJoin.get("name")),
                    normalizedCategoryName
            );
        };
    }

    /**
     * Filters stores by operating status.
     *
     * Example:
     *
     *     ?status=OPEN
     */
    public static Specification<Store> hasStatus(
            StoreStatus status) {

        if (status == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(
                        root.get("status"),
                        status
                );
    }
}