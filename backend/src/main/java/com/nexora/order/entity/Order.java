package com.nexora.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexora.address.entity.Address;
import com.nexora.store.entity.Store;
import com.nexora.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Core Quick Commerce Order entity with authoritative state machine,
 * delivery OTP verification, and store picker/packer audit metadata.
 */
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_store_id", columnList = "store_id"),
                @Index(name = "idx_orders_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    private Address deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_otp_hash", length = 64)
    private String deliveryOtpHash;

    @Column(name = "otp_attempts")
    @Builder.Default
    private Integer otpAttempts = 0;

    @Column(name = "payment_method", length = 30)
    @Builder.Default
    private String paymentMethod = "RAZORPAY";

    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /** Store Operations & Picking / Packing Audit Trail */
    @Column(name = "picker_id")
    private Long pickerId;

    @Column(name = "picker_name", length = 100)
    private String pickerName;

    @Column(name = "picking_started_at")
    private LocalDateTime pickingStartedAt;

    @Column(name = "picking_completed_at")
    private LocalDateTime pickingCompletedAt;

    @Column(name = "packer_id")
    private Long packerId;

    @Column(name = "packer_name", length = 100)
    private String packerName;

    @Column(name = "packing_started_at")
    private LocalDateTime packingStartedAt;

    @Column(name = "packing_completed_at")
    private LocalDateTime packingCompletedAt;

    @Column(name = "packing_bag_code", length = 50)
    private String packingBagCode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private Set<OrderItem> items = new HashSet<>();

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
