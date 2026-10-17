package com.nexora.search.repository;

import com.nexora.search.document.StoreDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreSearchRepository extends ElasticsearchRepository<StoreDocument, String> {

    List<StoreDocument> findByNameContainingIgnoreCase(String name);

    Page<StoreDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<StoreDocument> findByNameStartingWithIgnoreCase(String prefix);

    void deleteByStoreId(Long storeId);
}
