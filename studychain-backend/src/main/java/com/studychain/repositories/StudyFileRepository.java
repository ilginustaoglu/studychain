package com.studychain.repositories;

import com.studychain.models.StudyFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyFileRepository extends JpaRepository<StudyFile, Long> {
	List<StudyFile> findAllByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
	List<StudyFile> findAllByOrderByCreatedAtDesc();
}


