package com.mysticcard.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.mysticcard.backend.dto.wallpaper.WallpaperResponse;
import com.mysticcard.backend.entity.CoinTransaction;
import com.mysticcard.backend.entity.User;
import com.mysticcard.backend.entity.UserAstrologyProfile;
import com.mysticcard.backend.repository.CoinTransactionRepository;
import com.mysticcard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WallpaperService {

    private static final int WALLPAPER_COST = 10;

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    @Value("${nanobanana.api.url}")
    private String nanoBananaApiUrl;

    @Value("${nanobanana.api.key}")
    private String nanoBananaApiKey;

    @Value("${openrouter.api.url}")
    private String openRouterApiUrl;

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public WallpaperResponse generateWallpaper(String token, String focusArea) {
        // 1. ดึงข้อมูลของยูเซอร์
        User user = verifyTokenAndGetUser(token);

        // ตรวจสอบเหรียญ
        if (user.getCoinBalance() < WALLPAPER_COST) {
            throw new RuntimeException("Insufficient coins");
        }

        // ดึง profile ข้อมูลราศี ธาตุ
        UserAstrologyProfile profile = user.getAstrologyProfile();
        String zodiac = profile != null ? profile.getZodiacSign() : "ไม่ระบุ";
        String element = profile != null ? profile.getElement() : "ไม่ระบุ";
        String resolvedFocus = (focusArea != null && !focusArea.isBlank()) ? focusArea : "โชคลาภทั่วไป";

        // 2. เรียก OpenRouter API เพื่อให้ AI สร้าง prompt และคำอธิบาย
        Map<String, String> aiResult = callOpenRouterForPrompt(resolvedFocus, zodiac, element);
        String description = aiResult.get("dialogs");
        String imagePrompt = aiResult.get("prompt");

        // 3. เรียก nanobanana API เพื่อ gen รูปภาพ
        String imageUrl = callImageGenerationApi(imagePrompt);

        // 4. หักเหรียญ
        user.setCoinBalance(user.getCoinBalance() - WALLPAPER_COST);
        userRepository.save(user);

        // บันทึก transaction
        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .amount(-WALLPAPER_COST)
                .transactionType("WALLPAPER_GENERATE")
                .description("Generate wallpaper: " + resolvedFocus)
                .build();
        coinTransactionRepository.save(transaction);

        log.info("User {} generated wallpaper for focus '{}'. Remaining coins: {}", user.getId(), resolvedFocus, user.getCoinBalance());

        return WallpaperResponse.builder()
                .description(description)
                .imagePrompt(imagePrompt)
                .imageUrl(imageUrl)
                .remainingCoins(user.getCoinBalance())
                .build();
    }

    // ===== Image Generation via nanobanana API =====

    private String callImageGenerationApi(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("prompt", prompt);
            body.put("aspect_ratio", "9:16");
            body.put("mode", "text-to-image");
            body.put("user_api_key", nanoBananaApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Calling nanobanana API with prompt: {}", prompt);
            ResponseEntity<String> response = restTemplate.postForEntity(nanoBananaApiUrl, request, String.class);

            String responseBody = response.getBody();
            log.info("Nanobanana API response: {}", responseBody);

            // Parse response JSON to extract image URL
            JsonNode root = objectMapper.readTree(responseBody);

            // Try common response fields
            if (root.has("image_url")) {
                return root.get("image_url").asText();
            } else if (root.has("imageUrl")) {
                return root.get("imageUrl").asText();
            } else if (root.has("url")) {
                return root.get("url").asText();
            } else if (root.has("data") && root.get("data").has("url")) {
                return root.get("data").get("url").asText();
            } else if (root.has("output")) {
                return root.get("output").asText();
            }

            // Fallback: return raw response body (in case the response itself is the URL)
            log.warn("Could not find known image URL field in response. Raw: {}", responseBody);
            return responseBody;

        } catch (Exception e) {
            log.error("Failed to call nanobanana image generation API: ", e);
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    // ===== OpenRouter API: Generate prompt & dialog via AI =====

    private Map<String, String> callOpenRouterForPrompt(String intent, String zodiac, String element) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openRouterApiKey);

            String systemContent = """
                Role:
                You are "Auspicious Wallpaper Designer Gem," an expert in astrology, zodiac signs, elements, and runic alphabets. Your main task is to design auspicious minimalist symbolism wallpapers to enhance luck, outputting an Image Prompt along with friendly explanations.

                Workflow:
                1. Collect Info: Get 3 details from the user: (1) Desired luck/blessing, (2) Zodiac sign, (3) Element. Ask for any missing details before starting.
                2. Analyze & Select:
                   - Pick "5 runes" that align with the desired blessing.
                   - Pick a "Zodiac or Planetary symbol" as the focal point.
                   - Pick an "Element symbol" and set main/accent colors based on it.
                3. Layout Design (for the Image Prompt):
                   - Top: Zodiac symbol.
                   - Center: A stylish arrangement of the 5 runes.
                   - Bottom (Base): Element symbol.
                   - Background: Element-themed color tone with interesting details/textures (not flat).
                   - Decor: Vector-style frames, constellations, or dotted patterns.
                4. Generate Content: Write an explanation of the wallpaper and the 5 runes in the `dialogs` field. Act as a kind, friendly fortune teller. Use a casual, friend-to-friend tone. Strictly avoid formal language.

                Rules & Constraints:
                - Style: Use "Minimalist Line Art" and "Vector Illustration" only. No photorealism or complex 3D.
                - Accuracy: Symbols/runes must perfectly match the user's goal.
                - Negative Prompt: The prompt must include: `--no text, font, letters, words, typography, watermarks`
                - Size: Always append `--ar 9:16` at the end of the prompt.
                - Strict Output: Reply ONLY in valid JSON format. Absolutely no greetings, introductions, or text outside the `{ ... }`.

                Output Format:
                {
                  "lang": "th or en (matching the user's input language)",
                  "dialogs": "Wallpaper overview and meanings of the 5 runes in a friendly, conversational tone (using the language defined in 'lang').",
                  "prompt": "[English Image Prompt] --no text, font, letters, words, typography, watermarks --ar 9:16"
                }""";

            String userContent = "Request for wallpaper design: { \"intent\": \"" + intent
                    + "\", \"zodiac\": \"" + zodiac
                    + "\", \"element\": \"" + element
                    + "\", \"lang\": \"th\" }";

            Map<String, Object> body = new HashMap<>();
            body.put("model", "google/gemini-2.5-flash");
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemContent),
                    Map.of("role", "user", "content", userContent)
            ));
            body.put("response_format", Map.of("type", "json_object"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Calling OpenRouter API for wallpaper prompt generation...");
            ResponseEntity<String> response = restTemplate.postForEntity(openRouterApiUrl, request, String.class);

            String responseBody = response.getBody();
            log.info("OpenRouter API response: {}", responseBody);

            // Parse OpenRouter response: choices[0].message.content -> JSON with dialogs & prompt
            JsonNode root = objectMapper.readTree(responseBody);
            String contentStr = root.get("choices").get(0).get("message").get("content").textValue();

            JsonNode content = objectMapper.readTree(contentStr);
            String dialogs = content.has("dialogs") ? content.get("dialogs").textValue() : "";
            String prompt = content.has("prompt") ? content.get("prompt").textValue() : "";

            log.info("AI dialogs: {}", dialogs);
            log.info("AI prompt: {}", prompt);

            return Map.of("dialogs", dialogs, "prompt", prompt);

        } catch (Exception e) {
            log.error("Failed to call OpenRouter API: ", e);
            throw new RuntimeException("Wallpaper prompt generation failed: " + e.getMessage(), e);
        }
    }
}
