package com.studychain.repositories;

import com.studychain.models.DailyNote;
import com.studychain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long> {
	List<DailyNote> findTop30ByUserOrderByCreatedAtDesc(User user);
}


