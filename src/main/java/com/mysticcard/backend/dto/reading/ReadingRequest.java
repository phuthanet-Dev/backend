package com.mysticcard.backend.dto.reading;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ReadingRequest {
    private List<String> cardNames;
    private String focusArea;
    private String birthDate;
    private String birthTime;
    private String zodiac;
    private String element;
}
