package com.project.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.TaskModel;

public interface TaskRepository extends JpaRepository<TaskModel, Long> {
}