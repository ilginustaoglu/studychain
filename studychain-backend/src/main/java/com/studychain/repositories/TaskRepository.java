package com.studychain.repositories;

import com.studychain.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByUser_IdOrderByIdDesc(Long userId);
} 

