package com.mysticcard.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.mysticcard.backend.dto.vip.VipSubscribeRequest;
import com.mysticcard.backend.dto.vip.VipSubscribeResponse;
import com.mysticcard.backend.entity.CoinTransaction;
import com.mysticcard.backend.entity.User;
import com.mysticcard.backend.repository.CoinTransactionRepository;
import com.mysticcard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VipService {

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    private User verifyTokenAndGetUser(String firebaseTokenString) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseTokenString);
            String uid = decodedToken.getUid();
            return userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (FirebaseAuthException e) {
            log.error("Firebase Auth Error: ", e);
            throw new RuntimeException("Invalid Firebase Token", e);
        }
    }

    @Transactional
    public VipSubscribeResponse subscribeVip(String token, VipSubscribeRequest request) {
        User user = verifyTokenAndGetUser(token);
        
        int cost = 0;
        int daysToAdd = 0;
        String planName = "";

        if ("WEEKLY".equalsIgnoreCase(request.getPlan())) {
            cost = 250;
            daysToAdd = 7;
            planName = "VIP_WEEKLY";
        } else if ("MONTHLY".equalsIgnoreCase(request.getPlan())) {
            cost = 600;
            daysToAdd = 30;
            planName = "VIP_MONTHLY";
        } else {
            throw new IllegalArgumentException("Invalid plan selected: " + request.getPlan());
        }

        if (user.getCoinBalance() < cost) {
            throw new IllegalArgumentException("Insufficient coins for VIP subscription");
        }

        // Deduct coins
        user.setCoinBalance(user.getCoinBalance() - cost);

        // Calculate new VIP date (if they are already VIP, extend. Otherwise, start from now)
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime currentVipUntil = user.getVipUntil();
        if (currentVipUntil != null && currentVipUntil.isAfter(now)) {
            user.setVipUntil(currentVipUntil.plusDays(daysToAdd));
        } else {
            user.setVipUntil(now.plusDays(daysToAdd));
        }

        userRepository.save(user);

        // Record transaction
        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .amount(-cost)
                .transactionType(planName)
                .description("Subscribed to " + planName)
                .build();
        coinTransactionRepository.save(transaction);

        log.info("User {} subscribed to {}. Expires at {}", user.getId(), planName, user.getVipUntil());

        return VipSubscribeResponse.builder()
                .message("Successfully subscribed to VIP " + request.getPlan())
                .newBalance(user.getCoinBalance())
                .vipUntil(user.getVipUntil())
                .build();
    }
}
