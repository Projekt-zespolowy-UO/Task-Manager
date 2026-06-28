package com.project.backend.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Enum.DashboardRole;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.DashboardMember;
import com.project.backend.Repository.DashboardMemberRepository;
import com.project.backend.Repository.DashboardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardAuthorizationService {

    private final DashboardMemberRepository dashboardMemberRepository;
    private final DashboardRepository dashboardRepository;

    /**
     * Check if a user is a member of a dashboard
     */
    @Transactional(readOnly = true)
    public boolean isDashboardMember(Long userId, Long dashboardId) {
        return dashboardMemberRepository.existsByDashboardIdAndUserId(dashboardId, userId);
    }

    /**
     * Check if a user is the owner of a dashboard
     */
    @Transactional(readOnly = true)
    public boolean isDashboardOwner(Long userId, Long dashboardId) {
        return dashboardMemberRepository
                .findByDashboard_IdAndUser_Id(dashboardId, userId)
                .map(member -> member.getRole() == DashboardRole.OWNER)
                .orElse(false);
    }

    /**
     * Validate that user has access to dashboard (is member)
     * Throws 403 Forbidden if user is not a member
     */
    @Transactional(readOnly = true)
    public void validateDashboardAccess(Long userId, Long dashboardId) {
        if (!isDashboardMember(userId, dashboardId)) {
            throw ApiError.forbidden("Access denied: User is not a member of this dashboard");
        }
    }

    /**
     * Validate that user is the owner of dashboard
     * Throws 403 Forbidden if user is not the owner
     */
    @Transactional(readOnly = true)
    public void validateDashboardOwnerAccess(Long userId, Long dashboardId) {
        if (!isDashboardOwner(userId, dashboardId)) {
            throw ApiError.forbidden("Access denied: User is not the owner of this dashboard");
        }
    }

    /**
     * Get the owner member record of a dashboard
     */
    @Transactional(readOnly = true)
    public DashboardMember getOwnerMember(Long dashboardId) {
        return dashboardMemberRepository
                .findByDashboardIdAndRole(dashboardId, DashboardRole.OWNER)
                .stream()
                .findFirst()
                .orElseThrow(() -> ApiError.badRequest("Dashboard has no owner"));
    }

    /**
     * Get a dashboard by ID and validate it exists
     */
    @Transactional(readOnly = true)
    public Dashboard getDashboardOrThrow(Long dashboardId) {
        return dashboardRepository
                .findById(dashboardId)
                .orElseThrow(() -> ApiError.notFound("Dashboard not found"));
    }

    /**
     * Get a dashboard member record or throw
     */
    @Transactional(readOnly = true)
    public DashboardMember getMemberOrThrow(Long dashboardId, Long userId) {
        return dashboardMemberRepository
                .findByDashboard_IdAndUser_Id(dashboardId, userId)
                .orElseThrow(() -> ApiError.notFound("User is not a member of this dashboard"));
    }
}
