package com.project.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.CategoryModel;

public interface CategoryRepository extends JpaRepository<CategoryModel, Long> {
}