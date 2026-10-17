package com.nexora.search.dto;

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
public class SearchSyncResponse {
    private String status;
    private String message;
    private long productsSynced;
    private long storesSynced;
    private long durationMs;
}
