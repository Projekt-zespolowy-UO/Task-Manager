package com.project.backend.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.Dto.DashboardCreateDto;
import com.project.backend.Dto.DashboardInvitationResponseDto;
import com.project.backend.Dto.DashboardMemberResponseDto;
import com.project.backend.Dto.DashboardResponseDto;
import com.project.backend.Dto.InviteUserDto;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.DashboardInvitation;
import com.project.backend.Security.CustomUserDetails;
import com.project.backend.Service.DashboardMembershipService;
import com.project.backend.Service.DashboardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardMembershipService dashboardMembershipService;

    @GetMapping
    public ResponseEntity<List<DashboardResponseDto>> getDashboards(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DashboardResponseDto> dashboards = dashboardService.getDashboards(userDetails);
        return ResponseEntity.ok(dashboards);
    }

    @PostMapping
    public ResponseEntity<DashboardResponseDto> createDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DashboardCreateDto dto) {
        DashboardResponseDto dashboard = dashboardService.createDashboard(userDetails, dto.getName());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/{dashboardId}")
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @PathVariable Long dashboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        DashboardResponseDto dashboard = dashboardService.getDashboardById(dashboardId, userDetails);
        return ResponseEntity.ok(dashboard);
    }

    @DeleteMapping("/{dashboardId}")
    public ResponseEntity<Void> deleteDashboard(
            @PathVariable Long dashboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        dashboardService.deleteDashboard(dashboardId, userDetails);
        return ResponseEntity.noContent().build();
    }

    // Dashboard Members Endpoints

    @GetMapping("/{dashboardId}/members")
    public ResponseEntity<List<DashboardMemberResponseDto>> getDashboardMembers(
            @PathVariable Long dashboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DashboardMemberResponseDto> members = dashboardMembershipService.getDashboardMembers(
                dashboardId,
                userDetails.user().getId()
        );
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{dashboardId}/members/invite")
    public ResponseEntity<Void> inviteUser(
            @PathVariable Long dashboardId,
            @Valid @RequestBody InviteUserDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        dashboardMembershipService.inviteUser(
                dashboardId,
                dto.getUserId(),
                userDetails.user().getId()
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{dashboardId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long dashboardId,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        dashboardMembershipService.removeMember(
                dashboardId,
                userId,
                userDetails.user().getId()
        );
        return ResponseEntity.noContent().build();
    }

    // Dashboard Invitations Endpoints

    @GetMapping("/my-invitations")
    public ResponseEntity<List<DashboardInvitationResponseDto>> getMyPendingInvitations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DashboardInvitationResponseDto> invitations = dashboardMembershipService.getPendingInvitations(
                userDetails.user().getId()
        );
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        dashboardMembershipService.acceptInvitation(
                invitationId,
                userDetails.user().getId()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<Void> rejectInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        dashboardMembershipService.rejectInvitation(
                invitationId,
                userDetails.user().getId()
        );
        return ResponseEntity.ok().build();
    }
}
