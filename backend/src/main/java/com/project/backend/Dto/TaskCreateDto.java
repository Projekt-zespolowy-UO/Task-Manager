package com.project.backend.Dto;

import java.time.LocalDate;

import com.project.backend.Enum.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCreateDto {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    private Long statusId;

    private Priority priority;
    private LocalDate startDate;
    private LocalDate deadline;

    @NotNull
    private Long categoryId;

    private Long dashboardId;
}
