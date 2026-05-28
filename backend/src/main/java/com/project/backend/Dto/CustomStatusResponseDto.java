package com.project.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomStatusResponseDto {
    private Long id;
    private String name;
    private Integer position;
}
