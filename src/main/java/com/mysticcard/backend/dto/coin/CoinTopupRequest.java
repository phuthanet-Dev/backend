package com.mysticcard.backend.dto.coin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CoinTopupRequest {
    private Integer amount;
}
