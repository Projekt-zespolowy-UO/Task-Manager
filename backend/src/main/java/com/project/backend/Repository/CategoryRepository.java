package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.CategoryModel;

public interface CategoryRepository extends JpaRepository<CategoryModel, Long> {
    List<CategoryModel> findByUser_IdOrderByIdAsc(Long userId);

    Optional<CategoryModel> findByIdAndUser_Id(Long id, Long userId);
}
