package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.TaskStatusModel;

public interface TaskStatusRepository extends JpaRepository<TaskStatusModel, Long> {

    List<TaskStatusModel> findByCategory_IdOrderByPositionAsc(Long categoryId);

    Optional<TaskStatusModel> findByIdAndCategory_Id(Long id, Long categoryId);

    Optional<TaskStatusModel> findByCategory_IdAndSystemKey(Long categoryId, String systemKey);
}