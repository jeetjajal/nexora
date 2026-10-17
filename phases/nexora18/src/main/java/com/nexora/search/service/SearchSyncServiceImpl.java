package com.nexora.search.service;

import com.nexora.category.entity.Category;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.search.document.ProductDocument;
import com.nexora.search.document.StoreDocument;
import com.nexora.search.dto.SearchSyncResponse;
import com.nexora.search.repository.ProductSearchRepository;
import com.nexora.search.repository.StoreSearchRepository;
import com.nexora.store.entity.Store;
import com.nexora.store.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SearchSyncServiceImpl implements SearchSyncService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    @Autowired(required = false)
    private ProductSearchRepository productSearchRepository;

    @Autowired(required = false)
    private StoreSearchRepository storeSearchRepository;

    public SearchSyncServiceImpl(ProductRepository productRepository, StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSyncResponse syncAll() {
        long startTime = System.currentTimeMillis();

        if (productSearchRepository == null || storeSearchRepository == null) {
            log.warn("Elasticsearch repositories are not active. Sync aborted.");
            return SearchSyncResponse.builder()
                    .status("SKIPPED")
                    .message("Elasticsearch repositories are not active or cluster is unavailable.")
                    .productsSynced(0)
                    .storesSynced(0)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            List<Product> products = productRepository.findAll();
            List<ProductDocument> productDocs = products.stream()
                    .map(this::toProductDocument)
                    .toList();
            productSearchRepository.saveAll(productDocs);

            List<Store> stores = storeRepository.findAll();
            List<StoreDocument> storeDocs = stores.stream()
                    .map(this::toStoreDocument)
                    .toList();
            storeSearchRepository.saveAll(storeDocs);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Search catalog sync complete: {} products, {} stores synced in {} ms",
                    productDocs.size(), storeDocs.size(), duration);

            return SearchSyncResponse.builder()
                    .status("SUCCESS")
                    .message("Elasticsearch index synchronization complete.")
                    .productsSynced(productDocs.size())
                    .storesSynced(storeDocs.size())
                    .durationMs(duration)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to sync search catalog: {}", e.getMessage(), e);
            return SearchSyncResponse.builder()
                    .status("FAILED")
                    .message("Search sync failed: " + e.getMessage())
                    .productsSynced(0)
                    .storesSynced(0)
                    .durationMs(duration)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncProduct(Long productId) {
        if (productSearchRepository == null || productId == null) {
            return;
        }
        try {
            productRepository.findById(productId).ifPresent(p -> {
                ProductDocument doc = toProductDocument(p);
                productSearchRepository.save(doc);
                log.debug("Synchronized product id={} to Elasticsearch", productId);
            });
        } catch (Exception e) {
            log.warn("Could not sync product {} to Elasticsearch: {}", productId, e.getMessage());
        }
    }

    @Override
    public void deleteProduct(Long productId) {
        if (productSearchRepository == null || productId == null) {
            return;
        }
        try {
            productSearchRepository.deleteById("prod-" + productId);
            log.debug("Deleted product id={} from Elasticsearch", productId);
        } catch (Exception e) {
            log.warn("Could not delete product {} from Elasticsearch: {}", productId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void syncStore(Long storeId) {
        if (storeSearchRepository == null || storeId == null) {
            return;
        }
        try {
            storeRepository.findById(storeId).ifPresent(s -> {
                StoreDocument doc = toStoreDocument(s);
                storeSearchRepository.save(doc);
                log.debug("Synchronized store id={} to Elasticsearch", storeId);
            });
        } catch (Exception e) {
            log.warn("Could not sync store {} to Elasticsearch: {}", storeId, e.getMessage());
        }
    }

    @Override
    public void deleteStore(Long storeId) {
        if (storeSearchRepository == null || storeId == null) {
            return;
        }
        try {
            storeSearchRepository.deleteById("store-" + storeId);
            log.debug("Deleted store id={} from Elasticsearch", storeId);
        } catch (Exception e) {
            log.warn("Could not delete store {} from Elasticsearch: {}", storeId, e.getMessage());
        }
    }

    private ProductDocument toProductDocument(Product p) {
        List<String> categoryNames = p.getCategories() != null
                ? p.getCategories().stream().map(Category::getName).toList()
                : List.of();

        return ProductDocument.builder()
                .id("prod-" + p.getId())
                .productId(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice() != null ? p.getPrice().doubleValue() : null)
                .discount(p.getDiscount() != null ? p.getDiscount().doubleValue() : null)
                .imageUrl(p.getImageUrl())
                .available(p.isAvailable())
                .storeId(p.getStore() != null ? p.getStore().getId() : null)
                .storeName(p.getStore() != null ? p.getStore().getName() : null)
                .categories(categoryNames)
                .build();
    }

    private StoreDocument toStoreDocument(Store s) {
        List<String> categoryNames = s.getCategories() != null
                ? s.getCategories().stream().map(Category::getName).toList()
                : List.of();

        return StoreDocument.builder()
                .id("store-" + s.getId())
                .storeId(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .imageUrl(s.getImageUrl())
                .categories(categoryNames)
                .build();
    }
}
