package com.studychain.repositories;

import com.studychain.models.StudyDailyStat;
import com.studychain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyDailyStatRepository extends JpaRepository<StudyDailyStat, Long> {
	Optional<StudyDailyStat> findByUserAndStatDate(User user, LocalDate statDate);
	List<StudyDailyStat> findAllByUserAndStatDateBetweenOrderByStatDateAsc(User user, LocalDate start, LocalDate end);
	void deleteAllByUser(User user);
	void deleteByUserAndStatDate(User user, LocalDate statDate);
}


