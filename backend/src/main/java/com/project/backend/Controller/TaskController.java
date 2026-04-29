package com.project.backend.Controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.TaskModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Repository.TaskRepository;
import com.project.backend.Security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping
    public TaskResponseDto createTask(
            @Valid @RequestBody TaskCreateDto dto,
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

        TaskModel savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    private TaskResponseDto toResponse(TaskModel task) {
        CategoryModel category = task.getCategory();
        UserModel user = task.getUser();

        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                user != null ? user.getId() : null);
    }
}
