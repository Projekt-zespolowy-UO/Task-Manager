package com.project.backend.Dto;

import com.project.backend.Enum.InvitationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardInvitationResponseDto {
    private Long id;
    private Long dashboardId;
    private String dashboardName;
    private Long invitedByUserId;
    private String invitedByUsername;
    private InvitationStatus status;
    private String createdAt;
}
