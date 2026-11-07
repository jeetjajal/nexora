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
public class DispatchHandoverRequest implements Serializable {
    @NotNull
    private Long deliveryPartnerId;

    @NotBlank
    private String bagCode;
}
