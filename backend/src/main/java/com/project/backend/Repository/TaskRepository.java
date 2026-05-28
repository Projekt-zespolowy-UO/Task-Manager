package com.project.backend.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.Enum.Status;
import com.project.backend.Model.TaskModel;

public interface TaskRepository extends JpaRepository<TaskModel, Long>, JpaSpecificationExecutor<TaskModel> {
    List<TaskModel> findByUser_IdOrderByIdAsc(Long userId);

    List<TaskModel> findByUser_IdAndCategory_User_IdOrderByIdAsc(Long userId, Long categoryUserId);

    Optional<TaskModel> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM TaskModel t WHERE t.category.id = :categoryId AND t.user.id = :userId")
    int deleteByCategoryIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    @Query("""
            SELECT t
            FROM TaskModel t
            JOIN FETCH t.user u
            LEFT JOIN FETCH t.category c
            WHERE t.deadline IS NOT NULL
              AND t.deadline BETWEEN :fromDate AND :toDate
              AND t.dueNotificationSentAt IS NULL
              AND u.email IS NOT NULL
              AND (t.status IS NULL OR t.status <> :doneStatus)
            ORDER BY t.deadline ASC, t.id ASC
            """)
    List<TaskModel> findTasksForDueNotifications(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("doneStatus") Status doneStatus);
}
