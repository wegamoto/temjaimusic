package com.wewe.temjaimusic.service;

import com.wewe.temjaimusic.model.Song;
import com.wewe.temjaimusic.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SongServiceImpl implements SongService {

    @Autowired
    private SongRepository songRepository;

    @Override
    public Page<Song> findSongs(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return songRepository.findAll(pageable);
        } else {
            return songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(keyword, keyword, pageable);
        }
    }
}

