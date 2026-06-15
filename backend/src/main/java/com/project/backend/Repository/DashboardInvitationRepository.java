package com.project.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.Enum.InvitationStatus;
import com.project.backend.Model.DashboardInvitation;

@Repository
public interface DashboardInvitationRepository extends JpaRepository<DashboardInvitation, Long> {

    List<DashboardInvitation> findByInvitedUser_IdAndStatusOrderByCreatedAtDesc(Long userId, InvitationStatus status);

    @Query("""
            SELECT COUNT(di) > 0
            FROM DashboardInvitation di
            WHERE di.dashboard.id = :dashboardId
              AND di.invitedUser.id = :userId
              AND di.status = 'PENDING'
            """)
    boolean existsPendingInvitation(@Param("dashboardId") Long dashboardId, @Param("userId") Long userId);

    Optional<DashboardInvitation> findByIdAndInvitedUser_Id(Long invitationId, Long userId);
}
