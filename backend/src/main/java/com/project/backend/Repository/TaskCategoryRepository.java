package com.project.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.Model.TaskCategoryModel;

@Repository
public interface TaskCategoryRepository extends JpaRepository<TaskCategoryModel, Long> {
    // Custom query methods can be added here if needed
}