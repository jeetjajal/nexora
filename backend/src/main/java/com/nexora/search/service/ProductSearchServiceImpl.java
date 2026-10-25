package com.nexora.search.service;

import com.nexora.category.entity.Category;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.product.spec.ProductSpecifications;
import com.nexora.search.document.ProductDocument;
import com.nexora.search.document.StoreDocument;
import com.nexora.search.dto.AutocompleteResponse;
import com.nexora.search.dto.ProductSearchResult;
import com.nexora.search.dto.StoreSearchResult;
import com.nexora.search.repository.ProductSearchRepository;
import com.nexora.search.repository.StoreSearchRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import com.nexora.store.spec.StoreSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    @Autowired(required = false)
    private ProductSearchRepository productSearchRepository;

    @Autowired(required = false)
    private StoreSearchRepository storeSearchRepository;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${nexora.search.enabled:true}")
    private boolean searchEnabled;

    public ProductSearchServiceImpl(ProductRepository productRepository, StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    @Override
    public Page<ProductSearchResult> searchProducts(String query,
                                                    String category,
                                                    BigDecimal minPrice,
                                                    BigDecimal maxPrice,
                                                    Boolean available,
                                                    Long storeId,
                                                    Pageable pageable) {
        if (searchEnabled && elasticsearchOperations != null && productSearchRepository != null) {
            try {
                return searchProductsElasticsearch(query, category, minPrice, maxPrice, available, storeId, pageable);
            } catch (Exception e) {
                log.warn("Elasticsearch query failed, falling back to database search: {}", e.getMessage());
            }
        }
        return searchProductsDatabaseFallback(query, category, minPrice, maxPrice, available, storeId, pageable);
    }

    @Override
    public Page<StoreSearchResult> searchStores(String query, String category, Pageable pageable) {
        if (searchEnabled && elasticsearchOperations != null && storeSearchRepository != null) {
            try {
                return searchStoresElasticsearch(query, category, pageable);
            } catch (Exception e) {
                log.warn("Elasticsearch query failed for stores, falling back to database: {}", e.getMessage());
            }
        }
        return searchStoresDatabaseFallback(query, category, pageable);
    }

    @Override
    public AutocompleteResponse autocomplete(String prefix, int limit) {
        int maxSuggestions = limit > 0 ? Math.min(limit, 20) : 10;
        Set<String> suggestionSet = new LinkedHashSet<>();
        List<ProductSearchResult> topProducts = new ArrayList<>();
        List<StoreSearchResult> topStores = new ArrayList<>();

        if (!StringUtils.hasText(prefix)) {
            return AutocompleteResponse.builder()
                    .query("")
                    .suggestions(List.of())
                    .products(List.of())
                    .stores(List.of())
                    .build();
        }

        String term = prefix.trim();

        if (searchEnabled && productSearchRepository != null && storeSearchRepository != null) {
            try {
                List<ProductDocument> esProducts = productSearchRepository.findByNameStartingWithIgnoreCase(term);
                for (ProductDocument doc : esProducts) {
                    suggestionSet.add(doc.getName());
                    if (topProducts.size() < 5) {
                        topProducts.add(mapDocumentToProductResult(doc));
                    }
                }

                List<StoreDocument> esStores = storeSearchRepository.findByNameStartingWithIgnoreCase(term);
                for (StoreDocument doc : esStores) {
                    suggestionSet.add(doc.getName());
                    if (topStores.size() < 5) {
                        topStores.add(mapDocumentToStoreResult(doc));
                    }
                }
            } catch (Exception e) {
                log.warn("Elasticsearch autocomplete failed, falling back to database: {}", e.getMessage());
                return autocompleteDatabaseFallback(term, maxSuggestions);
            }
        } else {
            return autocompleteDatabaseFallback(term, maxSuggestions);
        }

        List<String> suggestions = suggestionSet.stream().limit(maxSuggestions).toList();
        return AutocompleteResponse.builder()
                .query(term)
                .suggestions(suggestions)
                .products(topProducts)
                .stores(topStores)
                .build();
    }

    private Page<ProductSearchResult> searchProductsElasticsearch(String query,
                                                                 String category,
                                                                 BigDecimal minPrice,
                                                                 BigDecimal maxPrice,
                                                                 Boolean available,
                                                                 Long storeId,
                                                                 Pageable pageable) {
        Criteria criteria = new Criteria();

        if (StringUtils.hasText(query)) {
            String trimmed = query.trim();
            criteria = criteria.subCriteria(
                    new Criteria("name").contains(trimmed)
                            .or(new Criteria("description").contains(trimmed))
            );
        }

        if (StringUtils.hasText(category)) {
            criteria = criteria.and(new Criteria("categories").is(category.trim()));
        }

        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice.doubleValue()));
        }

        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice.doubleValue()));
        }

        if (available != null) {
            criteria = criteria.and(new Criteria("available").is(available));
        }

        if (storeId != null) {
            criteria = criteria.and(new Criteria("storeId").is(storeId));
        }

        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(criteriaQuery, ProductDocument.class);

        List<ProductSearchResult> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::mapDocumentToProductResult)
                .toList();

        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }

    private Page<StoreSearchResult> searchStoresElasticsearch(String query, String category, Pageable pageable) {
        Criteria criteria = new Criteria();

        if (StringUtils.hasText(query)) {
            String trimmed = query.trim();
            criteria = criteria.subCriteria(
                    new Criteria("name").contains(trimmed)
                            .or(new Criteria("description").contains(trimmed))
            );
        }

        if (StringUtils.hasText(category)) {
            criteria = criteria.and(new Criteria("categories").is(category.trim()));
        }

        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<StoreDocument> hits = elasticsearchOperations.search(criteriaQuery, StoreDocument.class);

        List<StoreSearchResult> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::mapDocumentToStoreResult)
                .toList();

        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }

    private Page<ProductSearchResult> searchProductsDatabaseFallback(String query,
                                                                    String category,
                                                                    BigDecimal minPrice,
                                                                    BigDecimal maxPrice,
                                                                    Boolean available,
                                                                    Long storeId,
                                                                    Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.nameContains(query))
                .and(ProductSpecifications.hasCategoryName(category))
                .and(ProductSpecifications.priceGreaterThanOrEqual(minPrice))
                .and(ProductSpecifications.priceLessThanOrEqual(maxPrice))
                .and(ProductSpecifications.isAvailable(available))
                .and(ProductSpecifications.belongsToStore(storeId));

        Page<Product> dbPage = productRepository.findAll(spec, pageable);
        List<ProductSearchResult> results = dbPage.getContent().stream()
                .map(this::mapProductToResult)
                .toList();

        return new PageImpl<>(results, pageable, dbPage.getTotalElements());
    }

    private Page<StoreSearchResult> searchStoresDatabaseFallback(String query, String category, Pageable pageable) {
        Specification<Store> spec = Specification.where(StoreSpecifications.nameContains(query))
                .and(StoreSpecifications.hasCategoryName(category));

        Page<Store> dbPage = storeRepository.findAll(spec, pageable);
        List<StoreSearchResult> results = dbPage.getContent().stream()
                .map(this::mapStoreToResult)
                .toList();

        return new PageImpl<>(results, pageable, dbPage.getTotalElements());
    }

    private AutocompleteResponse autocompleteDatabaseFallback(String term, int maxSuggestions) {
        Set<String> suggestionSet = new LinkedHashSet<>();
        List<ProductSearchResult> topProducts = new ArrayList<>();
        List<StoreSearchResult> topStores = new ArrayList<>();

        Page<Product> productPage = productRepository.findAll(
                ProductSpecifications.nameContains(term),
                PageRequest.of(0, maxSuggestions)
        );
        for (Product p : productPage.getContent()) {
            suggestionSet.add(p.getName());
            if (topProducts.size() < 5) {
                topProducts.add(mapProductToResult(p));
            }
        }

        Page<Store> storePage = storeRepository.findAll(
                StoreSpecifications.nameContains(term),
                PageRequest.of(0, maxSuggestions)
        );
        for (Store s : storePage.getContent()) {
            suggestionSet.add(s.getName());
            if (topStores.size() < 5) {
                topStores.add(mapStoreToResult(s));
            }
        }

        List<String> suggestions = suggestionSet.stream().limit(maxSuggestions).toList();
        return AutocompleteResponse.builder()
                .query(term)
                .suggestions(suggestions)
                .products(topProducts)
                .stores(topStores)
                .build();
    }

    private ProductSearchResult mapDocumentToProductResult(ProductDocument doc) {
        return ProductSearchResult.builder()
                .id(doc.getProductId())
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice() != null ? BigDecimal.valueOf(doc.getPrice()) : null)
                .discount(doc.getDiscount() != null ? BigDecimal.valueOf(doc.getDiscount()) : null)
                .imageUrl(doc.getImageUrl())
                .available(Boolean.TRUE.equals(doc.getAvailable()))
                .storeId(doc.getStoreId())
                .storeName(doc.getStoreName())
                .categories(doc.getCategories() != null ? doc.getCategories() : List.of())
                .source("ELASTICSEARCH")
                .build();
    }

    private StoreSearchResult mapDocumentToStoreResult(StoreDocument doc) {
        return StoreSearchResult.builder()
                .id(doc.getStoreId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .imageUrl(doc.getImageUrl())
                .categories(doc.getCategories() != null ? doc.getCategories() : List.of())
                .source("ELASTICSEARCH")
                .build();
    }

    private ProductSearchResult mapProductToResult(Product product) {
        List<String> categoryNames = product.getCategories() != null
                ? product.getCategories().stream().map(Category::getName).toList()
                : List.of();

        return ProductSearchResult.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discount(product.getDiscount())
                .imageUrl(product.getImageUrl())
                .available(product.isAvailable())
                .storeId(product.getStore() != null ? product.getStore().getId() : null)
                .storeName(product.getStore() != null ? product.getStore().getName() : null)
                .categories(categoryNames)
                .source("DATABASE_FALLBACK")
                .build();
    }

    private StoreSearchResult mapStoreToResult(Store store) {
        List<String> categoryNames = store.getCategories() != null
                ? store.getCategories().stream().map(Category::getName).toList()
                : List.of();

        return StoreSearchResult.builder()
                .id(store.getId())
                .name(store.getName())
                .description(store.getDescription())
                .status(store.getStatus() != null ? store.getStatus().name() : null)
                .imageUrl(store.getImageUrl())
                .categories(categoryNames)
                .source("DATABASE_FALLBACK")
                .build();
    }
}
