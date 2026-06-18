package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.CategoryModel;

public interface CategoryRepository extends JpaRepository<CategoryModel, Long> {
    List<CategoryModel> findByDashboard_IdOrderByIdAsc(Long dashboardId);

    Optional<CategoryModel> findByIdAndDashboard_Id(Long id, Long dashboardId);
}
