package com.wewe.temjaimusic.service;

import com.wewe.temjaimusic.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SongService {
    Page<Song> findSongs(String keyword, Pageable pageable);
}

