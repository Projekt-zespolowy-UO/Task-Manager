package com.project.backend.Service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Enum.Priority;
import com.project.backend.Enum.Status;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.TaskModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Repository.DashboardRepository;
import com.project.backend.Repository.TaskRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final DashboardRepository dashboardRepository;

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasks(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);

        return taskRepository.findByUser_IdOrderByIdAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponseDto createTask(TaskCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = categoryRepository.findByIdAndUser_Id(dto.getCategoryId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        Dashboard dashboard = dashboardRepository.findByIdAndUser_Id(dto.getDashboardId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found"));

        TaskModel task = new TaskModel();
        task.setTitle(dto.getTitle().trim());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : Status.TODO);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        task.setDeadline(dto.getDeadline());
        task.setCategory(category);
        task.setUser(user);
        task.setDashboard(dashboard);

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

        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                category != null ? category.getId() : null,
                task.getDashboard() != null ? task.getDashboard().getId() : null);
    }
}
