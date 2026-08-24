package com.nexora.inventory.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.dto.InventoryResponse;
import com.nexora.inventory.dto.InventoryUpdateRequest;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.mapper.InventoryMapper;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PHASE 4 SCOPE ONLY: check current stock and set it to an absolute
 * value. This intentionally does NOT yet handle the concurrency
 * problem — two simultaneous "buy the last item" requests both
 * succeeding and overselling. That's explicitly Phase 6 (basic
 * transactional safety) and Phase 21 (optimistic/pessimistic locking
 * under real concurrent load). Here, we're just building the
 * management surface store owners/admins use to view and set stock.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryForProduct(Long productId) {
        Inventory inventory = findInventoryOrThrow(productId);
        return InventoryMapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(Long productId) {
        Inventory inventory = findInventoryOrThrow(productId);
        return inventory.getQuantity() > 0;
    }

    @Transactional
    public InventoryResponse updateStock(Long productId, InventoryUpdateRequest request, UserPrincipal caller) {
        Product product = findProductOrThrow(productId);
        assertCallerOwnsStoreOrIsAdmin(product, caller);

        Inventory inventory = findInventoryOrThrow(productId);

        // The @Min(0) constraint on InventoryUpdateRequest already
        // rejects a negative quantity at the DTO-validation layer
        // (400 Bad Request) before we ever reach this line — this is
        // just the domain-level guarantee restated: inventory can
        // never go negative, full stop.
        inventory.setQuantity(request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);
        return InventoryMapper.toResponse(saved);
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private Inventory findInventoryOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for product id: " + productId));
    }

    private void assertCallerOwnsStoreOrIsAdmin(Product product, UserPrincipal caller) {
        boolean isAdmin = caller.getUser().getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);

        if (isAdmin) {
            return;
        }

        if (!product.getStore().getOwner().getId().equals(caller.getId())) {
            throw new ForbiddenOperationException(
                    "You do not have permission to manage inventory for a product you do not own.");
        }
    }
}
