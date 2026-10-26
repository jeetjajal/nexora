package com.nexora.store.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CompletePackingRequest {
    @NotBlank(message = "Packing bag tamper-evident barcode is required")
    private String bagCode;
}
