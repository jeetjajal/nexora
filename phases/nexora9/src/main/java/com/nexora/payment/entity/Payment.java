package com.nexora.payment.entity;

import com.nexora.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ONE ROW PER PAYMENT ATTEMPT, not one row per order — @ManyToOne to
 * Order, not @OneToOne. If a first attempt FAILS, PaymentService
 * creates a fresh Razorpay order and a fresh Payment row for a retry,
 * rather than mutating the failed one. This keeps a complete,
 * untouched history of every attempt, which matters for support/audit
 * ("why did my payment fail the first time?") and is exactly why the
 * original database design called this table `payments` (plural
 * attempts) alongside a separate `payment_transactions` log table.
 *
 * SECURITY: this entity — and this entire module — NEVER stores a
 * card number, CVV, UPI PIN, or bank password. Razorpay's Checkout
 * widget collects that information directly on Razorpay's own hosted
 * page; it never touches our frontend's JavaScript or our backend at
 * all. All we ever see or store is Razorpay's own opaque identifiers
 * (order id, payment id) and a status.
 */
@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_razorpay_order_id", columnNames = "razorpay_order_id")
        },
        indexes = {
                @Index(name = "idx_payments_order_id", columnList = "order_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Razorpay's own order identifier (e.g. "order_JZ9r3vXvXvXvXv") — an opaque string, not sensitive. */
    @Column(name = "razorpay_order_id", nullable = false, length = 100)
    private String razorpayOrderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String gateway = "RAZORPAY";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
