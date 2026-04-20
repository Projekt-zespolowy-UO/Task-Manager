package com.project.backend.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.TaskModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Repository.TaskRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping
    public TaskModel createTask(
            @RequestBody TaskCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskModel task = new TaskModel();

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());

        // category
        CategoryModel category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        task.setCategory(category);

        // user (з JWT)
        UserModel user = userDetails.user();
        task.setUser(user);

        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public TaskModel updateTask(
            @PathVariable Long id,
            @RequestBody TaskCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UserModel user = userDetails.user();

        TaskModel task = taskRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());

        if (dto.getCategoryId() != null) {
            CategoryModel category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            task.setCategory(category);
        }

        return taskRepository.save(task);
    }
}
