package com.project.backend.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.Enum.Status;
import com.project.backend.Model.TaskModel;

public interface TaskRepository extends JpaRepository<TaskModel, Long> {
    List<TaskModel> findByUser_IdAndCategory_User_IdOrderByIdAsc(Long userId, Long categoryUserId);

    List<TaskModel> findByUser_IdOrderByIdAsc(Long userId);

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
