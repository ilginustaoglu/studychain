package com.studychain.repositories;

import com.studychain.models.StudySession;
import com.studychain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
	List<StudySession> findAllByUserAndSessionDateBetweenOrderBySessionDateAsc(User user, LocalDate start, LocalDate end);
	void deleteAllByUser(User user);
	void deleteAllByUserAndSessionDate(User user, LocalDate sessionDate);
}


