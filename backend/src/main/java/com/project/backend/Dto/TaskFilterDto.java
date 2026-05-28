package com.project.backend.Dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.project.backend.Enum.Priority;
import com.project.backend.Enum.Status;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskFilterDto {
    private Status status;
    private Priority priority;
    private Long categoryId;
    private Long dashboardId;
    private Long customStatusId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadlineFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadlineTo;

    private String search;
}
