package com.nexora.product.spec;

import com.nexora.category.entity.Category;
import com.nexora.product.entity.Product;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * WHAT IS A Specification?
 * A Specification<Product> is just an object that knows how to add ONE
 * WHERE-clause condition to a query, using JPA's Criteria API instead
 * of a hand-written JPQL/SQL string. The real value is that they
 * COMPOSE: ProductService combines only the filters the customer
 * actually asked for (Specification.where(a).and(b).and(c)...), so a
 * search with just "minPrice" produces a different, simpler SQL query
 * than a search with "name + category + minPrice + maxPrice + available"
 * — all from the same small set of building blocks below, instead of
 * needing a hand-written derived-query method for every possible
 * combination (which would be 2^5 = 32 methods for 5 optional filters).
 *
 * Each method returns null when its filter wasn't requested; nulls are
 * filtered out by ProductService before combining, so an absent filter
 * contributes NO condition to the query at all (not "match everything",
 * which would be different — it just means "don't restrict on this
 * dimension").
 */
public class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String likePattern = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), likePattern);
    }

    public static Specification<Product> hasCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            // Product.categories is a @ManyToMany Set<Category> — a JOIN
            // is required to filter on a property of the related entity.
            query.distinct(true); // avoid duplicate rows if other joins are added later
            Join<Product, Category> categoryJoin = root.join("categories");
            return cb.equal(cb.lower(categoryJoin.get("name")), categoryName.toLowerCase());
        };
    }

    public static Specification<Product> belongsToStore(Long storeId) {
        if (storeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("store").get("id"), storeId);
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> isAvailable(Boolean available) {
        if (available == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("available"), available);
    }
}
