package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.Model.Dashboard;
import com.project.backend.Model.UserModel;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {
    List<Dashboard> findByUser(UserModel user);

    List<Dashboard> findByUser_IdOrderByIdAsc(Long userId);

    Optional<Dashboard> findByIdAndUser_Id(Long id, Long userId);
}
