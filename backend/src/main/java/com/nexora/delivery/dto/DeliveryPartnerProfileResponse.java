package com.nexora.delivery.dto;

import com.nexora.delivery.entity.DeliveryPartnerStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DeliveryPartnerProfileResponse {
    private Long id;
    private Long userId;
    private String name;
    private Boolean isOnline;
    private DeliveryPartnerStatus status;
    private BigDecimal totalEarnings;
}
