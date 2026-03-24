package com.mysticcard.backend.dto.vip;

import lombok.Data;

@Data
public class VipSubscribeRequest {
    private String plan; // "WEEKLY" or "MONTHLY"
}
