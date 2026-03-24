package com.mysticcard.backend.service;

import com.mysticcard.backend.dto.coin.SlipVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlipVerificationService {

    @Value("${rdcw.api.clientId:PLACEHOLDER}")
    private String clientId;

    @Value("${rdcw.api.clientSecret:PLACEHOLDER}")
    private String clientSecret;

    @Value("${rdcw.api.expectedReceiverName:บจก. มิสติคคาร์ด}")
    private String expectedReceiverName;

    private final RestTemplate restTemplate = new RestTemplate();

    public SlipVerificationResult verifySlip(MultipartFile file, Integer expectedAmount) {
        if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty() || "PLACEHOLDER".equals(clientId)) {
            log.warn("RDCW API credentials not configured. Accepting slip automatically for development.");
            return SlipVerificationResult.builder()
                    .valid(true)
                    .amount(expectedAmount.doubleValue())
                    .transRef("MOCK_REF_" + System.currentTimeMillis())
                    .receiverName(expectedReceiverName)
                    .build();
        }

        try {
            String url = "https://suba.rdcw.co.th/v2/inquiry";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            String authHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            headers.set("Authorization", authHeader);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            log.info("RDCW API Full Response: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> bodyData = response.getBody();
                
                // 1. Check valid flag at root (if it exists)
                if (bodyData.containsKey("valid")) {
                    Object v = bodyData.get("valid");
                    if (v instanceof Boolean && !(Boolean)v) {
                       return SlipVerificationResult.builder().valid(false).errorMessage("Slip is marked as not valid").build();
                    }
                }

                if (bodyData.containsKey("data") && bodyData.get("data") instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) bodyData.get("data");
                    
                    // Also check valid flag inside data (promptparse standard)
                    if (data.containsKey("valid")) {
                        Object v = data.get("valid");
                        if (v instanceof Boolean && !(Boolean)v) {
                           return SlipVerificationResult.builder().valid(false).errorMessage("Slip is marked as not valid in data").build();
                        }
                    }

                    // 2. Extract Amount
                    double actualAmount = 0.0;
                    if (data.containsKey("amount")) {
                        Object amountObj = data.get("amount");
                        if (amountObj instanceof Number) {
                            actualAmount = ((Number) amountObj).doubleValue();
                        } else if (amountObj instanceof String) {
                            actualAmount = Double.parseDouble((String) amountObj);
                        }
                    }

                    // 3. Extract Receiver Name
                    String receiverName = "";
                    if (data.containsKey("receiver")) {
                        Map<String, Object> receiver = (Map<String, Object>) data.get("receiver");
                        if (receiver.containsKey("displayName")) {
                            receiverName = (String) receiver.get("displayName");
                        } else if (receiver.containsKey("account") && receiver.get("account") instanceof Map) {
                            Map<String, Object> account = (Map<String, Object>) receiver.get("account");
                            if (account.containsKey("name")) {
                                Object nameObj = account.get("name");
                                if (nameObj instanceof Map && ((Map)nameObj).containsKey("th")) {
                                    receiverName = (String) ((Map)nameObj).get("th");
                                } else if (nameObj instanceof String) {
                                    receiverName = (String) nameObj;
                                }
                            }
                        }
                    }

                    // 4. Extract transRef
                    String transRef = "";
                    if (data.containsKey("transRef")) {
                        transRef = (String) data.get("transRef");
                    }

                    // Validations
                    if (actualAmount < expectedAmount) {
                        return SlipVerificationResult.builder().valid(false)
                                .errorMessage("Insufficient amount. Expected: " + expectedAmount + ", but got: " + actualAmount).build();
                    }

                    if (transRef == null || transRef.isEmpty()) {
                        return SlipVerificationResult.builder().valid(false).errorMessage("Missing transaction reference").build();
                    }
                    
                    // You can optionally add receiver name validation string comparison here
                    // if (!receiverName.contains(expectedReceiverName)) { 
                    //     log.warn("Receiver name mismatch. Expected: {}, Actual: {}", expectedReceiverName, receiverName);
                    // }

                    return SlipVerificationResult.builder()
                        .valid(true)
                        .amount(actualAmount)
                        .receiverName(receiverName)
                        .transRef(transRef)
                        .build();
                }
            }
            return SlipVerificationResult.builder().valid(false).errorMessage("Failed to parse RDCW Response").build();
        } catch (Exception e) {
            log.error("Error verifying slip with RDCW API: {}", e.getMessage());
            return SlipVerificationResult.builder().valid(false).errorMessage("API Error: " + e.getMessage()).build();
        }
    }
}
