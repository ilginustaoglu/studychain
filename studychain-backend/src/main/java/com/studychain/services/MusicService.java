package com.studychain.services;

import com.studychain.models.Music;
import com.studychain.repositories.MusicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MusicService {

    private final MusicRepository musicRepository;
    private final Path uploadDir;

    public MusicService(
        MusicRepository musicRepository,
        @Value("${music.upload.dir:uploads/music}") String uploadDirPath
    ) throws IOException {
        this.musicRepository = musicRepository;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public List<Music> listAll() {
        return musicRepository.findAll();
    }

    public Music getById(Long id) {
        return musicRepository.findById(id).orElse(null);
    }

    public Resource getResource(Music music) {
        return new FileSystemResource(music.getFilePath());
    }

    public MediaType resolveMediaType(Music music) {
        if (music.getContentType() != null && !music.getContentType().isBlank()) {
            try {
                return MediaType.parseMediaType(music.getContentType());
            } catch (Exception ignored) {
            }
        }
        return MediaType.valueOf("audio/mpeg");
    }

    @Transactional
    public Music saveUpload(MultipartFile file, String title, com.studychain.models.Music.Category category, Long uploaderUserId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "track.mp3");
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot > -1 && dot < originalFilename.length() - 1) {
            ext = originalFilename.substring(dot).toLowerCase();
        }
        if (!".mp3".equals(ext)) {
            // Allow only mp3 for now
            throw new IllegalArgumentException("Only MP3 files are allowed");
        }
        String storedName = UUID.randomUUID().toString() + ext;
        Path target = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), target);

        Music music = new Music();
        music.setTitle((title != null && !title.isBlank()) ? title : originalFilename.replace(ext, ""));
        music.setFilePath(target.toString());
        music.setContentType(file.getContentType() != null ? file.getContentType() : "audio/mpeg");
        music.setUploadedByUserId(uploaderUserId);
        music.setCategory(category != null ? category : com.studychain.models.Music.Category.MUSIC);
        // createdAt default
        return musicRepository.save(music);
    }
}


