package com.wewe.temjaimusic.controller;

import com.wewe.temjaimusic.model.Song;
import com.wewe.temjaimusic.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/songs")
public class SongController {

    private final SongRepository songRepository;

    @Value("${supabase.url:https://fdcpainrjthultvetriq.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.apikey:defaultApiKeyIfMissing}")
    private String supabaseApiKey;

    @Value("${supabase.bucket:temjaimusic}")
    private String bucketName;

    @Autowired
    public SongController(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    // 📄 แสดงรายการเพลงทั้งหมด พร้อมระบบค้นหา keyword
    @GetMapping
    public String listSongs(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
        List<Song> songs = (keyword != null && !keyword.trim().isEmpty())
                ? songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(keyword, keyword)
                : songRepository.findAll();

        model.addAttribute("songs", songs);
        model.addAttribute("keyword", keyword);
        return "songs";
    }

    // 🎧 แสดงรายละเอียดเพลงตาม ID
    @GetMapping("/{id}")
    public String viewSong(@PathVariable Long id, Model model) {
        Optional<Song> optionalSong = songRepository.findById(id);
        if (optionalSong.isPresent()) {
            model.addAttribute("song", optionalSong.get());
            return "song-detail"; // -> resources/templates/song-detail.html
        } else {
            return "redirect:/songs"; // ถ้าไม่เจอ กลับไปหน้า songs
        }
    }

    // แสดงฟอร์มแก้ไขเพลง
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Song> optionalSong = songRepository.findById(id);
        if (optionalSong.isPresent()) {
            model.addAttribute("song", optionalSong.get());
            return "edit-song"; // หน้าแก้ไขเพลง
        } else {
            return "redirect:/songs";
        }
    }

    // ประมวลผลฟอร์มแก้ไขเพลง
    @PostMapping("/{id}/edit")
    public String updateSong(@PathVariable Long id,
                             @ModelAttribute("song") Song updatedSong) {
        Optional<Song> optionalSong = songRepository.findById(id);
        if (optionalSong.isPresent()) {
            Song existingSong = optionalSong.get();
            existingSong.setTitle(updatedSong.getTitle());
            existingSong.setArtist(updatedSong.getArtist());
            existingSong.setMp3Url(updatedSong.getMp3Url());
            existingSong.setLyrics(updatedSong.getLyrics());
            existingSong.setChords(updatedSong.getChords());
            existingSong.setPremiumOnly(updatedSong.isPremiumOnly());
            existingSong.setTags(updatedSong.getTags());

            songRepository.save(existingSong);
        }
        return "redirect:/songs/" + id;
    }

    // แสดงฟอร์มเพิ่มเพลงใหม่
    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("song", new Song());
        return "add-song";
    }

    @PostMapping
    public String addNewSong(@ModelAttribute Song song,
                             @RequestParam(name = "rawTags", required = false) String rawTags,
                             @RequestParam("mp3File") MultipartFile mp3File,
                             Model model) {
        try {
            processTags(song, rawTags);

            if (mp3File != null && !mp3File.isEmpty()) {
                String mp3Url = uploadToSupabase(mp3File);
                song.setMp3Url(mp3Url);
            }

            songRepository.save(song);
            return "redirect:/songs";
        } catch (Exception e) {
            model.addAttribute("error", "เกิดข้อผิดพลาด: " + e.getMessage());
            e.printStackTrace();
            return "add-song";
        }
    }

    private String uploadToSupabase(MultipartFile file) throws IOException {
        String fileName = "uploads/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("audio/mpeg"));
        headers.set("Authorization", "Bearer " + supabaseApiKey);
        headers.set("x-upsert", "true");
        headers.set("cache-control", "public, max-age=31536000");

        HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Supabase upload failed: " + response.getBody());
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
    }

    private void processTags(Song song, String rawTags) {
        if (rawTags != null && !rawTags.trim().isEmpty()) {
            List<String> tags = Arrays.stream(rawTags.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            song.setTags(tags);
        }
    }

//    @PostMapping
//    public String addNewSong(@ModelAttribute Song song,
//                             @RequestParam(name = "rawTags", required = false) String rawTags,
//                             @RequestParam("mp3File") MultipartFile mp3File,
//                             Model model) {
//        try {
//            // ✅ จัดการ tags
//            if (rawTags != null && !rawTags.trim().isEmpty()) {
//                List<String> tags = Arrays.stream(rawTags.split(","))
//                        .map(String::trim)
//                        .filter(s -> !s.isEmpty())
//                        .toList();
//                song.setTags(tags);
//            }
//
//            // ✅ อัปโหลดไฟล์ MP3 ไปยัง Supabase
//            if (mp3File != null && !mp3File.isEmpty()) {
//                String fileName = "uploads/" + UUID.randomUUID() + "-" + mp3File.getOriginalFilename();
//
//                // ✅ ต้องไม่มี "public" ตรง URL PUT
//                String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;
//
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.valueOf("audio/mpeg")); // ✅ ใช้ MIME type mp3
//                headers.set("x-upsert", "true");
//                headers.set("cache-control", "public, max-age=31536000");  // เพื่อให้ browser cache และรู้ว่าเล่นได้
//
//                headers.set("Authorization", "Bearer " + supabaseApiKey);
//                headers.set("x-upsert", "true");
//
//                HttpEntity<byte[]> requestEntity = new HttpEntity<>(mp3File.getBytes(), headers);
//                RestTemplate restTemplate = new RestTemplate();
//
//                ResponseEntity<String> response = restTemplate.exchange(
//                        uploadUrl,
//                        HttpMethod.PUT,
//                        requestEntity,
//                        String.class
//                );
//
//                if (response.getStatusCode().is2xxSuccessful()) {
//                    // ✅ ใช้ public URL สำหรับแสดงผล
//                    String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
//                    song.setMp3Url(publicUrl);
//                } else {
//                    throw new RuntimeException("Supabase upload failed: " + response.getBody());
//                }
//            }
//
//            songRepository.save(song);
//        } catch (Exception e) {
//            model.addAttribute("error", "เกิดข้อผิดพลาด: " + e.getMessage());
//            e.printStackTrace();
//            return "add-song";
//        }
//
//        return "redirect:/songs";
//    }

    // รับข้อมูลจากฟอร์ม เพิ่มเพลงใหม่ (รองรับ tags ผ่าน rawTags)
//    @PostMapping
//    public String addNewSong(@ModelAttribute Song song,
//                             @RequestParam(name = "rawTags", required = false) String rawTags,
//                             @RequestParam("mp3File") MultipartFile mp3File) {
//        try {
//            // แปลง rawTags เป็น List<String>
//            if (rawTags != null && !rawTags.trim().isEmpty()) {
//                List<String> tags = Arrays.stream(rawTags.split(","))
//                        .map(String::trim)
//                        .filter(s -> !s.isEmpty())
//                        .collect(Collectors.toList());
//                song.setTags(tags);
//            }
//
//            // อัพโหลดไฟล์ MP3
//            if (mp3File != null && !mp3File.isEmpty()) {
//                String uploadDir = "c:/temjaimusic/uploads";
//                Path uploadPath = Paths.get(uploadDir);
//
//                // สร้างโฟลเดอร์ถ้ายังไม่มี
//                if (!Files.exists(uploadPath)) {
//                    Files.createDirectories(uploadPath);
//                }
//
//                // ตั้งชื่อไฟล์ด้วย timestamp ป้องกันชื่อซ้ำ
//                String filename = System.currentTimeMillis() + "_" + mp3File.getOriginalFilename();
//
//                Path filePath = uploadPath.resolve(filename);
//
//                // บันทึกไฟล์ไปยัง path ที่กำหนด
//                mp3File.transferTo(filePath.toFile());
//
//                // บันทึกชื่อไฟล์ใน entity เพื่อเก็บในฐานข้อมูล
//                song.setMp3Filename(filename);
//            }
//
//            // บันทึกข้อมูล song ลง database
//            songRepository.save(song);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            // กรณี error แนะนำเพิ่มข้อความแจ้ง user (ในกรณีใช้ Thymeleaf หรืออื่น ๆ)
//            // เช่น redirect พร้อม error message หรือเก็บใน model attribute
//        }
//
//        return "redirect:/songs";  // redirect ไปหน้าแสดงเพลงหลังบันทึกเสร็จ
//    }



}
