package com.project.backend.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.Dto.DashboardInvitationResponseDto;
import com.project.backend.Dto.DashboardMemberResponseDto;
import com.project.backend.Enum.DashboardRole;
import com.project.backend.Exception.ApiError;
import com.project.backend.Model.Dashboard;
import com.project.backend.Model.DashboardInvitation;
import com.project.backend.Model.DashboardMember;
import com.project.backend.Model.UserModel;
import com.project.backend.Repository.DashboardInvitationRepository;
import com.project.backend.Repository.DashboardMemberRepository;
import com.project.backend.Repository.DashboardRepository;
import com.project.backend.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardMembershipService {

    private final DashboardMemberRepository dashboardMemberRepository;
    private final DashboardInvitationRepository dashboardInvitationRepository;
    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final DashboardAuthorizationService authorizationService;

    /**
     * Get all members of a dashboard
     */
    @Transactional(readOnly = true)
    public List<DashboardMemberResponseDto> getDashboardMembers(Long dashboardId, Long requesterId) {
        // Validate requester is a member
        authorizationService.validateDashboardAccess(requesterId, dashboardId);

        return dashboardMemberRepository
                .findByDashboard_IdOrderByCreatedAtAsc(dashboardId)
                .stream()
                .map(this::toMemberResponseDto)
                .toList();
    }

    /**
     * Invite a user to a dashboard (owner only)
     * Creates an invitation record for async acceptance
     */
    @Transactional
    public DashboardInvitation inviteUser(Long dashboardId, Long userIdToInvite, Long requesterId) {
        // Validate requester is the owner
        authorizationService.validateDashboardOwnerAccess(requesterId, dashboardId);

        // Get dashboard and user to invite
        Dashboard dashboard = authorizationService.getDashboardOrThrow(dashboardId);
        UserModel userToInvite = userRepository
                .findById(userIdToInvite)
                .orElseThrow(() -> ApiError.notFound("User not found"));

        // Check user is not already a member
        if (dashboardMemberRepository.existsByDashboardIdAndUserId(dashboardId, userIdToInvite)) {
            throw ApiError.badRequest("User is already a member of this dashboard");
        }

        // Check no pending invitation exists
        if (dashboardInvitationRepository.existsPendingInvitation(dashboardId, userIdToInvite)) {
            throw ApiError.badRequest("A pending invitation already exists for this user");
        }

        // Get requester user object
        UserModel requester = userRepository
                .findById(requesterId)
                .orElseThrow(() -> ApiError.unauthorized("Requester user not found"));

        // Create invitation
        DashboardInvitation invitation = new DashboardInvitation(dashboard, userToInvite, requester);
        return dashboardInvitationRepository.save(invitation);
    }

    /**
     * Accept an invitation and add user as member
     */
    @Transactional
    public void acceptInvitation(Long invitationId, Long userId) {
        DashboardInvitation invitation = dashboardInvitationRepository
                .findByIdAndInvitedUser_Id(invitationId, userId)
                .orElseThrow(() -> ApiError.notFound("Invitation not found"));

        if (!invitation.getStatus().name().equals("PENDING")) {
            throw ApiError.badRequest("Invitation has already been processed");
        }

        // Add user as member
        DashboardMember member = new DashboardMember(
                invitation.getDashboard(),
                invitation.getInvitedUser(),
                DashboardRole.MEMBER
        );
        dashboardMemberRepository.save(member);

        // Update invitation status
        invitation.setStatus(com.project.backend.Enum.InvitationStatus.ACCEPTED);
        dashboardInvitationRepository.save(invitation);
    }

    /**
     * Remove a member from a dashboard (owner only)
     */
    @Transactional
    public void removeMember(Long dashboardId, Long userIdToRemove, Long requesterId) {
        // Validate requester is the owner
        authorizationService.validateDashboardOwnerAccess(requesterId, dashboardId);

        // Cannot remove self
        if (requesterId.equals(userIdToRemove)) {
            throw ApiError.badRequest("Owner cannot remove themselves from dashboard");
        }

        // Check member exists
        authorizationService.getMemberOrThrow(dashboardId, userIdToRemove);

        // Remove member
        int deleted = dashboardMemberRepository.deleteByDashboard_IdAndUser_Id(dashboardId, userIdToRemove);
        if (deleted == 0) {
            throw ApiError.notFound("Member not found");
        }
    }

    /**
     * Get all pending invitations for a user
     */
    @Transactional(readOnly = true)
    public List<DashboardInvitationResponseDto> getPendingInvitations(Long userId) {
        return dashboardInvitationRepository
                .findByInvitedUser_IdAndStatusOrderByCreatedAtDesc(userId, com.project.backend.Enum.InvitationStatus.PENDING)
                .stream()
                .map(this::toInvitationResponseDto)
                .toList();
    }

    /**
     * Reject an invitation
     */
    @Transactional
    public void rejectInvitation(Long invitationId, Long userId) {
        DashboardInvitation invitation = dashboardInvitationRepository
                .findByIdAndInvitedUser_Id(invitationId, userId)
                .orElseThrow(() -> ApiError.notFound("Invitation not found"));

        if (!invitation.getStatus().name().equals("PENDING")) {
            throw ApiError.badRequest("Invitation has already been processed");
        }

        invitation.setStatus(com.project.backend.Enum.InvitationStatus.REJECTED);
        dashboardInvitationRepository.save(invitation);
    }

    // Helper methods

    private DashboardMemberResponseDto toMemberResponseDto(DashboardMember member) {
        UserModel user = member.getUser();
        return new DashboardMemberResponseDto(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                member.getRole()
        );
    }

    private DashboardInvitationResponseDto toInvitationResponseDto(DashboardInvitation invitation) {
        Dashboard dashboard = invitation.getDashboard();
        UserModel invitedBy = invitation.getInvitedBy();
        return new DashboardInvitationResponseDto(
                invitation.getId(),
                dashboard.getId(),
                dashboard.getName(),
                invitedBy.getId(),
                invitedBy.getUserName(),
                invitation.getStatus(),
                invitation.getCreatedAt().toString()
        );
    }
}
