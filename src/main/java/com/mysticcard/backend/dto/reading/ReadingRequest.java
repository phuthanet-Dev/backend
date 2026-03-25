package com.mysticcard.backend.dto.reading;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class ReadingRequest {
    @NotEmpty(message = "At least one card must be selected")
    private List<String> cardNames;
    
    @NotBlank(message = "Focus area is required")
    private String focusArea;
    
    private String birthDate;
    private String birthTime;
    private String zodiac;
    private String element;
}
