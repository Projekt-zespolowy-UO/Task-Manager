package com.project.backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.DashboardResponseDto;
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
    public List<DashboardResponseDto> getDashboards(CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        return dashboardMemberRepository.findByUser_IdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(DashboardMember::getDashboard)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
public DashboardResponseDto createDashboard(CustomUserDetails userDetails, String name) {
    UserModel user = requireAuthenticatedUser(userDetails);
    String dashboardName = normalizeName(name);

    if (dashboardRepository.existsByUser_IdAndNameIgnoreCase(
            user.getId(),
            dashboardName
    )) {
        throw ApiError.badRequest(
                "Dashboard with this name already exists"
        );
    }

    Dashboard dashboard = new Dashboard();
    dashboard.setUser(user);
    dashboard.setName(dashboardName);

    Dashboard savedDashboard = dashboardRepository.save(dashboard);

    DashboardMember ownerMember = new DashboardMember(
            savedDashboard,
            user,
            DashboardRole.OWNER
    );

    dashboardMemberRepository.save(ownerMember);

    return toResponse(savedDashboard);
}

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboardById(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        authorizationService.validateDashboardAccess(user.getId(), id);
        return toResponse(authorizationService.getDashboardOrThrow(id));
    }

    @Transactional
    public void deleteDashboard(Long id, CustomUserDetails userDetails) {
        UserModel user = requireAuthenticatedUser(userDetails);
        // Verify user is the owner
        authorizationService.validateDashboardOwnerAccess(user.getId(), id);
        Dashboard dashboard = authorizationService.getDashboardOrThrow(id);
        dashboardRepository.delete(dashboard);
    }
@Transactional
public DashboardResponseDto renameDashboard(
        Long dashboardId,
        String newName,
        CustomUserDetails userDetails) {

    UserModel user = requireAuthenticatedUser(userDetails);

    authorizationService.validateDashboardOwnerAccess(
            user.getId(),
            dashboardId
    );

    Dashboard dashboard =
            authorizationService.getDashboardOrThrow(dashboardId);

    String normalizedName = normalizeName(newName);

    if (dashboardRepository.existsByUser_IdAndNameIgnoreCase(
            user.getId(),
            normalizedName
    ) && !dashboard.getName().equalsIgnoreCase(normalizedName)) {

        throw ApiError.badRequest(
                "Dashboard with this name already exists"
        );
    }

    dashboard.setName(normalizedName);

    Dashboard savedDashboard = dashboardRepository.save(dashboard);

    return toResponse(savedDashboard);
}
    @Transactional
    public void transferOwnership(Long dashboardId, Long targetUserId, CustomUserDetails userDetails) {
        UserModel currentUser = requireAuthenticatedUser(userDetails);
        Dashboard dashboard = authorizationService.getDashboardOrThrow(dashboardId);
        authorizationService.validateDashboardOwnerAccess(currentUser.getId(), dashboardId);
    if (currentUser.getId().equals(targetUserId)) {
    throw ApiError.badRequest("Cannot transfer ownership to yourself");
    }
        DashboardMember targetMember = authorizationService.getMemberOrThrow(dashboardId, targetUserId);
        if (targetMember.getRole() == DashboardRole.OWNER) {
            throw ApiError.badRequest("Target user is already the dashboard owner");
        }

        DashboardMember currentOwnerMember = authorizationService.getOwnerMember(dashboardId);
        currentOwnerMember.setRole(DashboardRole.MEMBER);
        targetMember.setRole(DashboardRole.OWNER);
        dashboard.setUser(targetMember.getUser());

        dashboardMemberRepository.save(currentOwnerMember);
        dashboardMemberRepository.save(targetMember);
        dashboardRepository.save(dashboard);
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

        String normalizedName = name.trim().replaceAll("\\s+", " ");

    if (normalizedName.length() < 3) {
        throw ApiError.badRequest(
                "Dashboard name must contain at least 3 characters"
        );
    }
    if (normalizedName.length() > 255) {
    throw ApiError.badRequest(
            "Dashboard name cannot exceed 255 characters"
    );
    }
    if (normalizedName.matches("\\d+")) {
    throw ApiError.badRequest(
            "Dashboard name cannot contain only numbers"
    );
}

    return normalizedName;
    }

    private DashboardResponseDto toResponse(Dashboard dashboard) {
        return new DashboardResponseDto(dashboard.getId(), dashboard.getName());
    }
}
