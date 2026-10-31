package com.nexora.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchGenerationRequest {
    private Long storeId;
    @Builder.Default
    private Integer maxBatchSize = 3;
    @Builder.Default
    private Double maxProximityKm = 2.5;
}
