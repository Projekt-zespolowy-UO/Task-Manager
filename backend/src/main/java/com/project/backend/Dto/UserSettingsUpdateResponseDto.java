package com.project.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSettingsUpdateResponseDto {

    private String userName;
    private String email;
    private String token;
    private String refreshToken;
}
