package com.project.backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.TaskStatusCreateDto;
import com.project.backend.Dto.TaskStatusRenameDto;
import com.project.backend.Dto.TaskStatusResponseDto;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.TaskStatusModel;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.DashboardRepository;
import com.project.backend.Repository.TaskStatusRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskStatusService {

    private final TaskStatusRepository taskStatusRepository;
    private final DashboardRepository dashboardRepository;

    @Transactional(readOnly = true)
    public List<TaskStatusResponseDto> getStatuses(Long dashboardId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        requireOwnedDashboard(dashboardId, user.getId());

        return taskStatusRepository.findByDashboard_IdOrderByPositionAsc(dashboardId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskStatusResponseDto createStatus(TaskStatusCreateDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        Dashboard dashboard = requireOwnedDashboard(dto.getDashboardId(), user.getId());

        int nextPosition = taskStatusRepository.findByDashboard_IdOrderByPositionAsc(dashboard.getId()).size();

        TaskStatusModel status = new TaskStatusModel();
        status.setName(dto.getName().trim());
        status.setPosition(nextPosition);
        status.setSystemKey(null);
        status.setDashboard(dashboard);

        return toResponse(taskStatusRepository.save(status));
    }

    @Transactional
    public TaskStatusResponseDto renameStatus(Long statusId, TaskStatusRenameDto dto, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskStatusModel status = requireOwnedStatus(statusId, user.getId());

        status.setName(dto.getName().trim());

        return toResponse(taskStatusRepository.save(status));
    }

    @Transactional
    public void deleteStatus(Long statusId, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        TaskStatusModel status = requireOwnedStatus(statusId, user.getId());

        if (status.getSystemKey() != null) {
            throw ApiError.badRequest("Default status cannot be deleted");
        }

        taskStatusRepository.delete(status);
    }

    private TaskStatusResponseDto toResponse(TaskStatusModel status) {
        return new TaskStatusResponseDto(
                status.getId(),
                status.getName(),
                status.getPosition(),
                status.getDashboard().getId(),
                status.getSystemKey());
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw ApiError.unauthorized("Authenticated user is required");
        }

        return userDetails.user();
    }

    private Dashboard requireOwnedDashboard(Long dashboardId, Long userId) {
        return dashboardRepository.findByIdAndUser_Id(dashboardId, userId)
                .orElseThrow(() -> ApiError.notFound("Dashboard not found"));
    }

    private TaskStatusModel requireOwnedStatus(Long statusId, Long userId) {
        TaskStatusModel status = taskStatusRepository.findById(statusId)
                .orElseThrow(() -> ApiError.notFound("Status not found"));

        if (status.getDashboard() == null
                || status.getDashboard().getUser() == null
                || !status.getDashboard().getUser().getId().equals(userId)) {
            throw ApiError.notFound("Status not found");
        }

        return status;
    }
}