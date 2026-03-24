package com.mysticcard.backend.controller;

import com.mysticcard.backend.dto.auth.AuthResponse;
import com.mysticcard.backend.dto.auth.LoginRequest;
import com.mysticcard.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<AuthResponse> syncUser(@RequestBody LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.getFirebaseToken() == null
                || loginRequest.getFirebaseToken().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            AuthResponse response = userService.verifyTokenAndLoginOrRegister(loginRequest.getFirebaseToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(null);
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<?> saveProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody com.mysticcard.backend.dto.auth.ProfileRequest profileRequest) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        try {
            userService.saveProfile(token, profileRequest);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}
