package com.project.backend.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomStatusCreateDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    private Integer position;
}
