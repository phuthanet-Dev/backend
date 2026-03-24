package com.mysticcard.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ReadingService {

  @Value("${gemini.api.key}")
  private String apiKey;

  @Value("${gemini.api.url}")
  private String apiUrl;

  private final RestTemplate restTemplate = new RestTemplate();

  @Retryable(
      value = { Exception.class },
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  public String generateReading(List<String> cardNames, String focusArea,
      String birthDate, String birthTime, String zodiac, String element) {
    // 1. ต่อ URL เข้ากับ API Key
    String requestUrl = apiUrl + "?key=" + apiKey;

    // 2. ตั้งค่า Header ให้เป็น JSON
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // 3. กำหนด System Instruction
    String systemInstruction = "คุณคือผู้เชี่ยวชาญดูดวงด้วยไพ่ (Tarot, Lenormand, Oracle, ไพ่ป๊อก) ผสมโหราศาสตร์\n"
        + "\n"
        + "[กฎเหล็ก]\n"
        + "1. พูดเป็นกันเอง เหมือนเพื่อนสนิทที่ดูดวงเก่ง ใช้ภาษาไทยง่ายๆ อ่านสบาย ห้ามใช้ราชาศัพท์หรือภาษาทางการ\n"
        + "2. ตอบสั้นกระชับ ไม่เกิน 3 ย่อหน้า ไม่ต้องยืดยาว\n"
        + "3. ทำนายเฉพาะเจาะจง ชี้ชัดถึงเหตุการณ์ อารมณ์ ช่วงเวลา ห้ามพูดกว้างๆ แบบใครอ่านก็ตรง\n"
        + "4. ห้ามอธิบายกระบวนการ ห้ามบอกว่าเปิดไพ่ใบไหน ดาวอะไรทับอะไร ให้บอกแค่ผลลัพธ์ที่กลั่นกรองแล้ว\n"
        + "5. หากมีเรื่องร้าย ให้เตือนอย่างตรงๆ แต่แนะทางออกเสมอ ไม่ตัดสิน\n"
        + "6. ห้ามใช้คำว่า 'ท่าน' ให้ใช้ 'เธอ' แทน\n"
        + "7. ใช้การอ่านแบบร้อยเรียงเรื่องราว (Storytelling Reading): ห้ามแยกอธิบายไพ่ทีละใบ ให้นำความหมายของไพ่ทุกใบมาผสานกันเป็นเรื่องราวเดียวที่ต่อเนื่อง เล่าเป็นภาพรวมเพื่อตอบโจทย์สถานการณ์ของผู้ใช้โดยตรง\n"
        + "\n"
        + "[โครงสร้างคำตอบ]\n"
        + "- เปิดด้วยทักทายสั้นๆ 1 ประโยค แล้วเข้าเรื่องเลย\n"
        + "- คำทำนาย: เล่าเป็นเรื่องราวต่อเนื่อง บอกตรงๆ ว่าตอนนี้เป็นยังไง และจะเป็นยังไงต่อไป\n"
        + "- คำแนะนำ: บอกสิ่งที่ควรทำหรือระวัง 2-3 ข้อแบบนำไปใช้ได้จริง";

    // 4. กำหนดคำถามที่ต้องการถาม (Prompt)
    String cardList = String.join(", ", cardNames);
    String userPrompt = "ข้อมูลของฉัน:\n"
        + "- ไพ่ที่เปิดได้: " + cardList + "\n"
        + "- หัวข้อที่ต้องการดู: " + focusArea + "\n"
        + "- วันเดือนปีเกิด: " + (birthDate != null ? birthDate : "ไม่ระบุ") + "\n"
        + "- เวลาเกิด: " + (birthTime != null ? birthTime : "ไม่ระบุ") + "\n"
        + "- ราศี: " + (zodiac != null ? zodiac : "ไม่ระบุ") + "\n"
        + "- ธาตุ: " + (element != null ? element : "ไม่ระบุ") + "\n"
        + "คำทำนายของฉันเป็นอย่างไร?";

    // 5. สร้างโครงสร้าง JSON ตามมาตรฐานของ Gemini API
    String sysJson = "\"" + escapeJson(systemInstruction) + "\"";
    String promptJson = "\"" + escapeJson(userPrompt) + "\"";
    String requestBody = """
        {
          "system_instruction": {
            "parts": { "text": %s }
          },
          "contents": [{
            "parts": [{ "text": %s }]
          }]
        }
        """.formatted(sysJson, promptJson);

    // 6. ส่ง Request ไปหา Google
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);
    return response.getBody(); // ส่งผลลัพธ์กลับไป (จะเป็นก้อน JSON ยาวๆ)
  }

  @Recover
  public String recover(Exception e, List<String> cardNames, String focusArea,
      String birthDate, String birthTime, String zodiac, String element) {
    return "ระบบแม่หมอกำลังพักผ่อนและพยายามเชื่อมต่อใหม่หลายครั้งแล้ว แต่ยังไม่สำเร็จ กรุณาลองใหม่อีกครั้ง: " + e.getMessage();
  }

  /** Escape สตริงให้ใช้ใน JSON ได้อย่างสุรกษิต */
  private String escapeJson(String text) {
    return text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}