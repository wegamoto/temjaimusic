package com.wewe.temjaimusic.controller;

import com.wewe.temjaimusic.model.Song;
import com.wewe.temjaimusic.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/songs")
public class SongController {

    @Autowired
    private SongRepository songRepository;

    // 📄 แสดงรายการเพลงทั้งหมด พร้อมระบบค้นหา keyword
    @GetMapping
    public String listSongs(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
        List<Song> songs;

        if (keyword != null && !keyword.trim().isEmpty()) {
            songs = songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(keyword, keyword);
        } else {
            songs = songRepository.findAll();
        }

        model.addAttribute("songs", songs);
        model.addAttribute("keyword", keyword);
        return "songs"; // -> resources/templates/songs.html
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

    // รับข้อมูลจากฟอร์ม เพิ่มเพลงใหม่ (รองรับ tags ผ่าน rawTags)
    @PostMapping
    public String addNewSong(@ModelAttribute Song song,
                             @RequestParam(name = "rawTags", required = false) String rawTags,
                             @RequestParam("mp3File") MultipartFile mp3File) {
        try {
            // แปลง rawTags เป็น List<String>
            if (rawTags != null && !rawTags.trim().isEmpty()) {
                List<String> tags = Arrays.stream(rawTags.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                song.setTags(tags);
            }

            // อัพโหลดไฟล์ MP3
            if (mp3File != null && !mp3File.isEmpty()) {
                String uploadDir = "c:/temjaimusic/uploads";
                Path uploadPath = Paths.get(uploadDir);

                // สร้างโฟลเดอร์ถ้ายังไม่มี
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // ตั้งชื่อไฟล์ด้วย timestamp ป้องกันชื่อซ้ำ
                String filename = System.currentTimeMillis() + "_" + mp3File.getOriginalFilename();

                Path filePath = uploadPath.resolve(filename);

                // บันทึกไฟล์ไปยัง path ที่กำหนด
                mp3File.transferTo(filePath.toFile());

                // บันทึกชื่อไฟล์ใน entity เพื่อเก็บในฐานข้อมูล
                song.setMp3Filename(filename);
            }

            // บันทึกข้อมูล song ลง database
            songRepository.save(song);

        } catch (Exception e) {
            e.printStackTrace();
            // กรณี error แนะนำเพิ่มข้อความแจ้ง user (ในกรณีใช้ Thymeleaf หรืออื่น ๆ)
            // เช่น redirect พร้อม error message หรือเก็บใน model attribute
        }

        return "redirect:/songs";  // redirect ไปหน้าแสดงเพลงหลังบันทึกเสร็จ
    }



}
