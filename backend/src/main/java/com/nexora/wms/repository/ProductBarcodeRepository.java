package com.nexora.wms.repository;

import com.nexora.wms.entity.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, Long> {
    Optional<ProductBarcode> findByBarcodeValue(String barcodeValue);
    List<ProductBarcode> findByProductId(Long productId);
}
