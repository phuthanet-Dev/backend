package com.mysticcard.backend.controller;

import com.mysticcard.backend.dto.coin.CoinBalanceResponse;
import com.mysticcard.backend.dto.coin.CoinDeductRequest;
import com.mysticcard.backend.service.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Missing or invalid Authorization header");
    }

    @GetMapping("/balance")
    public ResponseEntity<CoinBalanceResponse> getBalance(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            CoinBalanceResponse response = coinService.getBalance(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/deduct")
    public ResponseEntity<?> deductCoins(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CoinDeductRequest request) {
        try {
            String token = extractToken(authHeader);
            CoinBalanceResponse response = coinService.deductCoins(token, request.getAmount(),
                    request.getReadingType());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if ("Insufficient coins".equals(e.getMessage())) {
                return ResponseEntity.status(402).body("Insufficient coins"); // 402 Payment Required
            }
            return ResponseEntity.status(401).build();
        }
    }
}
