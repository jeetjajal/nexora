package com.nexora.concurrency.dto;

import com.nexora.concurrency.model.ConcurrencyStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcurrencyStrategyInfoResponse {

    private ConcurrencyStrategy strategy;
    private String name;
    private String mechanism;
    private String isolationLevel;
    private String bestUsedFor;
    private String tradeOffs;
}
