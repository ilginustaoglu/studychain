package com.studychain.repositories;

import com.studychain.models.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
	List<CalendarEvent> findAllByUser_IdOrderByStartTimeAsc(Long userId);
	List<CalendarEvent> findAllByUser_IdAndStartTimeBetweenOrderByStartTimeAsc(
		Long userId, LocalDateTime start, LocalDateTime end
	);
	Optional<CalendarEvent> findByUser_IdAndGoogleEventId(Long userId, String googleEventId);
	List<CalendarEvent> findAllByUser_IdAndGoogleEventIdIsNull(Long userId);
	List<CalendarEvent> findAllByUser_IdAndGoogleEventIdIsNotNullAndStartTimeBetween(
		Long userId, LocalDateTime start, LocalDateTime end
	);
}

