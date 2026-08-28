package com.nexora.store.spec;

import com.nexora.category.entity.Category;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic filtering for Store queries.
 *
 * Store search supports:
 *
 * 1. Name search
 * 2. Category filtering
 * 3. Status filtering
 *
 * Specifications are combined by StoreService, allowing any combination
 * of filters without creating a separate repository method for every
 * possible combination.
 *
 * NAME SEARCH:
 *
 * The name filter intentionally treats whitespace-separated words as
 * independent search terms.
 *
 * Example:
 *
 * Search:
 *     "pizza 1234"
 *
 * Store:
 *     "Rajkot Pizza Hub 1234"
 *
 * The store matches because:
 *
 *     pizza  -> exists in store name
 *     1234   -> exists in store name
 *
 * This is more useful for customer-facing search than requiring the
 * complete phrase to appear contiguously.
 */
public class StoreSpecifications {

    private StoreSpecifications() {
    }

    /**
     * Searches store names using case-insensitive word matching.
     *
     * Every non-blank word supplied by the caller must appear somewhere
     * in the store name.
     *
     * Example:
     *
     * name = "pizza 1234"
     *
     * becomes logically:
     *
     * name LIKE "%pizza%"
     * AND
     * name LIKE "%1234%"
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

            /*
             * Build one LIKE condition for every search term.
             *
             * Using AND means every supplied word must be present,
             * which prevents unrelated stores from matching just one
             * part of a multi-word search.
             */
            var predicates = new java.util.ArrayList<
                    jakarta.persistence.criteria.Predicate>();

            for (String term : searchTerms) {
                if (!term.isBlank()) {
                    String likePattern = "%" + term + "%";

                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("name")),
                                    likePattern
                            )
                    );
                }
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(
                    predicates.toArray(
                            new jakarta.persistence.criteria.Predicate[0]
                    )
            );
        };
    }

    /**
     * Filters stores by category name.
     *
     * The category relationship is joined dynamically through
     * Store.categories.
     *
     * distinct(true) prevents duplicate Store rows when the underlying
     * many-to-many join produces multiple matching rows.
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
     * Filters stores by their current operating status.
     *
     * Example:
     *
     * ?status=OPEN
     */
    public static Specification<Store> hasStatus(StoreStatus status) {
        if (status == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}