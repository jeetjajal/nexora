package com.nexora.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyDeliveryOtpRequest {

    @NotBlank(message = "otp is required")
    @Pattern(regexp = "^\\d{4,6}$", message = "otp must be a 4 to 6 digit numeric code")
    private String otp;
}
