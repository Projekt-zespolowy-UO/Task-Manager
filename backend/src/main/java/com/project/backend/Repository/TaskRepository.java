package com.project.backend.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.Model.TaskModel;

public interface TaskRepository extends JpaRepository<TaskModel, Long> {

    List<TaskModel> findByUser_IdOrderByIdAsc(Long userId);

    List<TaskModel> findByUser_IdAndCategory_User_IdOrderByIdAsc(Long userId, Long categoryUserId);

    Optional<TaskModel> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM TaskModel t WHERE t.category.id = :categoryId AND t.user.id = :userId")
    int deleteByCategoryIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE TaskModel t
            SET t.category = NULL
            WHERE t.category.id = :categoryId
              AND t.dashboard.id = :dashboardId
            """)
    int clearCategoryForDashboardTasks(
            @Param("categoryId") Long categoryId,
            @Param("dashboardId") Long dashboardId);

    @Query("""
            SELECT t
            FROM TaskModel t
            LEFT JOIN FETCH t.category c
            LEFT JOIN FETCH t.status s
            LEFT JOIN FETCH t.dashboard d
            WHERE t.user.id = :userId
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:statusId IS NULL OR s.id = :statusId)
              AND (
                    :search IS NULL
                    OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR (t.description IS NOT NULL AND LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
                  )
            ORDER BY t.id ASC
            """)
    Slice<TaskModel> findOwnedTasksForCsvExport(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("statusId") Long statusId,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT t
            FROM TaskModel t
            JOIN FETCH t.user u
            LEFT JOIN FETCH t.category c
            LEFT JOIN FETCH t.status s
            WHERE t.deadline IS NOT NULL
              AND t.deadline BETWEEN :fromDate AND :toDate
              AND t.dueNotificationSentAt IS NULL
              AND u.email IS NOT NULL
              AND (s IS NULL OR s.systemKey <> :doneSystemKey)
            ORDER BY t.deadline ASC, t.id ASC
            """)
    List<TaskModel> findTasksForDueNotifications(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("doneSystemKey") String doneSystemKey);

    // Shared Dashboard Queries

    @Query("""
            SELECT t
            FROM TaskModel t
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.status
            LEFT JOIN FETCH t.dashboard
            WHERE t.dashboard.id = :dashboardId
            ORDER BY t.id ASC
            """)
    List<TaskModel> findByDashboard_IdOrderByIdAsc(@Param("dashboardId") Long dashboardId);

    @Query("""
            SELECT t
            FROM TaskModel t
            WHERE t.id = :taskId
              AND t.dashboard.id = :dashboardId
            """)
    Optional<TaskModel> findByIdAndDashboard_Id(
            @Param("taskId") Long taskId,
            @Param("dashboardId") Long dashboardId
    );

    @Query("""
            SELECT t
            FROM TaskModel t
            LEFT JOIN FETCH t.category c
            LEFT JOIN FETCH t.status s
            LEFT JOIN FETCH t.dashboard d
            WHERE d.id IN (
                SELECT dm.dashboard.id
                FROM DashboardMember dm
                WHERE dm.user.id = :userId
            )
            AND (:categoryId IS NULL OR c.id = :categoryId)
            AND (:statusId IS NULL OR s.id = :statusId)
            AND (
                :search IS NULL
                OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR (t.description IS NOT NULL AND LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
            )
            ORDER BY t.id ASC
            """)
    Slice<TaskModel> findAccessibleTasksForCsvExport(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("statusId") Long statusId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT t
            FROM TaskModel t
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.status
            LEFT JOIN FETCH t.dashboard
            WHERE t.dashboard.id IN (
                SELECT dm.dashboard.id
                FROM DashboardMember dm
                WHERE dm.user.id = :userId
            )
            ORDER BY t.id ASC
            """)
    List<TaskModel> findAccessibleTasks(@Param("userId") Long userId);
}
