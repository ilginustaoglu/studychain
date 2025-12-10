package com.studychain.controllers.studyfiles;

import com.studychain.models.StudyFile;
import com.studychain.models.User;
import com.studychain.models.UserRole;
import com.studychain.repositories.UserRepository;
import com.studychain.services.StudyFilesService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Controller
public class StudyFilesController {

	private final StudyFilesService studyFilesService;
	private final UserRepository userRepository;

	public StudyFilesController(StudyFilesService studyFilesService, UserRepository userRepository) {
		this.studyFilesService = studyFilesService;
		this.userRepository = userRepository;
	}

	@GetMapping("/study-files")
	public String index(HttpSession session, Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		model.addAttribute("username", session.getAttribute("username"));
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		model.addAttribute("isAdmin", isAdmin);
		if (isAdmin) {
			model.addAttribute("files", studyFilesService.listAllFiles());
			model.addAttribute("notes", studyFilesService.listAllNotes());
		} else {
			model.addAttribute("files", studyFilesService.listFiles(userId));
			model.addAttribute("notes", studyFilesService.listNotes(userId));
		}
		return "studyfiles/index";
	}

	@PostMapping("/study-files/upload")
	public String upload(@RequestParam("file") MultipartFile file, HttpSession session, Model model) throws IOException {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		try {
			studyFilesService.saveUpload(file, userId);
		} catch (IllegalArgumentException ex) {
			return "redirect:/study-files?error=" + ex.getMessage().replace(" ", "+");
		}
		return "redirect:/study-files";
	}

	@GetMapping("/study-files/{id}/view")
	public ResponseEntity<Resource> view(@PathVariable("id") Long id, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/auth/login").build();
		}
		StudyFile file = studyFilesService.getFile(id);
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		if (file == null || (!isAdmin && !Objects.equals(file.getOwnerUserId(), userId))) {
			return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/study-files?error=notfound").build();
		}
		Resource resource = studyFilesService.getResource(file);
		MediaType mediaType = studyFilesService.resolveMediaType(file);
		String disposition = mediaType.equals(MediaType.APPLICATION_PDF) ? "inline" : "inline";
		return ResponseEntity.ok()
			.contentType(mediaType)
			.header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + file.getOriginalName().replace("\"", "") + "\"")
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.body(resource);
	}

	@PostMapping("/study-files/notes")
	public String saveNote(@RequestParam(value = "title", required = false) String title,
	                       @RequestParam("content") String content,
	                       HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		try {
			var saved = studyFilesService.createNote(userId, title, content);
			return "redirect:/study-files/notes/" + saved.getId();
		} catch (IllegalArgumentException ex) {
			return "redirect:/study-files?error=" + ex.getMessage().replace(" ", "+");
		}
	}

	@GetMapping("/study-files/notes/new")
	public String newNotePage(HttpSession session, Model model) {
		if (session.getAttribute("userId") == null) {
			return "redirect:/auth/login";
		}
		model.addAttribute("username", session.getAttribute("username"));
		return "studyfiles/new-note";
	}

	@GetMapping("/study-files/notes/{id}")
	public String viewNote(@PathVariable("id") Long id, HttpSession session, Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		var note = studyFilesService.getNote(id);
		if (note == null) {
			return "redirect:/study-files?error=notfound";
		}
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		if (!isAdmin && !note.getOwnerUserId().equals(userId)) {
			return "redirect:/study-files?error=forbidden";
		}
		model.addAttribute("username", session.getAttribute("username"));
		model.addAttribute("note", note);
		return "studyfiles/note-view";
	}

	@GetMapping("/study-files/files/{id}")
	public String viewFilePage(@PathVariable("id") Long id, HttpSession session, Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		var file = studyFilesService.getFile(id);
		if (file == null) {
			return "redirect:/study-files?error=notfound";
		}
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		if (!isAdmin && !file.getOwnerUserId().equals(userId)) {
			return "redirect:/study-files?error=forbidden";
		}
		model.addAttribute("username", session.getAttribute("username"));
		model.addAttribute("file", file);
		return "studyfiles/file-view";
	}

	@PostMapping("/study-files/files/{id}/delete")
	public String deleteFile(@PathVariable("id") Long id, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		var file = studyFilesService.getFile(id);
		if (file == null) {
			return "redirect:/study-files?error=notfound";
		}
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		if (!isAdmin && !file.getOwnerUserId().equals(userId)) {
			return "redirect:/study-files?error=forbidden";
		}
		boolean ok = studyFilesService.deleteFile(file);
		return ok ? "redirect:/study-files?deleted=file" : "redirect:/study-files?error=deleteFailed";
	}

	@PostMapping("/study-files/notes/{id}/delete")
	public String deleteNote(@PathVariable("id") Long id, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		var note = studyFilesService.getNote(id);
		if (note == null) {
			return "redirect:/study-files?error=notfound";
		}
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		if (!isAdmin && !note.getOwnerUserId().equals(userId)) {
			return "redirect:/study-files?error=forbidden";
		}
		boolean ok = studyFilesService.deleteNote(note);
		return ok ? "redirect:/study-files?deleted=note" : "redirect:/study-files?error=deleteFailed";
	}

	@GetMapping("/study-files/notes/{id}/edit")
	public String editNotePage(@PathVariable("id") Long id, HttpSession session, Model model) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		var note = studyFilesService.getNote(id);
		if (note == null) {
			return "redirect:/study-files?error=notfound";
		}
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		if (!isAdmin && !note.getOwnerUserId().equals(userId)) {
			return "redirect:/study-files?error=forbidden";
		}
		model.addAttribute("username", session.getAttribute("username"));
		model.addAttribute("note", note);
		return "studyfiles/note-edit";
	}

	@PostMapping("/study-files/notes/{id}/edit")
	public String editNote(@PathVariable("id") Long id,
	                       @RequestParam(value = "title", required = false) String title,
	                       @RequestParam("content") String content,
	                       HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return "redirect:/auth/login";
		}
		var note = studyFilesService.getNote(id);
		if (note == null) {
			return "redirect:/study-files?error=notfound";
		}
		UserRole role = userRepository.findById(userId).map(User::getRole).orElse(UserRole.USER);
		boolean isAdmin = role == UserRole.ADMIN;
		if (!isAdmin && !note.getOwnerUserId().equals(userId)) {
			return "redirect:/study-files?error=forbidden";
		}
		try {
			studyFilesService.updateNote(note, title, content);
		} catch (IllegalArgumentException ex) {
			return "redirect:/study-files/notes/" + id + "/edit?error=" + ex.getMessage().replace(" ", "+");
		}
		return "redirect:/study-files/notes/" + id;
	}
}


