package com.project.backend.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferOwnershipDto {

    @NotNull
    private Long userId;
}
