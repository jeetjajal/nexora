package com.nexora.inventory.repository;

import com.nexora.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    /**
     * PHASE 6 — THE CORE CONCURRENCY-SAFE OPERATION.
     *
     * WHY NOT JUST "read quantity, check in Java, then save()"?
     * That naive approach — findByProductId() to read the current
     * quantity, an `if (quantity >= amount)` check in Java code, then
     * inventory.setQuantity(...) + save() — has a race condition.
     * Picture stock = 1, and two customers both trying to buy the
     * last item at almost the same instant:
     *
     *   Thread A: reads quantity = 1
     *   Thread B: reads quantity = 1        (before A has saved anything!)
     *   Thread A: checks 1 >= 1 → OK, sets quantity = 0, saves
     *   Thread B: checks 1 >= 1 → OK, sets quantity = 0, saves
     *
     * Both requests believe they succeeded. Two orders get created for
     * one item that existed. This is the classic "lost update" problem
     * — B's check used data that was already stale by the time it ran.
     *
     * THE FIX USED HERE: a single atomic SQL UPDATE that does the
     * check AND the write in one indivisible database operation:
     *
     *   UPDATE inventory
     *   SET quantity = quantity - :amount
     *   WHERE product_id = :productId AND quantity >= :amount
     *
     * The database guarantees this WHERE clause is evaluated against
     * the CURRENT committed value at the moment it acquires the row's
     * write lock — not a value some Java code read a moment earlier.
     * If Thread A's UPDATE runs first, it locks the row, sees
     * quantity=1, and reduces it to 0. Thread B's UPDATE then has to
     * wait for that lock; once it gets it, quantity is now 0, so its
     * own `quantity >= :amount` condition (0 >= 1) is false — the
     * UPDATE matches ZERO rows, and B's request correctly fails with
     * "insufficient stock" instead of silently overselling.
     *
     * The return value (an int — how many rows were actually updated)
     * is exactly how the service layer tells success (1) apart from
     * failure (0), without needing a separate read beforehand.
     *
     * WHAT THIS DOESN'T SOLVE YET: under very high concurrent load,
     * many transactions can still queue up waiting for the same row's
     * lock, which limits throughput for a single hot product. That's
     * a genuine scaling concern, but it's a PERFORMANCE problem, not a
     * CORRECTNESS problem — inventory still never goes negative.
     * Phase 21 explores @Version-based optimistic locking as an
     * alternative with different trade-offs for that scenario.
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :amount, i.updatedAt = :now " +
            "WHERE i.product.id = :productId AND i.quantity >= :amount")
    int reduceStockIfAvailable(
            @Param("productId") Long productId,
            @Param("amount") int amount,
            @Param("now") LocalDateTime now);

    /**
     * The mirror operation for restocking. Increasing stock has no
     * "insufficient" failure mode to guard against, but it's still
     * written as a single atomic UPDATE (rather than read-modify-save)
     * for the same reason: two simultaneous restock operations
     * (e.g. a supplier delivery recorded twice, or a delivery plus a
     * cancellation-triggered restock happening at once) should both
     * be reflected in the final total, not have one silently overwrite
     * the other.
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :amount, i.updatedAt = :now " +
            "WHERE i.product.id = :productId")
    int increaseStock(
            @Param("productId") Long productId,
            @Param("amount") int amount,
            @Param("now") LocalDateTime now);
}
