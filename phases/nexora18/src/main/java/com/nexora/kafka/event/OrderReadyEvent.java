package com.nexora.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadyEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long storeId;
    private LocalDateTime readyAt;
}
