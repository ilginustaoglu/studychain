package com.studychain.controllers.study;

import com.studychain.models.StudyDailyStat;
import com.studychain.models.StudySession;
import com.studychain.models.User;
import com.studychain.repositories.StudyDailyStatRepository;
import com.studychain.repositories.StudySessionRepository;
import com.studychain.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class StudyController {

	private final UserRepository userRepository;
	private final StudyDailyStatRepository statRepository;
	private final StudySessionRepository sessionRepository;

	public StudyController(UserRepository userRepository,
	                       StudyDailyStatRepository statRepository,
	                       StudySessionRepository sessionRepository) {
		this.userRepository = userRepository;
		this.statRepository = statRepository;
		this.sessionRepository = sessionRepository;
	}

	@PostMapping("/study/track")
	public ResponseEntity<?> addStudyMinutes(@RequestParam("minutes") long minutes,
	                                         @RequestParam(value = "date", required = false) String date,
	                                         HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) return ResponseEntity.status(401).build();
		if (minutes <= 0) return ResponseEntity.badRequest().body("minutes must be > 0");
		LocalDate statDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
		return userRepository.findById(userId).map(user -> {
			StudyDailyStat stat = statRepository.findByUserAndStatDate(user, statDate)
				.orElseGet(() -> {
					StudyDailyStat s = new StudyDailyStat();
					s.setUser(user);
					s.setStatDate(statDate);
					return s;
				});
			long newMinutes = Math.max(0, stat.getMinutes()) + minutes;
			stat.setMinutes(newMinutes);
			statRepository.save(stat);
			return ResponseEntity.ok().build();
		}).orElse(ResponseEntity.status(401).build());
	}

	@PostMapping("/study/session")
	public ResponseEntity<?> addSession(@RequestParam("minutes") long minutes,
	                                    @RequestParam(value = "date", required = false) String date,
	                                    HttpSession httpSession) {
		Long userId = (Long) httpSession.getAttribute("userId");
		if (userId == null) return ResponseEntity.status(401).build();
		if (minutes <= 0) return ResponseEntity.badRequest().body("minutes must be > 0");
		LocalDate day = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
		return userRepository.findById(userId).map(user -> {
			// record session
			StudySession s = new StudySession();
			s.setUser(user);
			s.setSessionDate(day);
			s.setMinutes(minutes);
			sessionRepository.save(s);
			// update daily aggregate
			StudyDailyStat stat = statRepository.findByUserAndStatDate(user, day)
				.orElseGet(() -> {
					StudyDailyStat d = new StudyDailyStat();
					d.setUser(user);
					d.setStatDate(day);
					return d;
				});
			stat.setMinutes(Math.max(0, stat.getMinutes()) + minutes);
			statRepository.save(stat);
			return ResponseEntity.ok().build();
		}).orElse(ResponseEntity.status(401).build());
	}

	@PostMapping("/study/clear-day")
	public ResponseEntity<?> clearDay(@RequestParam("date") String date, HttpSession httpSession) {
		Long userId = (Long) httpSession.getAttribute("userId");
		if (userId == null) return ResponseEntity.status(401).build();
		LocalDate day = LocalDate.parse(date);
		return userRepository.findById(userId).map(user -> {
			sessionRepository.deleteAllByUserAndSessionDate(user, day);
			statRepository.deleteByUserAndStatDate(user, day);
			return ResponseEntity.ok().build();
		}).orElse(ResponseEntity.status(401).build());
	}

	@PostMapping("/study/clear-all")
	public ResponseEntity<?> clearAll(HttpSession httpSession) {
		Long userId = (Long) httpSession.getAttribute("userId");
		if (userId == null) return ResponseEntity.status(401).build();
		return userRepository.findById(userId).map(user -> {
			sessionRepository.deleteAllByUser(user);
			statRepository.deleteAllByUser(user);
			return ResponseEntity.ok().build();
		}).orElse(ResponseEntity.status(401).build());
	}
}


