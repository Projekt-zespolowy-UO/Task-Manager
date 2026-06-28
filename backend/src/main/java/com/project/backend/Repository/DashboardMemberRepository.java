package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.Enum.DashboardRole;
import com.project.backend.Model.DashboardMember;

@Repository
public interface DashboardMemberRepository extends JpaRepository<DashboardMember, Long> {

    List<DashboardMember> findByDashboard_IdOrderByCreatedAtAsc(Long dashboardId);

    Optional<DashboardMember> findByDashboard_IdAndUser_Id(Long dashboardId, Long userId);

    List<DashboardMember> findByUser_IdOrderByCreatedAtAsc(Long userId);

    @Query("""
            SELECT dm
            FROM DashboardMember dm
            WHERE dm.dashboard.id = :dashboardId
              AND dm.role = :role
            """)
    List<DashboardMember> findByDashboardIdAndRole(@Param("dashboardId") Long dashboardId, @Param("role") DashboardRole role);

    @Query("""
            SELECT COUNT(dm) > 0
            FROM DashboardMember dm
            WHERE dm.dashboard.id = :dashboardId
              AND dm.user.id = :userId
            """)
    boolean existsByDashboardIdAndUserId(@Param("dashboardId") Long dashboardId, @Param("userId") Long userId);

    int deleteByDashboard_IdAndUser_Id(Long dashboardId, Long userId);
}
