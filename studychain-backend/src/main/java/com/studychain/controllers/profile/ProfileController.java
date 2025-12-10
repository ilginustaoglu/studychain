package com.studychain.controllers.profile;

import com.studychain.models.DailyNote;
import com.studychain.models.User;
import com.studychain.repositories.DailyNoteRepository;
import com.studychain.repositories.UserRepository;
import com.studychain.repositories.StudyDailyStatRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Controller
public class ProfileController {

	private final UserRepository userRepository;
	private final DailyNoteRepository dailyNoteRepository;
	private final StudyDailyStatRepository studyDailyStatRepository;

	public ProfileController(UserRepository userRepository, DailyNoteRepository dailyNoteRepository, StudyDailyStatRepository studyDailyStatRepository) {
		this.userRepository = userRepository;
		this.dailyNoteRepository = dailyNoteRepository;
		this.studyDailyStatRepository = studyDailyStatRepository;
	}

	@GetMapping("/profile")
	public String profile(HttpSession session, Model model) {
		Object username = session.getAttribute("username");
		Long userId = (Long) session.getAttribute("userId");
		if (username == null || userId == null) {
			return "redirect:/auth/login";
		}
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			session.invalidate();
			return "redirect:/auth/login";
		}
		User user = userOpt.get();
		model.addAttribute("username", username);
		model.addAttribute("user", user);
		model.addAttribute("hasPhoto", user.getProfileImage() != null && user.getProfileImage().length > 0);
		model.addAttribute("hasCover", user.getCoverImage() != null && user.getCoverImage().length > 0);
		// Determine display name based on preference
		String mode = user.getDisplayNameMode() == null ? "username" : user.getDisplayNameMode();
		String displayName = "full_name".equalsIgnoreCase(mode)
			? (user.getFirstName() + " " + user.getLastName()).trim()
			: user.getUsername();
		model.addAttribute("displayName", displayName);

		// Format dates as "day month year" with month name in English
		Locale enLocale = Locale.ENGLISH;
		DateTimeFormatter dmyFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", enLocale);
		String birthDateFormatted = user.getBirthDate() != null
			? user.getBirthDate().format(dmyFormatter)
			: "-";
		Instant createdAt = user.getCreatedAt();
		String joinedFormatted;
		if (createdAt != null) {
			ZonedDateTime zdt = createdAt.atZone(ZoneId.systemDefault());
			joinedFormatted = zdt.format(dmyFormatter);
		} else {
			joinedFormatted = "-";
		}
		model.addAttribute("birthDateFormatted", birthDateFormatted);
		model.addAttribute("joinedFormatted", joinedFormatted);
		model.addAttribute("about", user.getAbout() == null ? "" : user.getAbout());
		// Load real study stats aggregated by day for last 7 and 30 days
		Map<String, Long> studyStatsWeek = new LinkedHashMap<>();
		LocalDate today = LocalDate.now();
		LocalDate weekStart = today.minusDays(6);
		LocalDate monthStart = today.minusDays(29);
		// initialize with zeros
		for (int i = 6; i >= 0; i--) {
			LocalDate d = today.minusDays(i);
			studyStatsWeek.put(d.toString(), 0L);
		}
		Map<String, Long> studyStatsMonth = new LinkedHashMap<>();
		for (int i = 29; i >= 0; i--) {
			LocalDate d = today.minusDays(i);
			studyStatsMonth.put(d.toString(), 0L);
		}
		// fill from DB
		var weekStats = studyDailyStatRepository.findAllByUserAndStatDateBetweenOrderByStatDateAsc(user, weekStart, today);
		weekStats.forEach(row -> studyStatsWeek.put(row.getStatDate().toString(), row.getMinutes()));
		var monthStats = studyDailyStatRepository.findAllByUserAndStatDateBetweenOrderByStatDateAsc(user, monthStart, today);
		monthStats.forEach(row -> studyStatsMonth.put(row.getStatDate().toString(), row.getMinutes()));
		// Build English day-name keyed map for week
		Map<String, Long> studyStatsWeekDays = new LinkedHashMap<>();
		for (Map.Entry<String, Long> e : studyStatsWeek.entrySet()) {
			LocalDate d = LocalDate.parse(e.getKey());
			String dayName = d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
			studyStatsWeekDays.put(dayName, e.getValue());
		}
		model.addAttribute("studyStatsWeek", studyStatsWeek);
		model.addAttribute("studyStatsWeekDays", studyStatsWeekDays);
		model.addAttribute("studyStatsMonth", studyStatsMonth);
		// Recent daily notes
		List<com.studychain.models.DailyNote> notes = dailyNoteRepository.findTop30ByUserOrderByCreatedAtDesc(user);
		model.addAttribute("notes", notes);
		model.addAttribute("showBirth", user.getShowBirthDatePublic() == null ? true : user.getShowBirthDatePublic());
		model.addAttribute("showJoined", user.getShowJoinedDatePublic() == null ? true : user.getShowJoinedDatePublic());
		return "profile/index";
	}

	@GetMapping("/profile/photo")
	public ResponseEntity<byte[]> getPhoto(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return ResponseEntity.status(401).build();
		}
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		User user = userOpt.get();
		byte[] image = user.getProfileImage();
		if (image == null || image.length == 0) {
			return ResponseEntity.notFound().build();
		}
		String contentType = user.getProfileImageContentType();
		MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
		return ResponseEntity.ok()
			.header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
			.contentType(mediaType)
			.body(image);
	}

	@PostMapping("/profile/photo")
	public String uploadPhoto(@RequestParam("photo") MultipartFile photo, HttpSession session, Model model) {
		Object username = session.getAttribute("username");
		Long userId = (Long) session.getAttribute("userId");
		if (username == null || userId == null) {
			return "redirect:/auth/login";
		}
		if (photo == null || photo.isEmpty()) {
			return "redirect:/settings?error=L%C3%BCtfen+bir+resim+dosyas%C4%B1+se%C3%A7in#account";
		}
		String contentType = photo.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			return "redirect:/settings?error=Yaln%C4%B1zca+resim+dosyalar%C4%B1+y%C3%BCklenebilir#account";
		}
		if (photo.getSize() > (5L * 1024 * 1024)) { // 5MB
			return "redirect:/settings?error=Dosya+boyutu+5MB%27%C4%B1+ge%C3%A7emez#account";
		}
		return userRepository.findById(userId).map(user -> {
			try {
				user.setProfileImage(photo.getBytes());
				user.setProfileImageContentType(contentType);
				userRepository.save(user);
				return "redirect:/settings?success=Profile+photo+updated#account";
			} catch (IOException e) {
				return "redirect:/settings?error=Y%C3%BCkleme+s%C4%B1ras%C4%B1nda+bir+hata+olu%C5%9Ftu#account";
			}
		}).orElse("redirect:/auth/login");
	}

	@GetMapping("/profile/cover")
	public ResponseEntity<byte[]> getCover(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return ResponseEntity.status(401).build();
		}
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		User user = userOpt.get();
		byte[] image = user.getCoverImage();
		if (image == null || image.length == 0) {
			return ResponseEntity.notFound().build();
		}
		String contentType = user.getCoverImageContentType();
		MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
		return ResponseEntity.ok()
			.header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
			.contentType(mediaType)
			.body(image);
	}

	@PostMapping("/profile/cover")
	public String uploadCover(@RequestParam("cover") MultipartFile cover, HttpSession session, Model model) {
		Object username = session.getAttribute("username");
		Long userId = (Long) session.getAttribute("userId");
		if (username == null || userId == null) {
			return "redirect:/auth/login";
		}
		if (cover == null || cover.isEmpty()) {
			return "redirect:/settings?error=L%C3%BCtfen+bir+resim+dosyas%C4%B1+se%C3%A7in#account";
		}
		String contentType = cover.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			return "redirect:/settings?error=Yaln%C4%B1zca+resim+dosyalar%C4%B1+y%C3%BCklenebilir#account";
		}
		if (cover.getSize() > (8L * 1024 * 1024)) { // 8MB for cover
			return "redirect:/settings?error=Dosya+boyutu+8MB%27%C4%B1+ge%C3%A7emez#account";
		}
		return userRepository.findById(userId).map(user -> {
			try {
				user.setCoverImage(cover.getBytes());
				user.setCoverImageContentType(contentType);
				userRepository.save(user);
				return "redirect:/settings?success=Cover+photo+updated#account";
			} catch (IOException e) {
				return "redirect:/settings?error=Y%C3%BCkleme+s%C4%B1ras%C4%B1nda+bir+hata+olu%C5%9Ftu#account";
			}
		}).orElse("redirect:/auth/login");
	}

	@PostMapping("/profile/notes")
	public String addNote(@RequestParam("content") String content, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		if (content == null || content.isBlank()) return "redirect:/profile";
		if (content.length() > 280) return "redirect:/profile";
		return userRepository.findById(userId).map(user -> {
			DailyNote note = new DailyNote();
			note.setUser(user);
			note.setContent(content.trim());
			dailyNoteRepository.save(note);
			return "redirect:/profile";
		}).orElse("redirect:/auth/login");
	}

	@PostMapping("/profile/notes/{id}/delete")
	public String deleteNote(@PathVariable("id") Long noteId, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return "redirect:/auth/login";
		return userRepository.findById(userId).map(user -> {
			dailyNoteRepository.findById(noteId).ifPresent(note -> {
				if (note.getUser().getId().equals(user.getId())) {
					dailyNoteRepository.delete(note);
				}
			});
			return "redirect:/profile";
		}).orElse("redirect:/auth/login");
	}
}


