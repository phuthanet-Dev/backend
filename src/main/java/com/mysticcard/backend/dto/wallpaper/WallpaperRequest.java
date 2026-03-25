package com.mysticcard.backend.dto.wallpaper;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class WallpaperRequest {
    @NotBlank(message = "Focus area is required")
    private String focusArea; // หัวข้อที่ต้องการเสริม เช่น การเงิน ความรัก สุขภาพ
}
