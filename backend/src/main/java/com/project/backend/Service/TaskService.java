package com.project.backend.Service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.TaskModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Repository.TaskRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public TaskResponseDto createTask(TaskCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        TaskModel task = new TaskModel();
        task.setTitle(dto.getTitle().trim());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setCategory(category);
        task.setUser(user);

        return toResponse(taskRepository.save(task));
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userDetails.user();
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
