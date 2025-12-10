package com.studychain.controllers.music;

import com.studychain.models.Music;
import com.studychain.models.User;
import com.studychain.models.UserRole;
import com.studychain.repositories.UserRepository;
import com.studychain.services.MusicService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Controller
public class MusicController {

    private final MusicService musicService;
    private final UserRepository userRepository;

    public MusicController(MusicService musicService, UserRepository userRepository) {
        this.musicService = musicService;
        this.userRepository = userRepository;
    }

    @PostMapping(path = "/music/upload")
    public String uploadMusic(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "title", required = false) String title,
                              @RequestParam(value = "category", required = false, defaultValue = "MUSIC") String category,
                              HttpSession session) throws IOException {
        Object uid = session.getAttribute("userId");
        if (uid == null) {
            return "redirect:/auth/login";
        }
        Long userId = (Long) uid;
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty() || user.get().getRole() != UserRole.ADMIN) {
            return "redirect:/home?error=forbidden";
        }
        com.studychain.models.Music.Category cat;
        try {
            cat = com.studychain.models.Music.Category.valueOf(category.toUpperCase());
        } catch (Exception e) {
            cat = com.studychain.models.Music.Category.MUSIC;
        }
        musicService.saveUpload(file, title, cat, userId);
        return "redirect:/home";
    }

    @GetMapping(path = "/music/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable("id") Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/auth/login").build();
        }
        Music music = musicService.getById(id);
        if (music == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = musicService.getResource(music);
        MediaType mediaType = musicService.resolveMediaType(music);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }
}


