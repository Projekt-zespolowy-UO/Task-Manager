package com.project.backend.Service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.TaskCategoryUpdateDto;
import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskFilterDto;
import com.project.backend.Dto.TaskPatchDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Dto.TaskStatusUpdateDto;
import com.project.backend.Dto.TaskUpdateDto;
import com.project.backend.Enum.Priority;
import com.project.backend.Enum.Status;
import com.project.backend.Model.CategoryModel;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.TaskModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.CategoryRepository;
import com.project.backend.Repository.DashboardRepository;
import com.project.backend.Repository.TaskRepository;
import com.project.backend.Repository.TaskSpecifications;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final DashboardRepository dashboardRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "status", "priority", "deadline");

    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasks(TaskFilterDto filter, Sort sort, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);

        Sort effectiveSort = sanitizeSort(sort);
        return taskRepository.findAll(
                        TaskSpecifications.forUserAndFilter(user.getId(), filter),
                        effectiveSort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TaskResponseDto> getTasksPage(TaskFilterDto filter, Pageable pageable, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);

        Pageable effectivePageable = sanitizePageable(pageable);
        return taskRepository
                .findAll(TaskSpecifications.forUserAndFilter(user.getId(), filter), effectivePageable)
                .map(this::toResponse);
    }

    @Transactional
    public TaskResponseDto createTask(TaskCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        CategoryModel category = requireOwnedCategory(dto.getCategoryId(), user.getId());
        Dashboard dashboard = dto.getDashboardId() != null
                ? requireOwnedDashboard(dto.getDashboardId(), user.getId())
                : null;

        TaskModel task = new TaskModel();
        task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : Status.TODO);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        task.setDeadline(dto.getDeadline());
        task.setCategory(category);
        task.setUser(user);
        task.setDashboard(dashboard);

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto updateTask(Long taskId, TaskUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setCategory(requireOwnedCategory(dto.getCategoryId(), user.getId()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto patchTask(Long taskId, TaskPatchDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        if (dto.getTitle() != null) {
            task.setTitle(normalizeRequiredTitle(dto.getTitle()));
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }
        if (dto.getDeadline() != null) {
            task.setDeadline(dto.getDeadline());
        }
        if (dto.getCategoryId() != null) {
            task.setCategory(requireOwnedCategory(dto.getCategoryId(), user.getId()));
        }

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto changeStatus(Long taskId, TaskStatusUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        task.setStatus(dto.getStatus());

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto changeCategory(Long taskId, TaskCategoryUpdateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());

        task.setCategory(requireOwnedCategory(dto.getCategoryId(), user.getId()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskModel task = requireOwnedTask(taskId, user.getId());
        taskRepository.delete(task);
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userDetails.user();
    }

    private TaskModel requireOwnedTask(Long taskId, Long userId) {
        return taskRepository.findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private CategoryModel requireOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private Dashboard requireOwnedDashboard(Long dashboardId, Long userId) {
        return dashboardRepository.findByIdAndUser_Id(dashboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found"));
    }

    private Sort sanitizeSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        List<Sort.Order> safeOrders = sort.stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList();

        if (safeOrders.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort must reference one of: " + ALLOWED_SORT_FIELDS);
        }
        return Sort.by(safeOrders);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        Sort safeSort = sanitizeSort(pageable.getSort());
        if (safeSort.equals(pageable.getSort())) {
            return pageable;
        }
        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                safeSort);
    }

    private String normalizeRequiredTitle(String title) {
        String trimmed = title == null ? null : title.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must not be blank");
        }
        return trimmed;
    }

    private TaskResponseDto toResponse(TaskModel task) {
        CategoryModel category = task.getCategory();

        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                category != null ? category.getId() : null,
                task.getDashboard() != null ? task.getDashboard().getId() : null);
    }
}
