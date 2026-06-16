package com.project.backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Enum.DashboardRole;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.DashboardMember;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.DashboardMemberRepository;
import com.project.backend.Repository.DashboardRepository;
import com.project.backend.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final DashboardMemberRepository dashboardMemberRepository;
    private final DashboardAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<Dashboard> getDashboards(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        // Get dashboards where user is a member
        return dashboardMemberRepository.findByUser_IdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(DashboardMember::getDashboard)
                .toList();
    }

    @Transactional
    public Dashboard createDashboard(CustomUserDetails userDetails, String name) {
        UserModel user = requireAuthenticatedUser(userDetails);
        String dashboardName = normalizeName(name);

        Dashboard dashboard = new Dashboard();
        dashboard.setUser(user);
        dashboard.setName(dashboardName);

        Dashboard savedDashboard = dashboardRepository.save(dashboard);

        // Add creator as owner member
        DashboardMember ownerMember = new DashboardMember(savedDashboard, user, DashboardRole.OWNER);
        dashboardMemberRepository.save(ownerMember);

        return savedDashboard;
    }

    @Transactional(readOnly = true)
    public Dashboard getDashboardById(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        // Verify user is a member
        authorizationService.validateDashboardAccess(user.getId(), id);
        return authorizationService.getDashboardOrThrow(id);
    }

    @Transactional
    public void deleteDashboard(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        // Verify user is the owner
        authorizationService.validateDashboardOwnerAccess(user.getId(), id);
        Dashboard dashboard = authorizationService.getDashboardOrThrow(id);
        dashboardRepository.delete(dashboard);
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