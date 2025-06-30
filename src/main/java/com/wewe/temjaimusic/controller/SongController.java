package com.wewe.temjaimusic.controller;

import com.wewe.temjaimusic.model.Song;
import com.wewe.temjaimusic.repository.SongRepository;
import com.wewe.temjaimusic.service.SongService;
import com.wewe.temjaimusic.service.SongServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private final SongService songService;

    @Value("${supabase.api.key:DEFAULT_SUPABASE_API_KEY}")
    private String supabaseApiKey;

    @Value("${supabase.url:https://default.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.bucket:default-bucket}")
    private String bucketName;

    @Autowired
    public SongController(SongRepository songRepository, SongService songService) {
        this.songRepository = songRepository;
        this.songService = songService;
    }

    // 📄 แสดงรายการเพลงทั้งหมด พร้อมระบบค้นหา keyword ให้รองรับ page
    @GetMapping
    public String listSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {

        Page<Song> songs = songService.findSongs(keyword, PageRequest.of(page, size));
        model.addAttribute("songs", songs.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", songs.getTotalPages());
        model.addAttribute("keyword", keyword);
        return "songs";
    }

//    @GetMapping
//    public String listSongs(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
//        List<Song> songs = (keyword != null && !keyword.trim().isEmpty())
//                ? songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(keyword, keyword)
//                : songRepository.findAll();
//
//        model.addAttribute("songs", songs);
//        model.addAttribute("keyword", keyword);
//        return "songs";
//    }

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
        // ใช้ UUID และ timestamp เพื่อให้ไม่ซ้ำกัน
        String safeFileName = UUID.randomUUID() + ".mp3";  // ไม่ใช้ชื่อเดิม
        String filePath = "uploads/" + safeFileName;

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filePath;

        System.out.println("Supabase API Key: " + supabaseApiKey); // ต้องไม่เป็น null

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("audio/mpeg"));
        headers.set("Authorization", "Bearer " + supabaseApiKey);  // ✅ ต้องเป็น JWT จริงๆ
        headers.set("x-upsert", "true");
        headers.set("cache-control", "public, max-age=31536000");

        HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Supabase upload failed: " + response.getBody());
        }

        // Return public URL (if bucket is public)
        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + filePath;
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


}
