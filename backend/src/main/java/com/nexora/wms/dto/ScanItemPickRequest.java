package com.nexora.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanItemPickRequest implements Serializable {
    @NotBlank
    private String scannedBarcode;

    private String scannedBatchNumber;

    @NotNull
    private Integer quantity;
}
