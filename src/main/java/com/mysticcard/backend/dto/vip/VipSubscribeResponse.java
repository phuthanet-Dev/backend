package com.mysticcard.backend.dto.vip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VipSubscribeResponse {
    private String message;
    private Integer newBalance;
    private OffsetDateTime vipUntil;
}
