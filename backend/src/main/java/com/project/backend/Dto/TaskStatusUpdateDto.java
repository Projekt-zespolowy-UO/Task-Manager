package com.project.backend.Dto;

import com.project.backend.Enum.Status;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateDto {

    @NotNull
    private Status status;
}
