package com.project.backend.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.backend.Dto.ReminderTaskDto;
import com.project.backend.Model.ReminderTask;
import com.project.backend.Repository.ReminderTaskRepository;

@Service
public class ReminderTaskService {

    @Autowired
    private ReminderTaskRepository reminderTaskRepository;

    public List<ReminderTask> getAllReminderTasks() {
        return reminderTaskRepository.findAll();
    }

    public Optional<ReminderTask> getReminderTaskById(Long id) {
        return reminderTaskRepository.findById(id);
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
        Optional<ReminderTask> existingTask = reminderTaskRepository.findById(id);
        if (existingTask.isPresent()) {
            ReminderTask reminderTask = existingTask.get();
            reminderTask.setTitle(reminderTaskDto.getTitle());
            reminderTask.setDescription(reminderTaskDto.getDescription());
            reminderTask.setReminderTime(reminderTaskDto.getReminderTime());
            return reminderTaskRepository.save(reminderTask);
        }
        return null;
    }

    public void deleteReminderTask(Long id) {
        reminderTaskRepository.deleteById(id);
    }
}