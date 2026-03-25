package com.mysticcard.backend.dto.vip;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class VipSubscribeRequest {
    @NotBlank(message = "Plan is required")
    @Pattern(regexp = "^(WEEKLY|MONTHLY)$", message = "Plan must be either WEEKLY or MONTHLY")
    private String plan; // "WEEKLY" or "MONTHLY"
}
