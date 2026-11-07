package com.nexora.wms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExceptionRequest implements Serializable {
    @NotBlank
    private String reason; // SHORTAGE, DAMAGED_ON_SHELF, EXPIRED_ON_SHELF, BIN_EMPTY

    private Integer actualAvailableQuantity;

    private String notes;
}
