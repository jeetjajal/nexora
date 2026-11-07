package com.nexora.wms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletePackingWmsRequest implements Serializable {
    @NotBlank
    private String bagCode;

    @Builder.Default
    private Boolean qcPassed = true;
}
