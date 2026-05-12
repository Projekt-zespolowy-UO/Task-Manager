package com.project.backend.Service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.Model.Dashboard;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.DashboardRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

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
        return dashboardRepository.save(dashboard);
    }

    @Transactional(readOnly = true)
    public Dashboard getDashboardById(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        return dashboardRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found"));
    }

    @Transactional
    public void deleteDashboard(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        Dashboard dashboard = dashboardRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found"));

        dashboardRepository.delete(dashboard);
    }

    private UserModel requireAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.user() == null || userDetails.user().getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userDetails.user();
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dashboard name is required");
        }

        return name.trim();
    }
}
