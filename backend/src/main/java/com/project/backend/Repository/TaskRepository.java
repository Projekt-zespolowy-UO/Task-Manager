package com.project.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.TaskModel;

public interface TaskRepository extends JpaRepository<TaskModel, Long> {
    List<TaskModel> findByUser_IdAndCategory_User_IdOrderByIdAsc(Long userId, Long categoryUserId);
}
