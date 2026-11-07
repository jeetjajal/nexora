package com.nexora.wms.entity;

import com.nexora.product.entity.Product;
import com.nexora.wms.enums.BarcodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_barcodes",
        indexes = {
                @Index(name = "idx_pb_product", columnList = "product_id"),
                @Index(name = "idx_pb_value", columnList = "barcode_value")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBarcode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "barcode_value", nullable = false, unique = true, length = 64)
    private String barcodeValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", nullable = false, length = 20)
    @Builder.Default
    private BarcodeType barcodeType = BarcodeType.EAN_13;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
