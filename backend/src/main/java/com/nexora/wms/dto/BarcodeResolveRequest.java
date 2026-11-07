package com.nexora.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BarcodeResolveRequest implements Serializable {

    @NotNull
    private Long storeId;

    @NotBlank
    private String barcode;

    public BarcodeResolveRequest() {}

    public BarcodeResolveRequest(Long storeId, String barcode) {
        this.storeId = storeId;
        this.barcode = barcode;
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
}
