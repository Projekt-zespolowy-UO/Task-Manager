package com.project.backend.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.TaskModel;

public interface TaskRepository extends JpaRepository<TaskModel, Long> {
    Optional<TaskModel> findByIdAndUser_Id(Long id, Long userId);
}
