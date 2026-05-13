package com.project.backend.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateDto {

    @NotBlank
    @Size(max = 255)
    private String name;
}
