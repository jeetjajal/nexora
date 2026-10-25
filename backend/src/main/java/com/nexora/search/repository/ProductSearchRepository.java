package com.nexora.search.repository;

import com.nexora.search.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    List<ProductDocument> findByNameContainingIgnoreCase(String name);

    Page<ProductDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<ProductDocument> findByNameStartingWithIgnoreCase(String prefix);

    void deleteByProductId(Long productId);
}
