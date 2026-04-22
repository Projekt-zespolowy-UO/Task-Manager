package com.project.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSettingsResponseDto {

    private String userName;
    private String email;
}
