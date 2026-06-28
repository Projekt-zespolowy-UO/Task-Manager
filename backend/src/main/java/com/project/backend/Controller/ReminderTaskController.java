package com.project.backend.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.ReminderTaskDto;
import com.project.backend.Model.ReminderTask;
import com.project.backend.Service.ReminderTaskService;

@RestController
@RequestMapping("/api/reminders")
public class ReminderTaskController {

    @Autowired
    private ReminderTaskService reminderTaskService;

    @GetMapping
    public List<ReminderTask> getAllReminders() {
        return reminderTaskService.getAllReminderTasks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReminderTask> getReminderById(@PathVariable Long id) {
        return ResponseEntity.ok(reminderTaskService.getReminderTaskById(id));
    }

    @PostMapping
    public ReminderTask createReminder(@RequestBody ReminderTaskDto reminderTaskDto) {
        return reminderTaskService.createReminderTask(reminderTaskDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReminderTask> updateReminder(@PathVariable Long id, @RequestBody ReminderTaskDto reminderTaskDto) {
        ReminderTask updatedTask = reminderTaskService.updateReminderTask(id, reminderTaskDto);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id) {
        reminderTaskService.deleteReminderTask(id);
        return ResponseEntity.noContent().build();
    }
}
