package com.nexora.concurrency.service;

import com.nexora.exception.ResourceNotFoundException;
import com.nexora.inventory.entity.Inventory;
import com.nexora.inventory.exception.InsufficientStockException;
import com.nexora.inventory.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles individual transactional boundaries for concurrent purchases.
 * Using REQUIRES_NEW guarantees each concurrent thread runs in its own
 * real, isolated database transaction even when spawned by thread pools.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConcurrencyTransactionManager {

    private final InventoryRepository inventoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Strategy 1: Pessimistic Write Locking (SELECT ... FOR UPDATE).
     * Locks the target row immediately at database engine level.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public int executePessimisticPurchase(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithPessimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No inventory found for product: " + productId));

        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, inventory.getQuantity());
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.saveAndFlush(inventory);
        return inventory.getQuantity();
    }

    /**
     * Strategy 2: Optimistic Locking (@Version check).
     * Does not block other transactions. On commit, Hibernate verifies version matches.
     * Throws ObjectOptimisticLockingFailureException if another thread updated the version first.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int executeOptimisticPurchase(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithOptimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No inventory found for product: " + productId));

        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, inventory.getQuantity());
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.saveAndFlush(inventory);
        return inventory.getQuantity();
    }

    /**
     * Strategy 3: Atomic Conditional SQL UPDATE (Phase 6 baseline).
     * Single indivisible SQL query: UPDATE inventory SET quantity = quantity - :amount WHERE quantity >= :amount.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int executeAtomicUpdatePurchase(Long productId, int quantity) {
        int rowsUpdated = inventoryRepository.reduceStockIfAvailable(productId, quantity, LocalDateTime.now());
        if (rowsUpdated == 0) {
            Inventory current = inventoryRepository.findByProductId(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("No inventory found for product: " + productId));
            throw new InsufficientStockException(productId, quantity, current.getQuantity());
        }
        return inventoryRepository.findByProductId(productId).map(Inventory::getQuantity).orElse(0);
    }

    /**
     * Resets inventory quantity in an isolated transaction before running benchmark.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetStock(Long productId, int stock) {
        if (entityManager != null) {
            entityManager.clear();
        }
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No inventory found for product: " + productId));
        inventory.setQuantity(stock);
        inventoryRepository.saveAndFlush(inventory);
        if (entityManager != null) {
            entityManager.clear();
        }
    }

    /**
     * Reads current stock committed in database in an isolated fresh transaction,
     * ensuring persistence context cache does not return stale data.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public int getFreshStock(Long productId) {
        if (entityManager != null) {
            entityManager.clear();
        }
        return inventoryRepository.findByProductId(productId)
                .map(Inventory::getQuantity)
                .orElse(0);
    }
}
