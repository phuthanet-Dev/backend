package com.mysticcard.backend.dto.coin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CoinDeductRequest {
    private String readingType;
    private Integer amount;
}
