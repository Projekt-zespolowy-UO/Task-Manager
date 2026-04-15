package com.project.backend.Dto;

import java.time.LocalDate;

import com.project.backend.Enum.Priority;
import com.project.backend.Enum.Status;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCreateDto {

    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDate deadline;

    private Long categoryId;
}