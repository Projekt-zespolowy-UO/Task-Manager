package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.TaskStatusModel;

public interface TaskStatusRepository extends JpaRepository<TaskStatusModel, Long> {

    List<TaskStatusModel> findByDashboard_IdOrderByPositionAsc(Long dashboardId);

    Optional<TaskStatusModel> findByIdAndDashboard_Id(Long id, Long dashboardId);

    Optional<TaskStatusModel> findByDashboard_IdAndSystemKey(Long dashboardId, String systemKey);
}