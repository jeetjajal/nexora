package com.nexora.wms.entity;

import com.nexora.delivery.entity.DeliveryPartner;
import com.nexora.order.entity.Order;
import com.nexora.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "dispatch_handovers",
        indexes = {
                @Index(name = "idx_dh_order", columnList = "order_id"),
                @Index(name = "idx_dh_partner", columnList = "delivery_partner_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_partner_id", nullable = false)
    private DeliveryPartner deliveryPartner;

    @Column(name = "bag_code", nullable = false, length = 64)
    private String bagCode;

    @Column(name = "handover_by_user_id", nullable = false)
    private Long handoverByUserId;

    @Column(name = "handover_at", nullable = false, updatable = false)
    private LocalDateTime handoverAt;

    @PrePersist
    protected void onCreate() {
        this.handoverAt = LocalDateTime.now();
    }
}
