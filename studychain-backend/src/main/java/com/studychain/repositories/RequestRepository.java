package com.studychain.repositories;

import com.studychain.models.Request;
import com.studychain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findAllByOrderByCreatedAtDesc();
    List<Request> findByUserOrderByCreatedAtDesc(User user);
}


