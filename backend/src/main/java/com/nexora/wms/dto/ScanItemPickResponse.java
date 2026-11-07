package com.nexora.wms.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanItemPickResponse implements Serializable {
    private Long pickTaskItemId;
    private Long pickTaskId;
    private String status;
    private Integer pickedQuantity;
    private Integer requiredQuantity;
    private Boolean isFullyPicked;
    private Boolean isVerified;
    private String message;
    private LocalDateTime pickedAt;
}
