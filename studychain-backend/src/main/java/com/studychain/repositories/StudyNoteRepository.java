package com.studychain.repositories;

import com.studychain.models.StudyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {
	List<StudyNote> findAllByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
	List<StudyNote> findAllByOrderByUpdatedAtDesc();
}


