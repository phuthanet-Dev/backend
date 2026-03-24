package com.mysticcard.backend.dto.wallpaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WallpaperResponse {
    private String description;   // คำอธิบายจาก AI เกี่ยวกับวอลเปเปอร์
    private String imagePrompt;   // prompt ที่ใช้ gen รูปภาพ
    private String imageUrl;      // URL ของรูปภาพที่ gen ได้
    private Integer remainingCoins; // เหรียญคงเหลือหลังหักแล้ว
}
