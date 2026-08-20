package com.nexora.store.spec;

import com.nexora.category.entity.Category;
import com.nexora.store.entity.Store;
import com.nexora.store.entity.StoreStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * Same pattern as ProductSpecifications — see that class's javadoc for
 * the full reasoning on why composable Specifications are used instead
 * of one derived-query method per filter combination.
 */
public class StoreSpecifications {

    private StoreSpecifications() {
    }

    public static Specification<Store> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String[] searchTerms = name.trim()
                .toLowerCase()
                .split("\\s+");

        return (root, query, cb) -> {
            var nameExpression = cb.lower(root.<String>get("name"));

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            for (String term : searchTerms) {
                predicates.add(
                        cb.like(
                                nameExpression,
                                "%" + term + "%"
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new jakarta.persistence.criteria.Predicate[0])
            );
        };
    }

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
                    categoryName.toLowerCase()
            );
        };
    }

    public static Specification<Store> hasStatus(StoreStatus status) {
        if (status == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}