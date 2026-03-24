package com.mysticcard.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mysticcard.backend.dto.reading.ReadingRequest;
import com.mysticcard.backend.service.ReadingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    @PostMapping("/generateReading")
    public ResponseEntity<String> generateReading(@RequestBody ReadingRequest request) {
        try {
            String reading = readingService.generateReading(
                request.getCardNames(),
                request.getFocusArea(),
                request.getBirthDate(),
                request.getBirthTime(),
                request.getZodiac(),
                request.getElement()
            );
            return ResponseEntity.ok(reading);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating reading: " + e.getMessage());
        }
    }
}
