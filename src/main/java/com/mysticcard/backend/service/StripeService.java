package com.mysticcard.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    private final CoinService coinService;

    private static final java.util.Map<Integer, Integer> COIN_PRICE_MAP = java.util.Map.of(
            100, 1900,
            500, 8900,
            1200, 19900
    );

    public String createCheckoutSession(String firebaseToken, int coins) {
        Integer priceStang = COIN_PRICE_MAP.get(coins);
        if (priceStang == null) {
            throw new IllegalArgumentException("Invalid coin package: " + coins);
        }

        String uid = verifyTokenAndGetUid(firebaseToken);

        Stripe.apiKey = stripeSecretKey;

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PROMPTPAY)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata("firebaseUid", uid)
                    .putMetadata("coins", String.valueOf(coins))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("thb")
                                                    .setUnitAmount((long) priceStang)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(coins + " Mystic Coins")
                                                                    .setDescription("เหรียญสำหรับใช้บน Mystic Card")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            log.info("Stripe Checkout Session created: {} for uid: {}", session.getId(), uid);
            return session.getUrl();

        } catch (Exception e) {
            log.error("Failed to create Stripe Checkout Session: ", e);
            throw new RuntimeException("Failed to create payment session: " + e.getMessage(), e);
        }
    }

    public void handleWebhook(String payload, String sigHeader) {
        com.stripe.model.Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature", e);
        }

        if ("checkout.session.completed".equals(event.getType())) {
            com.stripe.model.StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new RuntimeException("Failed to deserialize Stripe event"));

            Session session = (Session) stripeObject;
            String firebaseUid = session.getMetadata().get("firebaseUid");
            String coinsStr = session.getMetadata().get("coins");

            if (firebaseUid == null || coinsStr == null) {
                log.error("Missing metadata in Stripe session: {}", session.getId());
                return;
            }

            int coins = Integer.parseInt(coinsStr);
            log.info("Payment completed for uid: {}, coins: {}, session: {}", firebaseUid, coins, session.getId());

            coinService.creditCoinsByUid(firebaseUid, coins, session.getId());
            log.info("Stripe webhook: credited {} coins to uid: {}", coins, firebaseUid);
        }
    }

    private String verifyTokenAndGetUid(String firebaseTokenString) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseTokenString);
            return decodedToken.getUid();
        } catch (FirebaseAuthException e) {
            log.error("Firebase Auth Error: ", e);
            throw new RuntimeException("Invalid Firebase Token", e);
        }
    }
}
