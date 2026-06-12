package com.project.backend.Dto;

import java.time.LocalDate;

import com.project.backend.Enum.Priority;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private Long statusId;
    private String statusName;
    private Priority priority;
    private LocalDate deadline;
    private Long categoryId;
    private Long dashboardId;
}