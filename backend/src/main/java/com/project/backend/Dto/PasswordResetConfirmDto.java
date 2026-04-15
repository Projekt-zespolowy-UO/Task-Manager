package com.project.backend.Dto;

import lombok.Data;

@Data
public class PasswordResetConfirmDto {
    private String email;
    private String code;
    private String newPassword;
}
