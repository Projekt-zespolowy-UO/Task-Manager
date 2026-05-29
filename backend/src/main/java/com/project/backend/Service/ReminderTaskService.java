package com.project.backend.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.backend.Dto.ReminderTaskDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.ReminderTask;
import com.project.backend.Repository.ReminderTaskRepository;

@Service
public class ReminderTaskService {

    @Autowired
    private ReminderTaskRepository reminderTaskRepository;

    public List<ReminderTask> getAllReminderTasks() {
        return reminderTaskRepository.findAll();
    }

    public ReminderTask getReminderTaskById(Long id) {
        return reminderTaskRepository.findById(id)
                .orElseThrow(() -> ApiError.notFound("Reminder not found"));
    }

    public ReminderTask createReminderTask(ReminderTaskDto reminderTaskDto) {
        ReminderTask reminderTask = new ReminderTask();
        reminderTask.setTitle(reminderTaskDto.getTitle());
        reminderTask.setDescription(reminderTaskDto.getDescription());
        reminderTask.setReminderTime(reminderTaskDto.getReminderTime());
        reminderTask.setCompleted(false);
        return reminderTaskRepository.save(reminderTask);
    }

    public ReminderTask updateReminderTask(Long id, ReminderTaskDto reminderTaskDto) {
        ReminderTask reminderTask = reminderTaskRepository.findById(id)
                .orElseThrow(() -> ApiError.notFound("Reminder not found"));
        reminderTask.setTitle(reminderTaskDto.getTitle());
        reminderTask.setDescription(reminderTaskDto.getDescription());
        reminderTask.setReminderTime(reminderTaskDto.getReminderTime());
        return reminderTaskRepository.save(reminderTask);
    }

    public void deleteReminderTask(Long id) {
        if (!reminderTaskRepository.existsById(id)) {
            throw ApiError.notFound("Reminder not found");
        }
        reminderTaskRepository.deleteById(id);
    }
}
