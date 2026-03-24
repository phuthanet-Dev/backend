package com.mysticcard.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.mysticcard.backend.dto.coin.CoinBalanceResponse;
import com.mysticcard.backend.entity.CoinTransaction;
import com.mysticcard.backend.entity.User;
import com.mysticcard.backend.repository.CoinTransactionRepository;
import com.mysticcard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinService {

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

    @Transactional(readOnly = true)
    public CoinBalanceResponse getBalance(String token) {
        User user = verifyTokenAndGetUser(token);
        return CoinBalanceResponse.builder()
                .balance(user.getCoinBalance())
                .build();
    }

    @Transactional
    public void creditCoinsByUid(String firebaseUid, int amount, String transRef) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found: " + firebaseUid));
        user.setCoinBalance(user.getCoinBalance() + amount);
        user = userRepository.save(user);
        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .amount(amount)
                .transactionType("TOPUP")
                .description("Stripe PromptPay top-up")
                .transRef(transRef)
                .build();
        coinTransactionRepository.save(transaction);
        log.info("Credited {} coins to uid: {}. New balance: {}", amount, firebaseUid, user.getCoinBalance());
    }

    @Transactional
    public CoinBalanceResponse executeTopUp(String token, Integer amount, String transRef) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Invalid top-up amount");
        }

        User user = verifyTokenAndGetUser(token);

        // Update balance
        user.setCoinBalance(user.getCoinBalance() + amount);
        user = userRepository.save(user);

        // Record transaction
        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .amount(amount)
                .transactionType("TOPUP")
                .description("Coin top-up")
                .transRef(transRef)
                .build();
        coinTransactionRepository.save(transaction);

        log.info("User {} topped up {} coins. New balance: {}", user.getId(), amount, user.getCoinBalance());

        return CoinBalanceResponse.builder()
                .balance(user.getCoinBalance())
                .build();
    }

    @Transactional
    public CoinBalanceResponse deductCoins(String token, Integer amount, String readingType) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Invalid deduction amount");
        }

        User user = verifyTokenAndGetUser(token);

        if (user.getCoinBalance() < amount) {
            throw new RuntimeException("Insufficient coins");
        }

        // Update balance
        user.setCoinBalance(user.getCoinBalance() - amount);
        user = userRepository.save(user);

        // Record transaction
        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .amount(-amount)
                .transactionType("READING_QUESTION")
                .description("Paid for reading: " + readingType)
                .build();
        coinTransactionRepository.save(transaction);

        log.info("User {} deducted {} coins for {}. New balance: {}", user.getId(), amount, readingType,
                user.getCoinBalance());

        return CoinBalanceResponse.builder()
                .balance(user.getCoinBalance())
                .build();
    }
}
