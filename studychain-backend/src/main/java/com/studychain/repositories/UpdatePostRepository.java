package com.studychain.repositories;

import com.studychain.models.UpdatePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UpdatePostRepository extends JpaRepository<UpdatePost, Long> {
    List<UpdatePost> findAllByOrderByCreatedAtDesc();
}


