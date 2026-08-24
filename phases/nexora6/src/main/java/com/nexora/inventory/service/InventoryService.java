package com.nexora.inventory.service;

import com.nexora.auth.security.UserPrincipal;
import com.nexora.exception.ForbiddenOperationException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.dto.InventoryResponse;
import com.nexora.inventory.dto.InventoryUpdateRequest;
import com.nexora.inventory.dto.LowStockThresholdRequest;
import com.nexora.inventory.dto.StockAdjustmentRequest;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.mapper.InventoryMapper;
import com.nexora.inventory.repository.InventoryRepository;
import com.nexora.product.entity.Product;
import com.nexora.product.repository.ProductRepository;
import com.nexora.role.entity.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * PHASE 4 gave us view + "set to an absolute value." PHASE 6 adds the
 * two operations real inventory management actually needs day to day
 * — increase (restock) and reduce (sale/consumption) — built on the
 * atomic conditional UPDATE in InventoryRepository so that "never
 * allow negative inventory" is a guarantee enforced by the database
 * itself, not just a check in Java code that a concurrent request
 * could slip past. See InventoryRepository.reduceStockIfAvailable's
 * javadoc for the full race-condition walkthrough.
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

    /**
     * PHASE 4 (unchanged): sets stock to an absolute value, e.g.
     * correcting a stock count after a manual audit. Still the right
     * tool for "I counted the shelf and there are exactly 12 left,"
     * as opposed to increase/reduce which are for "add 12" / "sell 3."
     */
    @Transactional
    public InventoryResponse updateStock(Long productId, InventoryUpdateRequest request, UserPrincipal caller) {
        Product product = findProductOrThrow(productId);
        assertCallerOwnsStoreOrIsAdmin(product, caller);

        Inventory inventory = findInventoryOrThrow(productId);
        inventory.setQuantity(request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);
        return InventoryMapper.toResponse(saved);
    }

    /**
     * PHASE 6: restock. Uses the atomic UPDATE (increaseStock) rather
     * than a read-modify-save cycle — see InventoryRepository for why
     * that matters even for increases, not just reduces.
     */
    @Transactional
    public InventoryResponse increaseStock(Long productId, StockAdjustmentRequest request, UserPrincipal caller) {
        Product product = findProductOrThrow(productId);
        assertCallerOwnsStoreOrIsAdmin(product, caller);

        // Confirms an Inventory row actually exists for this product
        // before attempting the update, so a bad productId fails with
        // a clean 404 rather than a silently-no-op UPDATE (0 rows
        // affected, but the caller wouldn't know why).
        findInventoryOrThrow(productId);

        inventoryRepository.increaseStock(productId, request.getAmount(), LocalDateTime.now());

        // Re-fetch rather than trust an in-memory entity: the UPDATE
        // above ran as a bulk JPQL query, which bypasses the
        // persistence context — reading fresh guarantees the response
        // reflects exactly what's now committed in the database.
        Inventory updated = findInventoryOrThrow(productId);
        return InventoryMapper.toResponse(updated);
    }

    /**
     * PHASE 6: sell/consume stock, THE concurrency-safe operation this
     * phase centers on. Throws InsufficientStockException (409) if the
     * atomic UPDATE affects zero rows — meaning, at the instant the
     * database evaluated the condition, there wasn't enough stock left.
     */
    @Transactional
    public InventoryResponse reduceStock(Long productId, StockAdjustmentRequest request, UserPrincipal caller) {
        Product product = findProductOrThrow(productId);
        assertCallerOwnsStoreOrIsAdmin(product, caller);

        // Confirms an Inventory row exists before attempting the
        // update, so a bad productId fails with a clean 404 rather
        // than a silently-no-op UPDATE.
        findInventoryOrThrow(productId);

        int rowsUpdated = inventoryRepository.reduceStockIfAvailable(
                productId, request.getAmount(), LocalDateTime.now());

        if (rowsUpdated == 0) {
            // The row existed (we just fetched it above) but the
            // WHERE quantity >= :amount condition didn't match at
            // update time — genuinely insufficient stock, not a
            // missing product. Re-read the CURRENT quantity for an
            // accurate error message that reflects exactly what's in
            // the database right now.
            Inventory current = findInventoryOrThrow(productId);
            throw new InsufficientStockException(productId, request.getAmount(), current.getQuantity());
        }

        Inventory updated = findInventoryOrThrow(productId);
        return InventoryMapper.toResponse(updated);
    }

    /**
     * PHASE 6: lets a store owner tune per-product low-stock
     * sensitivity (see Inventory.lowStockThreshold's javadoc).
     */
    @Transactional
    public InventoryResponse updateLowStockThreshold(
            Long productId, LowStockThresholdRequest request, UserPrincipal caller) {

        Product product = findProductOrThrow(productId);
        assertCallerOwnsStoreOrIsAdmin(product, caller);

        Inventory inventory = findInventoryOrThrow(productId);
        inventory.setLowStockThreshold(request.getThreshold());

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
