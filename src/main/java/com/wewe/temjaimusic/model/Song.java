package com.wewe.temjaimusic.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;         // ชื่อเพลง
    private String artist;        // ชื่อศิลปิน
    private String mp3Url;        // ลิงก์ไปยังไฟล์ MP3 (เก็บบน cloud เช่น S3)

    @Column(columnDefinition = "TEXT")
    private String lyrics;        // เนื้อเพลง (รองรับข้อความยาว)

    @Column(columnDefinition = "TEXT")
    private String chords;        // คอร์ดกีตาร์ (อาจเป็น ChordPro format ได้)

    private String genre;

    private boolean premiumOnly = false;   // true = สำหรับสมาชิกพรีเมียมเท่านั้น

    private LocalDateTime createdAt = LocalDateTime.now(); // วันเวลาที่เพิ่มเพลง

    @ElementCollection
    private List<String> tags;

    private String mp3Filename;
}

