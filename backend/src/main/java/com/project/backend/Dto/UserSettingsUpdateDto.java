package com.project.backend.Dto;

import lombok.Data;

@Data
public class UserSettingsUpdateDto {

    private String userName;
    private String email;
    private String currentPassword;
    private String newPassword;
}
