package com.project.backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final TaskStatusRepository taskStatusRepository;

    @Transactional(readOnly = true)
    public List<Dashboard> getDashboards(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        return dashboardRepository.findByUser_IdOrderByIdAsc(user.getId());
    }

    @Transactional
    public Dashboard createDashboard(CustomUserDetails userDetails, String name) {
        UserModel user = requireAuthenticatedUser(userDetails);
        String dashboardName = normalizeName(name);

        Dashboard dashboard = new Dashboard();
        dashboard.setUser(user);
        dashboard.setName(dashboardName);

        Dashboard savedDashboard = dashboardRepository.save(dashboard);
        createDefaultStatuses(savedDashboard);

        return savedDashboard;
    }

    @Transactional(readOnly = true)
    public Dashboard getDashboardById(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        return dashboardRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> ApiError.notFound("Dashboard not found"));
    }

    @Transactional
    public void deleteDashboard(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        Dashboard dashboard = dashboardRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> ApiError.notFound("Dashboard not found"));

        dashboardRepository.delete(dashboard);
    }

    private void createDefaultStatuses(Dashboard dashboard) {
        TaskStatusModel todo = new TaskStatusModel();
        todo.setName("To do");
        todo.setSystemKey("TODO");
        todo.setPosition(0);
        todo.setDashboard(dashboard);

        TaskStatusModel inProgress = new TaskStatusModel();
        inProgress.setName("In progress");
        inProgress.setSystemKey("IN_PROGRESS");
        inProgress.setPosition(1);
        inProgress.setDashboard(dashboard);

        TaskStatusModel done = new TaskStatusModel();
        done.setName("Done");
        done.setSystemKey("DONE");
        done.setPosition(2);
        done.setDashboard(dashboard);

        taskStatusRepository.saveAll(List.of(todo, inProgress, done));
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw ApiError.unauthorized("Authenticated user is required");
        }

        return userDetails.user();
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw ApiError.badRequest("Dashboard name is required");
        }

        return name.trim();
    }
}