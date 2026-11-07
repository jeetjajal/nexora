package com.nexora.wms.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchHandoverResponse implements Serializable {
    private Long handoverId;
    private Long orderId;
    private Long storeId;
    private Long deliveryPartnerId;
    private String deliveryPartnerName;
    private String bagCode;
    private String orderStatus;
    private LocalDateTime handoverAt;
    private String message;
}
