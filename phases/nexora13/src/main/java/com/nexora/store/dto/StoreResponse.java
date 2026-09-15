package com.nexora.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String status;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Set<String> categories;
    private Long ownerId;
}
