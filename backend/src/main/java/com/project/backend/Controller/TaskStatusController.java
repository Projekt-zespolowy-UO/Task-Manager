package com.project.backend.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.TaskStatusCreateDto;
import com.project.backend.Dto.TaskStatusRenameDto;
import com.project.backend.Dto.TaskStatusResponseDto;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.TaskStatusService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/statuses")
@RequiredArgsConstructor
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    @GetMapping
    public List<TaskStatusResponseDto> getStatuses(
            @RequestParam Long dashboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskStatusService.getStatuses(dashboardId, userDetails);
    }

    @PostMapping
    public TaskStatusResponseDto createStatus(
            @Valid @RequestBody TaskStatusCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskStatusService.createStatus(dto, userDetails);
    }

    @PutMapping("/{id}")
    public TaskStatusResponseDto renameStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusRenameDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskStatusService.renameStatus(id, dto, userDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        taskStatusService.deleteStatus(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}