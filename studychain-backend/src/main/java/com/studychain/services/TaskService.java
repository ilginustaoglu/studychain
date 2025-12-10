package com.studychain.services;

import com.studychain.models.Task;
import com.studychain.models.User;
import com.studychain.repositories.TaskRepository;
import com.studychain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public List<Task> findAllByUserId(Long userId) {
        return taskRepository.findAllByUser_IdOrderByIdDesc(userId);
    }

    @Transactional
    public Task create(Long userId, String title) {
        User userRef = userRepository.getReferenceById(userId);
        Task task = new Task();
        task.setUser(userRef);
        task.setTitle(title);
        return taskRepository.save(task);
    }

    @Transactional
    public Optional<Task> toggleDone(Long userId, Long id) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isEmpty()) return Optional.empty();
        Task task = taskOpt.get();
        if (!task.getUser().getId().equals(userId)) return Optional.empty();
        task.setDone(!task.isDone());
        return Optional.of(taskRepository.save(task));
    }

    @Transactional
    public boolean delete(Long userId, Long id) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isEmpty()) return false;
        Task task = taskOpt.get();
        if (!task.getUser().getId().equals(userId)) return false;
        taskRepository.delete(task);
        return true;
    }
}


