package com.project.backend.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Enum.Status;
import com.project.backend.Model.TaskModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskNotificationService {

    private final TaskRepository taskRepository;
    private final MailService mailService;

    @Value("${notifications.task-due-window-days:1}")
    private int dueWindowDays;

    @Scheduled(fixedDelayString = "${notifications.task-due-check-delay-ms:3600000}")
    @Transactional
    public void sendDueTaskNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(Math.max(dueWindowDays, 0));
        List<TaskModel> tasks = taskRepository.findTasksForDueNotifications(today, windowEnd, Status.DONE);

        for (TaskModel task : tasks) {
            sendNotification(task, today);
        }
    }

    private void sendNotification(TaskModel task, LocalDate today) {
        UserModel user = task.getUser();

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        try {
            mailService.sendEmail(
                    user.getEmail(),
                    "Task deadline reminder",
                    buildMessage(task, today));
            task.setDueNotificationSentAt(LocalDateTime.now());
        } catch (RuntimeException ex) {
            log.warn("Failed to send task deadline notification for task id {}", task.getId(), ex);
        }
    }

    private String buildMessage(TaskModel task, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, task.getDeadline());
        String deadlineText = daysLeft == 0
                ? "today"
                : "in " + daysLeft + " day(s)";

        String description = task.getDescription();
        String descriptionText = description == null || description.isBlank()
                ? ""
                : "\n\nDescription:\n" + description;

        return "Reminder: your task \"" + task.getTitle() + "\" is due " + deadlineText
                + " (" + task.getDeadline() + ")."
                + descriptionText;
    }
}
