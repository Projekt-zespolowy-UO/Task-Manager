package com.project.backend.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.project.backend.Dto.TaskCategoryUpdateDto;
import com.project.backend.Dto.TaskCreateDto;
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
    @Operation(summary = "List all tasks for the authenticated user")
    public List<TaskResponseDto> getTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.getTasks(userDetails);
    }

    @GetMapping("/export.csv")
    @Operation(summary = "Export tasks available to the authenticated user as CSV")
    public ResponseEntity<StreamingResponseBody> exportTasksCsv(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) String search) {

        String filename = "tasks-" + LocalDate.now() + ".csv";
        StreamingResponseBody responseBody = outputStream ->
                taskService.writeTasksCsv(outputStream, userDetails, categoryId, statusId, search);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(responseBody);
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