package com.project.backend.Dto;

import java.time.LocalDate;

import com.project.backend.Enum.Priority;
import com.project.backend.Enum.Status;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskPatchDto {

    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    private Status status;

    private Priority priority;

    private LocalDate deadline;

    private Long categoryId;

    private Long customStatusId;
}
