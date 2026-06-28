package com.project.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskStatusResponseDto {
    private Long id;
    private String name;
    private Integer position;
    private Long categoryId;
    private String systemKey;
}