package com.project.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.Model.ReminderTask;

@Repository
public interface ReminderTaskRepository extends JpaRepository<ReminderTask, Long> {
}