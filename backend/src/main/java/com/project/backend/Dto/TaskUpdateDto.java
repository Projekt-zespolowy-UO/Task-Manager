package com.project.backend.Dto;

import java.time.LocalDate;

import com.project.backend.Enum.Priority;
import com.project.backend.Enum.Status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskUpdateDto {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    private Status status;

    @NotNull
    private Priority priority;

    private LocalDate deadline;

    @NotNull
    private Long categoryId;
}
