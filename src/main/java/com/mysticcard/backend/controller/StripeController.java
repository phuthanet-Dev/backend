package com.mysticcard.backend.controller;

import com.mysticcard.backend.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class StripeController {

    private final StripeService stripeService;

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Missing or invalid Authorization header");
    }

    @PostMapping("/create-session")
    public ResponseEntity<?> createCheckoutSession(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Integer> request) {
        try {
            String token = extractToken(authHeader);
            Integer coins = request.get("coins");
            if (coins == null) {
                return ResponseEntity.badRequest().body("Missing 'coins' field");
            }
            String sessionUrl = stripeService.createCheckoutSession(token, coins);
            return ResponseEntity.ok(Map.of("sessionUrl", sessionUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (RuntimeException e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}
