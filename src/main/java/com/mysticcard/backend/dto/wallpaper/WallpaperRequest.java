package com.mysticcard.backend.dto.wallpaper;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WallpaperRequest {
    private String focusArea; // หัวข้อที่ต้องการเสริม เช่น การเงิน ความรัก สุขภาพ
}
