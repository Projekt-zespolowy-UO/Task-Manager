package com.project.backend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteUserDto {
    @NotNull
    private Long userId;
}
