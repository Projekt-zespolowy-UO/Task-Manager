package com.project.backend.Controller;

import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.Dto.TaskCategoryUpdateDto;
import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskImportResultDto;
import com.project.backend.Dto.TaskPatchDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Dto.TaskStatusUpdateDto;
import com.project.backend.Dto.TaskUpdateDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "CRUD operations for user tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "List all tasks for the authenticated user")
    public List<TaskResponseDto> getTasks(
            @RequestParam Long dashboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.getTasks(dashboardId, userDetails);
    }

    @GetMapping("/export.csv")
    @Operation(summary = "Export tasks available to the authenticated user as CSV")
    public ResponseEntity<byte[]> exportTasksCsv(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long dashboardId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) String search) {

        // Debug: check if userDetails is null
        if (userDetails == null) {
            System.out.println("🔴 DEBUG: userDetails is NULL in exportTasksCsv!");
            throw new RuntimeException("User not authenticated");
        }

        System.out.println("🟢 DEBUG: exportTasksCsv called for user: " + userDetails.getUsername());

        String filename = "tasks-" + LocalDate.now() + ".csv";
        byte[] csvData = taskService.getTasksCsvAsBytes(
                userDetails,
                dashboardId,
                categoryId,
                statusId,
                search);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(csvData);
    }

    @PostMapping(value = "/import.csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import tasks from a CSV file into a dashboard")
    public TaskImportResultDto importTasksCsv(
            @RequestParam Long dashboardId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (file == null || file.isEmpty()) {
            throw ApiError.badRequest("CSV file must not be empty");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw ApiError.badRequest("Failed to read the uploaded CSV file");
        }

        return taskService.importTasksFromCsv(userDetails, dashboardId, content);
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
