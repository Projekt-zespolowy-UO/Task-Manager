package com.project.backend.Controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Dto.PagedResponseDto;
import com.project.backend.Dto.TaskCategoryUpdateDto;
import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskFilterDto;
import com.project.backend.Dto.TaskPatchDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Dto.TaskStatusUpdateDto;
import com.project.backend.Dto.TaskUpdateDto;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "CRUD operations for user tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "List tasks for the authenticated user. "
            + "Supports filtering (status, priority, categoryId, dashboardId, deadlineFrom, deadlineTo, search), "
            + "sorting (sort=field,asc|desc; allowed fields: id, title, status, priority, deadline) "
            + "and optional pagination (page, size — when supplied the response is a paged envelope).")
    public ResponseEntity<?> getTasks(
            @ModelAttribute TaskFilterDto filter,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Sort parsedSort = parseSort(sort);

        if (page != null || size != null) {
            int effectivePage = page != null ? page : 0;
            int effectiveSize = size != null ? size : 20;
            if (effectivePage < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
            }
            if (effectiveSize <= 0 || effectiveSize > 200) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 200");
            }
            Pageable pageable = PageRequest.of(effectivePage, effectiveSize, parsedSort);
            return ResponseEntity.ok(
                    PagedResponseDto.from(taskService.getTasksPage(filter, pageable, userDetails)));
        }

        List<TaskResponseDto> tasks = taskService.getTasks(filter, parsedSort, userDetails);
        return ResponseEntity.ok(tasks);
    }

    private Sort parseSort(List<String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = sortParams.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(entry -> {
                    String[] parts = entry.split(",");
                    String property = parts[0].trim();
                    Sort.Direction direction = Sort.Direction.ASC;
                    if (parts.length > 1) {
                        try {
                            direction = Sort.Direction.fromString(parts[1].trim());
                        } catch (IllegalArgumentException ex) {
                            throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "Sort direction must be 'asc' or 'desc'");
                        }
                    }
                    return new Sort.Order(direction, property);
                })
                .toList();
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public TaskResponseDto createTask(
            @Valid @RequestBody TaskCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.createTask(dto, userDetails);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing task with new values")
    public TaskResponseDto updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.updateTask(id, dto, userDetails);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a task (only provided fields are changed)")
    public TaskResponseDto patchTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskPatchDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.patchTask(id, dto, userDetails);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change the status of a task")
    public TaskResponseDto changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.changeStatus(id, dto, userDetails);
    }

    @PatchMapping("/{id}/category")
    @Operation(summary = "Move a task to another category")
    public TaskResponseDto changeCategory(
            @PathVariable Long id,
            @Valid @RequestBody TaskCategoryUpdateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.changeCategory(id, dto, userDetails);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        taskService.deleteTask(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
