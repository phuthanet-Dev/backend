package com.mysticcard.backend.controller;

import com.mysticcard.backend.dto.vip.VipSubscribeRequest;
import com.mysticcard.backend.dto.vip.VipSubscribeResponse;
import com.mysticcard.backend.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody VipSubscribeRequest request) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            VipSubscribeResponse response = vipService.subscribeVip(token, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error subscribing to VIP: " + e.getMessage());
        }
    }
}
