package com.nexora.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An append-only audit log: one row per gateway interaction for a
 * given Payment (order creation, the client's reported result, and —
 * starting Phase 10 — webhook events). We never UPDATE a
 * PaymentTransaction row, only ever INSERT new ones, so there's
 * always a complete, untampered history of everything that happened
 * for a payment, in order.
 *
 * `rawPayload` stores a short, human-readable summary of what
 * happened — NEVER the actual card/UPI details Razorpay's hosted
 * checkout collected (we never receive those in the first place).
 */
@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_transactions_payment_id", columnList = "payment_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentEventType eventType;

    /** Razorpay's payment identifier, once one exists (null until the client reports a result). */
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "raw_payload", length = 1000)
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
