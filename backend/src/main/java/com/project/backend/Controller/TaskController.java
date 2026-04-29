package com.project.backend.Controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.TaskCreateDto;
import com.project.backend.Dto.TaskResponseDto;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public TaskResponseDto createTask(
            @Valid @RequestBody TaskCreateDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.createTask(dto, userDetails);
    }
}
