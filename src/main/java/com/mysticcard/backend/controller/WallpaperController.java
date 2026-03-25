package com.mysticcard.backend.controller;

import com.mysticcard.backend.dto.wallpaper.WallpaperRequest;
import com.mysticcard.backend.dto.wallpaper.WallpaperResponse;
import com.mysticcard.backend.service.WallpaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wallpaper")
@RequiredArgsConstructor
public class WallpaperController {

    private final WallpaperService wallpaperService;

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Missing or invalid Authorization header");
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateWallpaper(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody WallpaperRequest request) {
        try {
            String token = extractToken(authHeader);
            WallpaperResponse response = wallpaperService.generateWallpaper(token, request.getFocusArea());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            if ("Insufficient coins".equals(e.getMessage())) {
                return ResponseEntity.status(402).body("Insufficient coins");
            }
            return ResponseEntity.status(401).build();
        }
    }
}
