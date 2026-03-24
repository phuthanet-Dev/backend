package com.mysticcard.backend.dto.coin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlipVerificationResult {
    private boolean valid;
    private Double amount;
    private String receiverName;
    private String transRef;
    private String errorMessage;
}
