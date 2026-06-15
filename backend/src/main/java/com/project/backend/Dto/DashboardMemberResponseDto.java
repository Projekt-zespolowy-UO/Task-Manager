package com.project.backend.Dto;

import com.project.backend.Enum.DashboardRole;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardMemberResponseDto {
    private Long userId;
    private String username;
    private String email;
    private DashboardRole role;
}
