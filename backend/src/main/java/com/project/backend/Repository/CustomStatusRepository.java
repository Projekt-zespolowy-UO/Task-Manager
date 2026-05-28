package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.Model.CustomStatusModel;

public interface CustomStatusRepository extends JpaRepository<CustomStatusModel, Long> {

    List<CustomStatusModel> findByUser_IdOrderByPositionAscIdAsc(Long userId);

    Optional<CustomStatusModel> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUser_IdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
