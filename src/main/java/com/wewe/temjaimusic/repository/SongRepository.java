package com.wewe.temjaimusic.repository;

import com.wewe.temjaimusic.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    // สามารถเพิ่ม custom query เช่น findByTitleContaining(String title) ได้ในอนาคต
    List<Song> findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(String title, String artist);

    List<Song> findByGenre(String genre);

    @Query("SELECT s FROM Song s JOIN s.tags t WHERE t LIKE %:tag%")
    List<Song> findByTag(@Param("tag") String tag);
}

