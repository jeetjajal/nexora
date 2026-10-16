package com.nexora.store.dto;

import com.nexora.store.entity.StoreStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusRequest {

    @NotNull(message = "Status is required")
    private StoreStatus status;
}
