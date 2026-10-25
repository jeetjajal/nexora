package com.nexora.search.service;

import com.nexora.search.dto.SearchSyncResponse;

public interface SearchSyncService {

    SearchSyncResponse syncAll();

    void syncProduct(Long productId);

    void deleteProduct(Long productId);

    void syncStore(Long storeId);

    void deleteStore(Long storeId);
}
